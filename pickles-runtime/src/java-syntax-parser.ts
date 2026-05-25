import Parser from "tree-sitter";
import Java from "tree-sitter-java";

import type { JavaSyntaxFile } from "./types.ts";

export class JavaSyntaxParser {
    private readonly parser: Parser;

    constructor() {
        this.parser = new Parser();
        this.parser.setLanguage(Java);
    }

    parse(path: string, content: string): JavaSyntaxFile {
        this.parser.parse(content);

        return {
            path,
            packageName: null,
            imports: [],
            types: [],
            diagnostics: [],
        };
    }
}
