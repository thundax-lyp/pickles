export type ProblemType =
    | "architecture"
    | "style"
    | "security"
    | "maintainability"
    | "parser"
    | "adapter";

export type Severity = "ERROR" | "WARN";

export interface Position {
    line: number;
    column: number;
}

export interface ProblemSource {
    tool: string;
    rule: string | null;
}

export interface Problem {
    title: string;
    type: ProblemType;
    message: string;
    severity: Severity;
    source: ProblemSource;
    file: string | null;
    position: Position | null;
    fixHint: string | null;
}

export interface SourceRange {
    start: Position;
    end: Position;
}

export interface ProblemInput {
    title?: string;
    message: string;
    file?: string | null;
    position?: Position | null;
    fixHint?: string | null;
}

export type ChangeType = "added" | "modified" | "deleted" | "unchanged";

export interface ChangedFile {
    path: string;
    changeType: ChangeType;
    before: string | null;
    after: string | null;
}

export interface PicklesRuntimeConfig {
    agent: "codex";
    hook: {
        protocol: "http";
    };
    rules: PicklesNativeRule[];
    problemBoard: {
        aggregation: "workspace";
    };
}

export interface PicklesNativeRule {
    id: string;
    title: string;
    message: string;
    fixHint?: string | null;
    type: ProblemType;
    severity: Severity;
    language: string;
    files: string[];
    options?: unknown;
    rule: (ctx: RuleContext) => ProblemInput[] | Promise<ProblemInput[]>;
}

export interface JavaImportDeclaration {
    name: string;
    position: Position;
    range?: SourceRange;
}

export interface JavaTypeDeclaration {
    name: string;
    qualifiedName: string;
    annotations: string[];
    extendsTypes?: string[];
    implementsTypes?: string[];
    modifiers?: string[];
    methods?: JavaMethodDeclaration[];
    constructors?: JavaMethodDeclaration[];
    fields?: JavaFieldDeclaration[];
    nestedTypes?: JavaTypeDeclaration[];
    position: Position;
    range?: SourceRange;
}

export interface JavaMethodDeclaration {
    name: string;
    annotations: string[];
    modifiers: string[];
    position: Position;
    range: SourceRange;
}

export interface JavaFieldDeclaration {
    name: string;
    annotations: string[];
    modifiers: string[];
    position: Position;
    range: SourceRange;
}

export interface ParserDiagnostic {
    message: string;
    severity: Severity;
    file: string;
    position: Position;
    source: ProblemSource;
    fixHint: string | null;
}

export interface JavaSyntaxFile {
    path: string;
    packageName: string | null;
    imports: JavaImportDeclaration[];
    types: JavaTypeDeclaration[];
    diagnostics?: ParserDiagnostic[];
}

export interface RuleContext {
    workspaceRoot: string;
    changedFiles: ChangedFile[];
    files: {
        changed: (language?: string) => ChangedFile[];
        byGlob: (patterns: string | string[]) => ChangedFile[];
        read: (file: string) => Promise<string>;
    };
    syntax: {
        query: () => never;
    };
    java: {
        files: () => JavaSyntaxFile[];
        changedFiles: () => JavaSyntaxFile[];
        findType: (qualifiedName: string) => JavaTypeDeclaration | null;
        findTypesByAnnotation: (annotationName: string) => JavaTypeDeclaration[];
        findFilesByImport: (importTarget: string) => JavaSyntaxFile[];
        query: () => never;
    };
    problem: (input: ProblemInput) => ProblemInput;
}

export interface RuntimeCheckInput {
    workspaceRoot: string;
    changedFiles: ChangedFile[];
}

export interface RuntimeCheckResult {
    problems: Problem[];
}

export interface RuntimeStdioError {
    error: {
        message: string;
    };
}

export type RuntimeStdioResponse = RuntimeCheckResult | RuntimeStdioError;
