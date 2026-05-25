import { readFile } from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

import {
    createJavaIndex,
    findFilesByImport,
    findType,
    findTypesByAnnotation,
    javaFiles,
} from "./java-index.ts";
import type {
    ChangedFile,
    PicklesNativeRule,
    PicklesRuntimeConfig,
    Problem,
    ProblemInput,
    ParserDiagnostic,
    RuleContext,
    RuntimeCheckInput,
    RuntimeCheckResult,
} from "./types.ts";

const CONFIG_CANDIDATES = ["pickles.config.ts", "pickles.config.mjs", "pickles.config.js"];
const MAX_CHANGED_FILES = 200;
const MAX_PARSE_INPUT_BYTES = 2 * 1024 * 1024;

export const runRuntimeCheck = async (input: RuntimeCheckInput): Promise<RuntimeCheckResult> => {
    const config = await loadRuntimeConfig(input.workspaceRoot);
    validateConfig(config);

    const activeChangedFiles = input.changedFiles.filter((file) => file.changeType !== "unchanged");
    validateRuntimeInput(activeChangedFiles);
    const problems: Problem[] = [];

    for (const rule of config.rules) {
        if (rule.language !== "java") {
            throw new Error(`Unsupported rule language: ${rule.language}`);
        }

        const ruleChangedFiles = activeChangedFiles.filter((file) =>
            matchesAnyGlob(file.path, rule.files),
        );
        const javaIndex = createJavaIndex(ruleChangedFiles);
        problems.push(
            ...javaFiles(javaIndex).flatMap((file) =>
                (file.diagnostics ?? []).map(parserDiagnosticToProblem),
            ),
        );

        const context: RuleContext = {
            workspaceRoot: input.workspaceRoot,
            changedFiles: ruleChangedFiles,
            files: {
                changed: (language?: string) =>
                    ruleChangedFiles.filter(
                        (file) => language === undefined || matchesLanguage(file, language),
                    ),
                byGlob: (patterns: string | string[]) =>
                    ruleChangedFiles.filter((file) =>
                        matchesAnyGlob(file.path, Array.isArray(patterns) ? patterns : [patterns]),
                    ),
                read: async (file: string) =>
                    readFile(path.join(input.workspaceRoot, file), "utf8"),
            },
            syntax: {
                query: unsupportedQuery,
            },
            java: {
                files: () => javaFiles(javaIndex),
                changedFiles: () => javaFiles(javaIndex),
                findType: (qualifiedName: string) => findType(javaIndex, qualifiedName),
                findTypesByAnnotation: (annotationName: string) =>
                    findTypesByAnnotation(javaIndex, annotationName),
                findFilesByImport: (importTarget: string) =>
                    findFilesByImport(javaIndex, importTarget),
                query: unsupportedQuery,
            },
            problem: (problemInput: ProblemInput) => problemInput,
        };

        const ruleProblems = await rule.rule(context);
        problems.push(...ruleProblems.map((problem) => normalizeProblem(rule, problem)));
    }

    return {
        problems: dedupeProblems(problems),
    };
};

const validateRuntimeInput = (changedFiles: ChangedFile[]): void => {
    if (changedFiles.length > MAX_CHANGED_FILES) {
        throw new Error(
            `Runtime changedFiles limit exceeded: received ${changedFiles.length}, maximum is ${MAX_CHANGED_FILES}.`,
        );
    }

    for (const file of changedFiles) {
        if (file.after === null) {
            continue;
        }

        const byteLength = Buffer.byteLength(file.after, "utf8");
        if (byteLength > MAX_PARSE_INPUT_BYTES) {
            throw new Error(
                `Runtime parse input limit exceeded for ${file.path}: received ${byteLength} bytes, maximum is ${MAX_PARSE_INPUT_BYTES} bytes.`,
            );
        }
    }
};

const parserDiagnosticToProblem = (diagnostic: ParserDiagnostic): Problem => {
    return {
        title: "Java parser diagnostic",
        type: "parser",
        message: diagnostic.message,
        severity: diagnostic.severity,
        source: diagnostic.source,
        file: diagnostic.file,
        position: diagnostic.position,
        fixHint: diagnostic.fixHint,
    };
};

const loadRuntimeConfig = async (workspaceRoot: string): Promise<PicklesRuntimeConfig> => {
    for (const candidate of CONFIG_CANDIDATES) {
        const configPath = path.join(workspaceRoot, candidate);

        try {
            await readFile(configPath, "utf8");
        } catch {
            continue;
        }

        const configUrl = pathToFileURL(configPath);
        configUrl.searchParams.set("mtime", String(Date.now()));
        const module = (await import(configUrl.href)) as { default?: PicklesRuntimeConfig };

        if (module.default === undefined) {
            throw new Error(`${candidate} must export default defineConfig(...)`);
        }

        return module.default;
    }

    throw new Error(`Pickles runtime config not found in ${workspaceRoot}`);
};

const validateConfig = (config: PicklesRuntimeConfig): void => {
    if (config.agent !== "codex") {
        throw new Error("Pickles config agent must be codex");
    }

    if (config.hook?.protocol !== "http") {
        throw new Error("Pickles config hook.protocol must be http");
    }

    if (!Array.isArray(config.rules)) {
        throw new Error("Pickles config rules must be an array");
    }

    for (const rule of config.rules) {
        for (const field of [
            "id",
            "title",
            "message",
            "type",
            "severity",
            "language",
            "files",
            "rule",
        ] as const) {
            if (rule[field] === undefined || rule[field] === null) {
                throw new Error(`Pickles native rule is missing ${field}`);
            }
        }
    }
};

const normalizeProblem = (
    rule: PicklesNativeRule,
    problem: {
        title?: string;
        message: string;
        file?: string | null;
        position?: Problem["position"];
        fixHint?: string | null;
    },
): Problem => {
    if (problem.message === undefined || problem.message.length === 0) {
        throw new Error(`Rule ${rule.id} returned a problem without message`);
    }

    return {
        title: problem.title ?? rule.title,
        type: rule.type,
        message: problem.message,
        severity: rule.severity,
        source: {
            tool: "pickles-native",
            rule: rule.id,
        },
        file: problem.file ?? null,
        position: problem.position ?? null,
        fixHint: problem.fixHint ?? rule.fixHint ?? null,
    };
};

const dedupeProblems = (problems: Problem[]): Problem[] => {
    const seen = new Set<string>();
    const deduped: Problem[] = [];

    for (const problem of problems) {
        const key = [
            problem.source.tool,
            problem.source.rule,
            problem.file,
            problem.position?.line ?? null,
            problem.position?.column ?? null,
            problem.message,
        ].join("\u0000");

        if (seen.has(key)) {
            continue;
        }

        seen.add(key);
        deduped.push(problem);
    }

    return deduped;
};

const matchesLanguage = (file: ChangedFile, language: string): boolean => {
    return language === "java" && file.path.endsWith(".java");
};

const matchesAnyGlob = (filePath: string, patterns: string[]): boolean => {
    return patterns.some((pattern) => {
        if (pattern === filePath) {
            return true;
        }

        if (pattern.endsWith("/**/*.java")) {
            const prefix = pattern.slice(0, -"**/*.java".length);
            return filePath.startsWith(prefix) && filePath.endsWith(".java");
        }

        if (pattern.startsWith("**/*.")) {
            return filePath.endsWith(pattern.slice("**/*".length));
        }

        return false;
    });
};

const unsupportedQuery = (): never => {
    throw new Error("Syntax query is not implemented in the runtime sample testcase baseline");
};

export type { ChangedFile, PicklesRuntimeConfig, Problem, RuntimeCheckResult } from "./types.ts";
