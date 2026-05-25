#!/usr/bin/env node

import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(fileURLToPath(new URL("..", import.meta.url)));
const ignoredDirs = new Set([
    ".git",
    ".gradle",
    ".idea",
    ".intellijPlatform",
    "build",
    "node_modules",
]);
const functionDeclarationPattern = /^\s*(?:export\s+)?(?:async\s+)?function\s+\w+/u;

const listTypeScriptFiles = async (dir) => {
    const entries = await readdir(dir, { withFileTypes: true });
    const nested = await Promise.all(
        entries.map(async (entry) => {
            const entryPath = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                return ignoredDirs.has(entry.name) ? [] : listTypeScriptFiles(entryPath);
            }
            if (!entry.isFile() || !entry.name.endsWith(".ts") || entry.name.endsWith(".d.ts")) {
                return [];
            }
            return [entryPath];
        }),
    );
    return nested.flat();
};

const checkFile = async (file) => {
    const content = await readFile(file, "utf8");
    return content
        .split("\n")
        .map((line, index) => ({ line, lineNumber: index + 1 }))
        .filter(({ line }) => functionDeclarationPattern.test(line))
        .map(({ lineNumber }) => `${path.relative(rootDir, file)}:${lineNumber}`);
};

const files = await listTypeScriptFiles(rootDir);
const violations = (await Promise.all(files.sort().map((file) => checkFile(file)))).flat();

if (violations.length > 0) {
    console.error("TypeScript function declarations must use arrow functions:");
    for (const violation of violations) {
        console.error(`- ${violation}`);
    }
    process.exitCode = 1;
}
