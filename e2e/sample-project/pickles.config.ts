export default {
    agent: "codex",
    hook: {
        protocol: "http",
    },
    rules: [
        {
            id: "sample-eslint",
            title: "Sample TypeScript validation",
            type: "external-adapter",
            severity: "ERROR",
            adapter: "eslint",
            command: "npm run lint",
        },
    ],
    problemBoard: {
        aggregation: "workspace",
    },
};
