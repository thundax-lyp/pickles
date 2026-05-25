declare module "@pickles/runtime/config" {
    export type ProblemInput = {
        title?: string;
        message: string;
        file?: string | null;
        position?: { line: number; column: number } | null;
        source?: { tool?: string; rule?: string | null };
    };

    export type RuleContext = {
        problem(input: ProblemInput): ProblemInput;
    };

    export function defineConfig<TConfig extends object>(config: TConfig): TConfig;

    export function defineRule<TRule extends object>(rule: TRule): TRule;
}
