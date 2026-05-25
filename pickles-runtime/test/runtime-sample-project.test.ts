import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import assert from "node:assert/strict";
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
