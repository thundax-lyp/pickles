import assert from "node:assert/strict";
import { spawn, spawnSync } from "node:child_process";
import {
    existsSync,
    mkdirSync,
    mkdtempSync,
    readFileSync,
    realpathSync,
    symlinkSync,
    writeFileSync,
} from "node:fs";
import http from "node:http";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import { runRuntimeCheck } from "../../pickles-runtime/src/index.ts";

const repoRoot = fileURLToPath(new URL("../..", import.meta.url));
const hookScript = path.join(repoRoot, "pickles-hooks/pickles-hook.mjs");
const runtimePackage = path.join(repoRoot, "pickles-runtime");
const sampleJavaPath = "src/main/java/com/example/web/OrderController.java";

test("hook plugin runtime and feedback full flow", async () => {
    const requests = [];
    const problemBoard = [];
    const server = createPluginHarness(requests, problemBoard);
    await listen(server);

    try {
        const workspace = createWorkspace(server.address().port);

        const sessionStart = await runHook(workspace, {
            session_id: "session-full",
            hook_event_name: "SessionStart",
            cwd: workspace,
        });
        assert.equal(sessionStart.status, 0, sessionStart.stderr);

        const preToolUse = await runHook(workspace, {
            session_id: "session-full",
            turn_id: "turn-full",
            hook_event_name: "PreToolUse",
            tool_use_id: "tool-full",
            tool_name: "Edit",
            tool_input: {
                file_path: sampleJavaPath,
            },
            cwd: workspace,
        });
        assert.equal(preToolUse.status, 0, preToolUse.stderr);

        writeFileSync(path.join(workspace, sampleJavaPath), violatingController(), "utf8");

        const postToolUse = await runHook(workspace, {
            session_id: "session-full",
            turn_id: "turn-full",
            hook_event_name: "PostToolUse",
            tool_use_id: "tool-full",
            tool_name: "Edit",
            tool_input: {
                file_path: sampleJavaPath,
            },
            cwd: workspace,
        });
        assert.equal(postToolUse.status, 0, postToolUse.stderr);

        const stop = await runHook(workspace, {
            session_id: "session-full",
            turn_id: "turn-full",
            hook_event_name: "Stop",
            cwd: workspace,
        });
        assert.equal(stop.status, 0, stop.stderr);
        assert.match(stop.stdout, /blocking problem/);

        assert.equal(existsSync(path.join(workspace, ".pickles/server.json")), true);
        assert.equal(
            requests.some((request) => request.method === "GET" && request.url === "/health"),
            true,
        );

        const notify = requests.find(
            (request) => request.method === "POST" && request.url === "/notify",
        );
        assert.equal(notify.body.event.workspace, workspace);
        assert.deepEqual(notify.body.files, [
            {
                fileName: sampleJavaPath,
                before: cleanController(),
                after: violatingController(),
            },
        ]);

        assert.equal(problemBoard.length, 1);
        assert.equal(problemBoard[0].source.tool, "pickles-native");
        assert.equal(problemBoard[0].source.rule, "sample-java-no-controller-repository-import");
        assert.equal(problemBoard[0].file, sampleJavaPath);

        const feedback = requests.find(
            (request) => request.method === "POST" && request.url === "/feedback",
        );
        assert.equal(feedback.body.workspace, workspace);
    } finally {
        await close(server);
    }
});

function createPluginHarness(requests, problemBoard) {
    return http.createServer(async (request, response) => {
        const bodyText = await readRequestBody(request);
        const body = bodyText.length > 0 ? JSON.parse(bodyText) : null;
        requests.push({ method: request.method, url: request.url, body });

        if (request.method === "GET" && request.url === "/health") {
            writeJson(response, 200, { schemaVersion: 1, requestId: null, status: "ok" });
            return;
        }

        if (request.method === "POST" && request.url === "/notify") {
            const result = await runRuntimeCheck({
                workspaceRoot: body.event.workspace,
                changedFiles: body.files.map((file) => ({
                    path: file.fileName,
                    changeType: changeType(file),
                    before: file.before,
                    after: file.after,
                })),
            });
            problemBoard.splice(0, problemBoard.length, ...result.problems);
            writeJson(response, 202, {
                schemaVersion: 1,
                requestId: body.requestId,
                accepted: true,
                processed: true,
            });
            return;
        }

        if (request.method === "POST" && request.url === "/feedback") {
            const errorCount = problemBoard.filter(
                (problem) => problem.severity === "ERROR",
            ).length;
            const warnCount = problemBoard.filter((problem) => problem.severity === "WARN").length;
            writeJson(response, 200, {
                schemaVersion: 1,
                requestId: body.requestId,
                status: "ok",
                hasBlockingProblems: errorCount > 0,
                summary: {
                    errorCount,
                    warnCount,
                    text: `Pickles found ${errorCount} blocking problem(s) and ${warnCount} warning(s).`,
                },
                problems: problemBoard,
            });
            return;
        }

        writeJson(response, 404, {
            schemaVersion: 1,
            requestId: body?.requestId ?? null,
            error: { code: "INVALID_REQUEST", message: "Not found.", details: {} },
        });
    });
}

function createWorkspace(port) {
    const workspace = realpathSync(mkdtempSync(path.join(tmpdir(), "pickles-full-flow-")));
    mkdirSync(path.join(workspace, ".pickles"), { recursive: true });
    writeFileSync(path.join(workspace, ".pickles/server.json"), JSON.stringify({ port }), "utf8");
    mkdirSync(path.join(workspace, "node_modules/@pickles"), { recursive: true });
    symlinkSync(runtimePackage, path.join(workspace, "node_modules/@pickles/runtime"), "dir");
    mkdirSync(path.dirname(path.join(workspace, sampleJavaPath)), { recursive: true });
    writeFileSync(path.join(workspace, sampleJavaPath), cleanController(), "utf8");
    writeFileSync(path.join(workspace, "pickles.config.ts"), sampleConfig(), "utf8");

    git(workspace, ["init"]);
    git(workspace, ["config", "user.email", "pickles@example.test"]);
    git(workspace, ["config", "user.name", "Pickles Test"]);
    git(workspace, ["add", "."]);
    git(workspace, ["commit", "-m", "initial"]);

    return workspace;
}

function sampleConfig() {
    return `import { defineConfig, defineRule } from "@pickles/runtime/config";

const noControllerRepositoryImport = defineRule({
    id: "sample-java-no-controller-repository-import",
    title: "Controller must not import repository directly",
    message: "Controller classes must access repositories through a service layer.",
    fixHint: "Move repository access behind a service and let the controller depend on that service.",
    type: "architecture",
    severity: "ERROR",
    language: "java",
    files: ["src/main/java/**/*.java"],
    rule(ctx) {
        return ctx.java.changedFiles().flatMap((file) =>
            file.imports
                .filter((javaImport) => javaImport.name.endsWith("Repository"))
                .map((javaImport) =>
                    ctx.problem({
                        message: \`\${file.path} imports \${javaImport.name} directly.\`,
                        file: file.path,
                        position: javaImport.position,
                    }),
                ),
        );
    },
});

export default defineConfig({
    agent: "codex",
    hook: {
        protocol: "http",
    },
    rules: [noControllerRepositoryImport],
    problemBoard: {
        aggregation: "workspace",
    },
});
`;
}

function cleanController() {
    return `package com.example.web;

public class OrderController {
}
`;
}

function violatingController() {
    return `package com.example.web;

import com.example.data.OrderRepository;

public class OrderController {
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
`;
}

function changeType(file) {
    if (file.before === null) return "added";
    if (file.after === null) return "deleted";
    return "modified";
}

function runHook(cwd, input) {
    return new Promise((resolve) => {
        const child = spawn(process.execPath, [hookScript], {
            cwd,
            stdio: ["pipe", "pipe", "pipe"],
            env: process.env,
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

function git(cwd, args) {
    const result = spawnSync("git", args, { cwd, encoding: "utf8" });
    assert.equal(result.status, 0, result.stderr);
    return result.stdout;
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
