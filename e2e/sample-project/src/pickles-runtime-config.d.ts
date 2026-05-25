declare module "@pickles/runtime/config" {
    export type ProblemInput = {
        title?: string;
        message: string;
        file?: string | null;
        position?: { line: number; column: number } | null;
        source?: { tool?: string; rule?: string | null };
    };

    export type SourceFile = {
        path: string;
        language?: string;
    };

    export type SyntaxCapture = {
        name: string;
        kind: string;
        text: string;
        range: {
            start: { line: number; column: number };
            end: { line: number; column: number };
        };
    };

    export type SyntaxMatch = SyntaxCapture & {
        captures: SyntaxCapture[];
    };

    export type RuleContext = {
        workspaceRoot: string;
        changedFiles: SourceFile[];
        files: {
            changed(language?: string): SourceFile[];
        };
        java: {
            files(): SourceFile[];
            changedFiles(): SourceFile[];
            findType(qualifiedName: string): SyntaxMatch | null;
            findTypesByAnnotation(annotationName: string): SyntaxMatch[];
            findFilesByImport(importTarget: string): SourceFile[];
            query(file: SourceFile, query: string): SyntaxMatch[];
        };
        problem(input: ProblemInput): ProblemInput;
    };

    export function defineConfig<TConfig extends object>(config: TConfig): TConfig;

    export function defineRule<TRule extends object>(rule: TRule): TRule;
}
