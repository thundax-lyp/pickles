import { defineConfig, defineRule } from "@pickles/runtime/config";

const noControllerRepositoryImport = defineRule({
    id: "sample-java-no-controller-repository-import",
    title: "Controller must not import repository directly",
    message: "Controller classes must access repositories through a service layer.",
    fixHint:
        "Move repository access behind a service and let the controller depend on that service.",
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
                        message: `${file.path} imports ${javaImport.name} directly.`,
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
