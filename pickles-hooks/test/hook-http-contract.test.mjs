import { spawn, spawnSync } from "node:child_process";
import { existsSync, mkdtempSync, mkdirSync, realpathSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import http from "node:http";
import test from "node:test";
import assert from "node:assert/strict";

const hookScript = fileURLToPath(new URL("../pickles-hook.mjs", import.meta.url));

test("hook calls health, notify, and feedback contracts", async () => {
    const requests = [];
    const server = http.createServer(async (request, response) => {
        const body = await readRequestBody(request);
        requests.push({
            method: request.method,
            url: request.url,
            body: body.length > 0 ? JSON.parse(body) : null,
        });

        if (request.method === "GET" && request.url === "/health") {
            writeJson(response, 200, { schemaVersion: 1, requestId: null, status: "ok" });
            return;
        }

        if (request.method === "POST" && request.url === "/notify") {
            writeJson(response, 202, {
                schemaVersion: 1,
                requestId: requests.at(-1).body.requestId,
                accepted: true,
                processed: false,
            });
            return;
        }

        if (request.method === "POST" && request.url === "/feedback") {
            writeJson(response, 200, {
                schemaVersion: 1,
                requestId: requests.at(-1).body.requestId,
                status: "unimplemented",
                hasBlockingProblems: false,
                summary: {
                    errorCount: 0,
                    warnCount: 0,
                    text: "Governance feedback is not implemented yet.",
                },
                problems: [],
            });
            return;
        }

        writeJson(response, 404, {
            schemaVersion: 1,
            requestId: null,
            error: { code: "INVALID_REQUEST", message: "Not found.", details: {} },
        });
    });

    await listen(server);
    try {
        const repo = createGitWorkspace(server.address().port);

        const sessionStart = await runHook(repo, {
            session_id: "session-1",
            hook_event_name: "SessionStart",
            cwd: repo,
        });
        assert.equal(sessionStart.status, 0, sessionStart.stderr);

        const postToolUseEmpty = await runHook(repo, {
            session_id: "session-1",
            turn_id: "turn-1",
            hook_event_name: "PostToolUse",
            tool_use_id: "tool-1",
            tool_name: "Bash",
            cwd: repo,
        });
        assert.equal(postToolUseEmpty.status, 0, postToolUseEmpty.stderr);

        const postToolUse = await runHook(
            repo,
            {
                session_id: "session-1",
                turn_id: "turn-2",
                hook_event_name: "PostToolUse",
                tool_use_id: "tool-2",
                tool_name: "Bash",
                cwd: repo,
            },
            {
                PICKLES_TEST_CHANGED_FILE: JSON.stringify({
                    fileName: "src/pricing.ts",
                    before: null,
                    after: "new",
                }),
            },
        );
        assert.equal(postToolUse.status, 0, postToolUse.stderr);

        const stop = await runHook(repo, {
            session_id: "session-1",
            turn_id: "turn-1",
            hook_event_name: "Stop",
            cwd: repo,
        });
        assert.equal(stop.status, 0, stop.stderr);
        assert.match(stop.stdout, /unimplemented/);

        assert.equal(
            requests.some((request) => request.method === "GET" && request.url === "/health"),
            true,
        );

        const notifyRequests = requests.filter(
            (request) => request.method === "POST" && request.url === "/notify",
        );
        assert.equal(notifyRequests[0].body.event.idempotencyKey, "session-1:turn-1:PostToolUse");
        assert.deepEqual(notifyRequests[0].body.files, []);

        const notify = notifyRequests[1];
        assert.equal(notify.body.schemaVersion, 1);
        assert.equal(typeof notify.body.requestId, "string");
        assert.equal(notify.body.event.sessionId, "session-1");
        assert.equal(notify.body.event.hookEventName, "PostToolUse");
        assert.equal(notify.body.event.workspace, repo);
        assert.equal(
            notify.body.event.idempotencyKey,
            "session-1:turn-2:PostToolUse:src/pricing.ts",
        );
        assert.equal(Array.isArray(notify.body.files), true);
        assert.deepEqual(notify.body.files, [
            {
                fileName: "src/pricing.ts",
                before: null,
                after: "new",
            },
        ]);

        const feedback = requests.find(
            (request) => request.method === "POST" && request.url === "/feedback",
        );
        assert.equal(feedback.body.schemaVersion, 1);
        assert.equal(typeof feedback.body.requestId, "string");
        assert.equal(feedback.body.sessionId, "session-1");
        assert.equal(feedback.body.turnId, "turn-1");
        assert.equal(feedback.body.workspace, repo);
        assert.equal(Object.hasOwn(feedback.body, "files"), false);
    } finally {
        await close(server);
    }
});

test("hook fails when server.json is missing", async () => {
    const repo = createGitWorkspace(null);
    const result = await runHook(repo, {
        session_id: "session-1",
        hook_event_name: "SessionStart",
        cwd: repo,
    });

    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /server\.json/);
});

test("hook fails when server.json port is not a number", async () => {
    const repo = createGitWorkspace("not-a-number");
    const result = await runHook(repo, {
        session_id: "session-1",
        hook_event_name: "SessionStart",
        cwd: repo,
    });

    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /positive numeric port/);
});

test("hook captures modified added and deleted files", async () => {
    const requests = [];
    const server = createFakeServer(requests);
    await listen(server);
    try {
        const repo = createGitWorkspace(server.address().port);
        writeFileSync(path.join(repo, "existing.txt"), "old\n", "utf8");
        writeFileSync(path.join(repo, "deleted.txt"), "delete me\n", "utf8");
        git(repo, ["add", "existing.txt", "deleted.txt"]);
        git(repo, ["commit", "-m", "initial"]);

        const pre = await runHook(repo, {
            session_id: "session-capture",
            turn_id: "turn-capture",
            hook_event_name: "PreToolUse",
            tool_use_id: "tool-capture",
            tool_name: "apply_patch",
            tool_input: {
                command: [
                    "*** Begin Patch",
                    "*** Update File: existing.txt",
                    "*** Add File: added.txt",
                    "*** Delete File: deleted.txt",
                    "*** End Patch",
                ].join("\n"),
            },
            cwd: repo,
        });
        assert.equal(pre.status, 0, pre.stderr);
        const statePath = path.join(
            repo,
            ".pickles",
            "hooks-state",
            "session-capture",
            "turn-capture",
            "tool-capture.json",
        );
        assert.equal(existsSync(statePath), true);

        writeFileSync(path.join(repo, "existing.txt"), "new\n", "utf8");
        writeFileSync(path.join(repo, "added.txt"), "added\n", "utf8");
        rmSync(path.join(repo, "deleted.txt"));

        const post = await runHook(repo, {
            session_id: "session-capture",
            turn_id: "turn-capture",
            hook_event_name: "PostToolUse",
            tool_use_id: "tool-capture",
            tool_name: "apply_patch",
            tool_input: {},
            tool_response: {},
            cwd: repo,
        });
        assert.equal(post.status, 0, post.stderr);
        assert.equal(existsSync(statePath), false);

        const notify = requests.find(
            (request) => request.method === "POST" && request.url === "/notify",
        );
        assert.deepEqual(sortFiles(notify.body.files), [
            { fileName: "added.txt", before: null, after: "added\n" },
            { fileName: "deleted.txt", before: "delete me\n", after: null },
            { fileName: "existing.txt", before: "old\n", after: "new\n" },
        ]);
        assert.equal(
            notify.body.files.every((file) => path.isAbsolute(file.fileName) === false),
            true,
        );
    } finally {
        await close(server);
    }
});

test("hook falls back to git HEAD when PostToolUse state is missing", async () => {
    const requests = [];
    const server = createFakeServer(requests);
    await listen(server);
    try {
        const repo = createGitWorkspace(server.address().port);
        writeFileSync(path.join(repo, "fallback.txt"), "old\n", "utf8");
        git(repo, ["add", "fallback.txt"]);
        git(repo, ["commit", "-m", "initial"]);
        writeFileSync(path.join(repo, "fallback.txt"), "new\n", "utf8");

        const post = await runHook(repo, {
            session_id: "session-fallback",
            turn_id: "turn-fallback",
            hook_event_name: "PostToolUse",
            tool_use_id: "tool-fallback",
            tool_name: "Bash",
            tool_input: {},
            tool_response: {},
            cwd: repo,
        });
        assert.equal(post.status, 0, post.stderr);

        const notify = requests.find(
            (request) => request.method === "POST" && request.url === "/notify",
        );
        assert.deepEqual(notify.body.files, [
            { fileName: "fallback.txt", before: "old\n", after: "new\n" },
        ]);
    } finally {
        await close(server);
    }
});

test("hook flushes pending state before feedback on Stop", async () => {
    const requests = [];
    const server = createFakeServer(requests);
    await listen(server);
    try {
        const repo = createGitWorkspace(server.address().port);
        writeFileSync(path.join(repo, "pending.txt"), "old\n", "utf8");
        git(repo, ["add", "pending.txt"]);
        git(repo, ["commit", "-m", "initial"]);

        const pre = await runHook(repo, {
            session_id: "session-stop",
            turn_id: "turn-stop",
            hook_event_name: "PreToolUse",
            tool_use_id: "tool-stop",
            tool_name: "Edit",
            tool_input: {
                file_path: "pending.txt",
            },
            cwd: repo,
        });
        assert.equal(pre.status, 0, pre.stderr);
        const statePath = path.join(
            repo,
            ".pickles",
            "hooks-state",
            "session-stop",
            "turn-stop",
            "tool-stop.json",
        );
        assert.equal(existsSync(statePath), true);

        writeFileSync(path.join(repo, "pending.txt"), "new\n", "utf8");

        const stop = await runHook(repo, {
            session_id: "session-stop",
            turn_id: "turn-stop",
            hook_event_name: "Stop",
            cwd: repo,
        });
        assert.equal(stop.status, 0, stop.stderr);
        assert.equal(existsSync(statePath), false);

        const notifyIndex = requests.findIndex(
            (request) => request.method === "POST" && request.url === "/notify",
        );
        const feedbackIndex = requests.findIndex(
            (request) => request.method === "POST" && request.url === "/feedback",
        );
        assert.equal(notifyIndex >= 0, true);
        assert.equal(feedbackIndex > notifyIndex, true);
        assert.deepEqual(requests[notifyIndex].body.files, [
            { fileName: "pending.txt", before: "old\n", after: "new\n" },
        ]);
        assert.equal(Object.hasOwn(requests[feedbackIndex].body, "files"), false);
    } finally {
        await close(server);
    }
});

function runHook(cwd, input, env = {}) {
    return new Promise((resolve) => {
        const child = spawn(process.execPath, [hookScript], {
            cwd,
            stdio: ["pipe", "pipe", "pipe"],
            env: {
                ...process.env,
                ...env,
            },
        });

        let stdout = "";
        let stderr = "";
        child.stdout.setEncoding("utf8");
        child.stderr.setEncoding("utf8");
        child.stdout.on("data", (chunk) => {
            stdout += chunk;
        });
        child.stderr.on("data", (chunk) => {
            stderr += chunk;
        });
        child.on("close", (status) => {
            resolve({ status, stdout, stderr });
        });
        child.stdin.end(JSON.stringify(input));
    });
}

function createGitWorkspace(port) {
    const repo = realpathSync(mkdtempSync(path.join(tmpdir(), "pickles-hook-test-")));
    const init = spawnSync("git", ["init"], { cwd: repo, encoding: "utf8" });
    assert.equal(init.status, 0, init.stderr);
    git(repo, ["config", "user.email", "pickles@example.test"]);
    git(repo, ["config", "user.name", "Pickles Test"]);

    if (port !== null) {
        mkdirSync(path.join(repo, ".pickles"), { recursive: true });
        writeFileSync(path.join(repo, ".pickles", "server.json"), JSON.stringify({ port }), "utf8");
    }

    return repo;
}

function git(cwd, args) {
    const result = spawnSync("git", args, { cwd, encoding: "utf8" });
    assert.equal(result.status, 0, result.stderr);
    return result.stdout;
}

function createFakeServer(requests) {
    return http.createServer(async (request, response) => {
        const body = await readRequestBody(request);
        requests.push({
            method: request.method,
            url: request.url,
            body: body.length > 0 ? JSON.parse(body) : null,
        });

        if (request.method === "GET" && request.url === "/health") {
            writeJson(response, 200, { schemaVersion: 1, requestId: null, status: "ok" });
            return;
        }

        if (request.method === "POST" && request.url === "/notify") {
            writeJson(response, 202, {
                schemaVersion: 1,
                requestId: requests.at(-1).body.requestId,
                accepted: true,
                processed: false,
            });
            return;
        }

        if (request.method === "POST" && request.url === "/feedback") {
            writeJson(response, 200, {
                schemaVersion: 1,
                requestId: requests.at(-1).body.requestId,
                status: "unimplemented",
                hasBlockingProblems: false,
                summary: {
                    errorCount: 0,
                    warnCount: 0,
                    text: "Governance feedback is not implemented yet.",
                },
                problems: [],
            });
            return;
        }

        writeJson(response, 404, {
            schemaVersion: 1,
            requestId: null,
            error: { code: "INVALID_REQUEST", message: "Not found.", details: {} },
        });
    });
}

function sortFiles(files) {
    return [...files].sort((left, right) => left.fileName.localeCompare(right.fileName));
}

function readRequestBody(request) {
    return new Promise((resolve, reject) => {
        let data = "";
        request.setEncoding("utf8");
        request.on("data", (chunk) => {
            data += chunk;
        });
        request.on("end", () => resolve(data));
        request.on("error", reject);
    });
}

function writeJson(response, statusCode, body) {
    response.writeHead(statusCode, { "Content-Type": "application/json" });
    response.end(JSON.stringify(body));
}

function listen(server) {
    return new Promise((resolve) => {
        server.listen(0, "127.0.0.1", resolve);
    });
}

function close(server) {
    return new Promise((resolve, reject) => {
        server.close((error) => {
            if (error) {
                reject(error);
                return;
            }
            resolve();
        });
    });
}
