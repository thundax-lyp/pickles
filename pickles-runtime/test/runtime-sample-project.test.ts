import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import test from "node:test";

import { runRuntimeCheck } from "../src/index.ts";

const repoRoot = path.resolve(fileURLToPath(new URL("../..", import.meta.url)));
const sampleProjectRoot = path.join(repoRoot, "e2e/sample-project");
const sampleJavaPath = "src/main/java/com/example/web/OrderController.java";

test("runtime executes sample project native rule and returns a Problem", async () => {
    const after = await readFile(path.join(sampleProjectRoot, sampleJavaPath), "utf8");

    const result = await runRuntimeCheck({
        workspaceRoot: sampleProjectRoot,
        changedFiles: [
            {
                path: sampleJavaPath,
                changeType: "modified",
                before: null,
                after,
            },
        ],
    });

    assert.equal(result.problems.length, 1);
    assert.deepEqual(result.problems[0], {
        title: "Controller must not import repository directly",
        type: "architecture",
        message:
            "src/main/java/com/example/web/OrderController.java imports com.example.data.OrderRepository directly.",
        severity: "ERROR",
        source: {
            tool: "pickles-native",
            rule: "sample-java-no-controller-repository-import",
        },
        file: sampleJavaPath,
        position: {
            line: 3,
            column: 1,
        },
        fixHint:
            "Move repository access behind a service and let the controller depend on that service.",
    });
});

test("runtime stdio host returns a Problem for a valid request", async () => {
    const after = await readFile(path.join(sampleProjectRoot, sampleJavaPath), "utf8");
    const result = runRuntimeStdio({
        workspaceRoot: sampleProjectRoot,
        changedFiles: [
            {
                path: sampleJavaPath,
                changeType: "modified",
                before: null,
                after,
            },
        ],
    });

    assert.equal(result.status, 0, result.stderr);
    const response = JSON.parse(result.stdout);
    assert.equal(response.problems.length, 1);
    assert.equal(response.problems[0].source.rule, "sample-java-no-controller-repository-import");
});

test("runtime stdio host returns an error for an invalid request", () => {
    const result = runRuntimeStdio({
        changedFiles: [],
    });

    assert.notEqual(result.status, 0);
    const response = JSON.parse(result.stdout);
    assert.equal(
        response.error.message,
        "Runtime stdio request workspaceRoot must be a non-empty string.",
    );
});

const runRuntimeStdio = (request: unknown) => {
    return spawnSync(process.execPath, ["--import", "tsx", "src/stdio.ts"], {
        cwd: path.join(repoRoot, "pickles-runtime"),
        input: JSON.stringify(request),
        encoding: "utf8",
    });
};
