import { stdin, stdout } from "node:process";

import { runRuntimeCheck } from "./index.ts";
import type { RuntimeCheckInput, RuntimeStdioResponse } from "./types.ts";

const main = async (): Promise<void> => {
    try {
        const request = parseRequest(await readStdin());
        writeResponse(await runRuntimeCheck(request));
    } catch (error) {
        process.exitCode = 1;
        writeResponse({
            error: {
                message: error instanceof Error ? error.message : String(error),
            },
        });
    }
};

const readStdin = (): Promise<string> => {
    return new Promise((resolve, reject) => {
        let data = "";
        stdin.setEncoding("utf8");
        stdin.on("data", (chunk) => {
            data += chunk;
        });
        stdin.on("end", () => resolve(data));
        stdin.on("error", reject);
    });
};

const parseRequest = (input: string): RuntimeCheckInput => {
    if (input.trim().length === 0) {
        throw new Error("Runtime stdio request JSON is required.");
    }

    const request = JSON.parse(input) as RuntimeCheckInput;
    if (typeof request.workspaceRoot !== "string" || request.workspaceRoot.length === 0) {
        throw new Error("Runtime stdio request workspaceRoot must be a non-empty string.");
    }
    if (!Array.isArray(request.changedFiles)) {
        throw new Error("Runtime stdio request changedFiles must be an array.");
    }
    return request;
};

const writeResponse = (response: RuntimeStdioResponse): void => {
    stdout.write(`${JSON.stringify(response)}\n`);
};

await main();
