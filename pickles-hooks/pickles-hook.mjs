#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { existsSync } from "node:fs";
import { mkdir, readFile, readdir, rm, writeFile } from "node:fs/promises";
import path from "node:path";

const SCHEMA_VERSION = 1;
const SUPPORTED_EVENTS = new Set(["SessionStart", "PreToolUse", "PostToolUse", "Stop"]);

async function main() {
    const input = await readHookInput();
    const event = mapHookEvent(input);
    const workspace = discoverWorkspace(event.cwd);

    if (event.hookEventName === "PreToolUse") {
        const candidateFiles = extractCandidateFiles(event);
        const beforeFiles = await readBeforeFiles(workspace, candidateFiles);
        await writeCaptureState(workspace, event, {
            schemaVersion: SCHEMA_VERSION,
            sessionId: event.sessionId,
            turnId: event.turnId,
            toolUseId: event.toolUseId,
            toolName: event.toolName,
            workspace,
            candidateFiles,
            beforeFiles,
        });
        return;
    }

    const server = await readServerFile(workspace);
    const client = createHttpClient(server.baseUrl);

    if (event.hookEventName === "SessionStart") {
        await client.getJson("/health");
        return;
    }

    if (event.hookEventName === "PostToolUse") {
        const files = await readNotifyFiles(workspace, event);
        const request = {
            schemaVersion: SCHEMA_VERSION,
            requestId: randomUUID(),
            event: buildNotifyEvent(event, workspace, files),
            files,
        };
        await client.postJson("/notify", request);
        if (!hasTestChangedFile()) {
            await deleteCaptureState(workspace, event);
        }
        return;
    }

    if (event.hookEventName === "Stop") {
        await flushPendingDiffs(workspace, event, client);
        const request = {
            schemaVersion: SCHEMA_VERSION,
            requestId: randomUUID(),
            sessionId: event.sessionId,
            turnId: event.turnId,
            workspace,
        };
        const feedback = await client.postJson("/feedback", request);
        writeStopFeedback(feedback);
        return;
    }

    throw new Error(`Unsupported hook event: ${event.hookEventName}`);
}

async function readHookInput() {
    const stdin = await readStdin();
    if (stdin.trim().length === 0) {
        throw new Error("Hook stdin JSON is required.");
    }

    try {
        return JSON.parse(stdin);
    } catch (error) {
        throw new Error(`Hook stdin JSON is invalid: ${error.message}`);
    }
}

function readStdin() {
    return new Promise((resolve, reject) => {
        let data = "";
        process.stdin.setEncoding("utf8");
        process.stdin.on("data", (chunk) => {
            data += chunk;
        });
        process.stdin.on("end", () => resolve(data));
        process.stdin.on("error", reject);
    });
}

function mapHookEvent(input) {
    const hookEventName = requireString(input.hook_event_name, "hook_event_name");
    if (!SUPPORTED_EVENTS.has(hookEventName)) {
        throw new Error(`Unsupported hook event: ${hookEventName}`);
    }

    const sessionId = requireString(input.session_id, "session_id");
    const turnId = input.turn_id ?? null;
    if (turnId !== null && typeof turnId !== "string") {
        throw new Error("turn_id must be a string when present.");
    }
    if (hookEventName !== "SessionStart" && turnId === null) {
        throw new Error(`${hookEventName} requires turn_id.`);
    }
    const toolUseId = input.tool_use_id ?? null;
    const toolName = input.tool_name ?? null;
    if (
        (hookEventName === "PreToolUse" || hookEventName === "PostToolUse") &&
        typeof toolUseId !== "string"
    ) {
        throw new Error(`${hookEventName} requires tool_use_id.`);
    }
    if (
        (hookEventName === "PreToolUse" || hookEventName === "PostToolUse") &&
        typeof toolName !== "string"
    ) {
        throw new Error(`${hookEventName} requires tool_name.`);
    }

    return {
        sessionId,
        turnId,
        hookEventName,
        toolUseId,
        toolName,
        toolInput: input.tool_input ?? {},
        cwd: typeof input.cwd === "string" && input.cwd.length > 0 ? input.cwd : process.cwd(),
    };
}

function requireString(value, fieldName) {
    if (typeof value !== "string" || value.length === 0) {
        throw new Error(`${fieldName} must be a non-empty string.`);
    }
    return value;
}

function discoverWorkspace(cwd) {
    const gitRoot = runGitRoot(cwd);
    if (hasServerFile(gitRoot)) {
        return gitRoot;
    }

    const nestedWorkspace = findNearestServerWorkspace(cwd, gitRoot);
    if (nestedWorkspace !== null) {
        return nestedWorkspace;
    }

    return gitRoot;
}

function runGitRoot(cwd) {
    const result = spawnSync("git", ["rev-parse", "--show-toplevel"], {
        cwd,
        encoding: "utf8",
    });

    if (result.status !== 0) {
        const reason = result.stderr.trim() || "git rev-parse --show-toplevel failed.";
        throw new Error(`Unable to locate git root: ${reason}`);
    }

    return path.resolve(result.stdout.trim());
}

function findNearestServerWorkspace(cwd, stopAt) {
    let current = path.resolve(cwd);
    const boundary = path.resolve(stopAt);

    while (current.startsWith(boundary)) {
        if (hasServerFile(current)) {
            return current;
        }
        if (current === boundary) {
            return null;
        }
        current = path.dirname(current);
    }

    return null;
}

function hasServerFile(workspace) {
    return existsSync(path.join(workspace, ".pickles", "server.json"));
}

async function readServerFile(workspace) {
    const serverPath = path.join(workspace, ".pickles", "server.json");
    let raw;
    try {
        raw = await readFile(serverPath, "utf8");
    } catch (error) {
        throw new Error(`Unable to read ${serverPath}: ${error.message}`);
    }

    let server;
    try {
        server = JSON.parse(raw);
    } catch (error) {
        throw new Error(`${serverPath} is not valid JSON: ${error.message}`);
    }

    if (typeof server.port !== "number" || !Number.isInteger(server.port) || server.port <= 0) {
        throw new Error(`${serverPath} must contain a positive numeric port.`);
    }

    return {
        port: server.port,
        baseUrl: `http://127.0.0.1:${server.port}`,
    };
}

function createHttpClient(baseUrl) {
    return {
        getJson: (route) => requestJson(baseUrl, route, "GET"),
        postJson: (route, body) => requestJson(baseUrl, route, "POST", body),
    };
}

async function requestJson(baseUrl, route, method, body = undefined) {
    if (typeof fetch !== "function") {
        throw new Error("Node.js built-in fetch is required.");
    }

    const response = await fetch(`${baseUrl}${route}`, {
        method,
        headers: body === undefined ? undefined : { "Content-Type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
    });

    const text = await response.text();
    const payload = parseJsonResponse(route, text);
    if (!response.ok) {
        throw new Error(`${method} ${route} failed with HTTP ${response.status}: ${text}`);
    }
    return payload;
}

function parseJsonResponse(route, text) {
    try {
        return text.length === 0 ? null : JSON.parse(text);
    } catch (error) {
        throw new Error(`${route} returned invalid JSON: ${error.message}`);
    }
}

function buildNotifyEvent(event, workspace, files) {
    return {
        sessionId: event.sessionId,
        turnId: event.turnId,
        hookEventName: event.hookEventName,
        workspace,
        idempotencyKey: buildIdempotencyKey(event, files),
    };
}

function buildIdempotencyKey(event, files) {
    const base = [event.sessionId, event.turnId, event.hookEventName].join(":");
    if (files.length === 0) {
        return base;
    }
    return `${base}:${files[0].fileName}`;
}

function readTestChangedFiles() {
    const raw = process.env.PICKLES_TEST_CHANGED_FILE;
    if (raw === undefined || raw.length === 0) {
        return [];
    }

    let file;
    try {
        file = JSON.parse(raw);
    } catch (error) {
        throw new Error(`PICKLES_TEST_CHANGED_FILE is invalid JSON: ${error.message}`);
    }

    validateChangedFile(file);
    return [file];
}

function hasTestChangedFile() {
    return (
        process.env.PICKLES_TEST_CHANGED_FILE !== undefined &&
        process.env.PICKLES_TEST_CHANGED_FILE.length > 0
    );
}

function validateChangedFile(file) {
    if (typeof file !== "object" || file === null || Array.isArray(file)) {
        throw new Error("PICKLES_TEST_CHANGED_FILE must be a JSON object.");
    }
    if (typeof file.fileName !== "string" || file.fileName.length === 0) {
        throw new Error("PICKLES_TEST_CHANGED_FILE.fileName must be a non-empty string.");
    }
    if (path.isAbsolute(file.fileName)) {
        throw new Error("PICKLES_TEST_CHANGED_FILE.fileName must be relative.");
    }
    if (!(typeof file.before === "string" || file.before === null)) {
        throw new Error("PICKLES_TEST_CHANGED_FILE.before must be a string or null.");
    }
    if (!(typeof file.after === "string" || file.after === null)) {
        throw new Error("PICKLES_TEST_CHANGED_FILE.after must be a string or null.");
    }
}

function extractCandidateFiles(event) {
    if (event.toolName === "apply_patch") {
        return uniqueRelativePaths(extractPatchFiles(event.toolInput?.command ?? ""));
    }
    if (event.toolName === "Edit" || event.toolName === "Write") {
        return uniqueRelativePaths(extractObjectPaths(event.toolInput));
    }
    return [];
}

function extractPatchFiles(command) {
    if (typeof command !== "string") {
        return [];
    }

    const files = [];
    for (const line of command.split(/\r?\n/)) {
        const match = line.match(/^\*\*\* (?:Add|Update|Delete) File: (.+)$/);
        if (match) {
            files.push(match[1].trim());
            continue;
        }
        const moveMatch = line.match(/^\*\*\* Move to: (.+)$/);
        if (moveMatch) {
            files.push(moveMatch[1].trim());
        }
    }
    return files;
}

function extractObjectPaths(value) {
    if (typeof value !== "object" || value === null) {
        return [];
    }
    const files = [];
    for (const key of ["file_path", "filePath", "path"]) {
        if (typeof value[key] === "string") {
            files.push(value[key]);
        }
    }
    return files;
}

function uniqueRelativePaths(files) {
    const seen = new Set();
    const result = [];
    for (const file of files) {
        if (typeof file !== "string" || file.length === 0 || path.isAbsolute(file)) {
            continue;
        }
        const normalized = path.normalize(file).replaceAll("\\", "/");
        if (normalized.startsWith("../") || normalized === ".." || seen.has(normalized)) {
            continue;
        }
        seen.add(normalized);
        result.push(normalized);
    }
    return result;
}

async function readBeforeFiles(workspace, files) {
    const beforeFiles = {};
    for (const fileName of files) {
        beforeFiles[fileName] = await readWorkspaceFile(workspace, fileName);
    }
    return beforeFiles;
}

async function readWorkspaceFile(workspace, fileName) {
    const filePath = path.join(workspace, fileName);
    if (!filePath.startsWith(`${workspace}${path.sep}`)) {
        throw new Error(`File is outside workspace: ${fileName}`);
    }
    try {
        return await readFile(filePath, "utf8");
    } catch (error) {
        if (error.code === "ENOENT") {
            return null;
        }
        throw new Error(`Unable to read ${fileName}: ${error.message}`);
    }
}

async function readNotifyFiles(workspace, event) {
    if (hasTestChangedFile()) {
        return readTestChangedFiles();
    }

    const state = await readCaptureState(workspace, event);
    const changedFiles = listChangedFiles(workspace);
    const files = [];
    for (const fileName of changedFiles) {
        const before =
            state?.beforeFiles && Object.hasOwn(state.beforeFiles, fileName)
                ? state.beforeFiles[fileName]
                : readGitHeadFile(workspace, fileName);
        const after = await readWorkspaceFile(workspace, fileName);
        files.push({ fileName, before, after });
    }
    return files;
}

async function flushPendingDiffs(workspace, event, client) {
    const pendingStates = await listPendingCaptureStates(workspace, event);
    if (pendingStates.length === 0) {
        return;
    }

    const beforeFiles = {};
    for (const pending of pendingStates) {
        Object.assign(beforeFiles, pending.state.beforeFiles ?? {});
    }
    const files = await buildChangedFiles(workspace, beforeFiles);
    const request = {
        schemaVersion: SCHEMA_VERSION,
        requestId: randomUUID(),
        event: buildNotifyEvent(event, workspace, files),
        files,
    };
    await client.postJson("/notify", request);

    for (const pending of pendingStates) {
        await rm(pending.filePath, { force: true });
    }
}

async function buildChangedFiles(workspace, beforeFiles) {
    const files = [];
    for (const fileName of listChangedFiles(workspace)) {
        const before = Object.hasOwn(beforeFiles, fileName)
            ? beforeFiles[fileName]
            : readGitHeadFile(workspace, fileName);
        const after = await readWorkspaceFile(workspace, fileName);
        files.push({ fileName, before, after });
    }
    return files;
}

function listChangedFiles(workspace) {
    const result = spawnSync("git", ["status", "--porcelain", "--untracked-files=all"], {
        cwd: workspace,
        encoding: "utf8",
    });
    if (result.status !== 0) {
        const reason = result.stderr.trim() || "git status failed.";
        throw new Error(`Unable to inspect workspace diff: ${reason}`);
    }

    const files = [];
    for (const line of result.stdout.split(/\r?\n/)) {
        if (line.length === 0) {
            continue;
        }
        for (const fileName of parseGitStatusLine(line)) {
            if (fileName.startsWith(".pickles/")) {
                continue;
            }
            files.push(fileName);
        }
    }
    return uniqueRelativePaths(files);
}

function parseGitStatusLine(line) {
    const rawPath = line.slice(3).trim();
    if (rawPath.includes(" -> ")) {
        return rawPath.split(" -> ").map((value) => unquoteGitPath(value.trim()));
    }
    return [unquoteGitPath(rawPath)];
}

function unquoteGitPath(value) {
    if (value.startsWith('"') && value.endsWith('"')) {
        try {
            return JSON.parse(value);
        } catch {
            return value.slice(1, -1);
        }
    }
    return value;
}

function readGitHeadFile(workspace, fileName) {
    const result = spawnSync("git", ["show", `HEAD:${fileName}`], {
        cwd: workspace,
        encoding: "utf8",
        maxBuffer: 20 * 1024 * 1024,
    });
    if (result.status !== 0) {
        return null;
    }
    return result.stdout;
}

function stateFilePath(workspace, event) {
    const turnId = event.turnId ?? "session";
    const toolUseId = requireString(event.toolUseId, "tool_use_id");
    return path.join(
        workspace,
        ".pickles",
        "hooks-state",
        encodeStatePathPart(event.sessionId),
        encodeStatePathPart(turnId),
        `${encodeStatePathPart(toolUseId)}.json`,
    );
}

async function writeCaptureState(workspace, event, state) {
    const filePath = stateFilePath(workspace, event);
    await mkdir(path.dirname(filePath), { recursive: true });
    await writeFile(filePath, `${JSON.stringify(state, null, 2)}\n`, "utf8");
}

async function readCaptureState(workspace, event) {
    const filePath = stateFilePath(workspace, event);
    try {
        return JSON.parse(await readFile(filePath, "utf8"));
    } catch (error) {
        if (error.code === "ENOENT") {
            return null;
        }
        throw new Error(`Unable to read hook state ${filePath}: ${error.message}`);
    }
}

async function deleteCaptureState(workspace, event) {
    await rm(stateFilePath(workspace, event), { force: true });
}

async function listPendingCaptureStates(workspace, event) {
    const turnId = event.turnId ?? "session";
    const dir = path.join(
        workspace,
        ".pickles",
        "hooks-state",
        encodeStatePathPart(event.sessionId),
        encodeStatePathPart(turnId),
    );
    let entries;
    try {
        entries = await readdir(dir, { withFileTypes: true });
    } catch (error) {
        if (error.code === "ENOENT") {
            return [];
        }
        throw new Error(`Unable to list hook state ${dir}: ${error.message}`);
    }

    const states = [];
    for (const entry of entries) {
        if (!entry.isFile() || !entry.name.endsWith(".json")) {
            continue;
        }
        const filePath = path.join(dir, entry.name);
        states.push({
            filePath,
            state: JSON.parse(await readFile(filePath, "utf8")),
        });
    }
    return states;
}

function encodeStatePathPart(value) {
    return encodeURIComponent(value).replaceAll("%", "_");
}

function writeStopFeedback(feedback) {
    const status = typeof feedback?.status === "string" ? feedback.status : "unknown";
    const text = typeof feedback?.summary?.text === "string" ? feedback.summary.text : "";
    const systemMessage =
        text.length > 0
            ? `Pickles feedback status: ${status}. ${text}`
            : `Pickles feedback status: ${status}.`;
    process.stdout.write(`${JSON.stringify({ continue: true, systemMessage })}\n`);
}

main()
    .then(() => {
        process.exit(0);
    })
    .catch((error) => {
        process.stderr.write(`Pickles hook failed: ${error.message}\n`);
        process.exit(1);
    });
