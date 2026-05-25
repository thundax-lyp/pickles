import Parser from "tree-sitter";
import Java from "tree-sitter-java";

import type {
    JavaImportDeclaration,
    JavaSyntaxFile,
    JavaTypeDeclaration,
    Position,
    SourceRange,
} from "./types.ts";

const TYPE_DECLARATION_NODES = new Set([
    "class_declaration",
    "interface_declaration",
    "enum_declaration",
    "annotation_type_declaration",
    "record_declaration",
]);

export class JavaSyntaxParser {
    private readonly parser: Parser;

    constructor() {
        this.parser = new Parser();
        this.parser.setLanguage(Java);
    }

    parse(path: string, content: string): JavaSyntaxFile {
        const tree = this.parser.parse(content);
        const root = tree.rootNode;
        const packageName = parsePackageName(root);

        return {
            path,
            packageName,
            imports: parseImports(root),
            types: parseTopLevelTypes(root, packageName),
            diagnostics: [],
        };
    }
}

const parsePackageName = (root: Parser.SyntaxNode): string | null => {
    const packageDeclaration = root.namedChildren.find((child) => child.type === "package_declaration");
    const nameNode = packageDeclaration?.namedChildren[0] ?? null;

    return nameNode?.text ?? null;
};

const parseImports = (root: Parser.SyntaxNode): JavaImportDeclaration[] => {
    return root.namedChildren
        .filter((child) => child.type === "import_declaration")
        .map((node) => {
            const importName = node.namedChildren.map((child) => child.text).join(".");

            return {
                name: importName,
                position: toPosition(node.startPosition),
                range: toRange(node),
            };
        });
};

const parseTopLevelTypes = (
    root: Parser.SyntaxNode,
    packageName: string | null,
): JavaTypeDeclaration[] => {
    return root.namedChildren
        .filter((child) => TYPE_DECLARATION_NODES.has(child.type))
        .map((node) => {
            const name = node.childForFieldName("name")?.text ?? "Anonymous";
            const annotations = parseAnnotationNames(node);

            return {
                name,
                qualifiedName: packageName === null ? name : `${packageName}.${name}`,
                annotations,
                modifiers: [],
                methods: [],
                constructors: [],
                fields: [],
                position: toPosition(node.startPosition),
                range: toRange(node),
            };
        });
};

const parseAnnotationNames = (node: Parser.SyntaxNode): string[] => {
    const modifiers = node.namedChildren.find((child) => child.type === "modifiers");

    if (modifiers === undefined) {
        return [];
    }

    return modifiers.namedChildren
        .filter((child) => child.type === "marker_annotation" || child.type === "annotation")
        .map((annotation) => annotation.childForFieldName("name")?.text ?? annotation.namedChildren[0]?.text)
        .filter((name): name is string => name !== undefined);
};

const toPosition = (point: Parser.Point): Position => {
    return {
        line: point.row + 1,
        column: point.column + 1,
    };
};

const toRange = (node: Parser.SyntaxNode): SourceRange => {
    return {
        start: toPosition(node.startPosition),
        end: toPosition(node.endPosition),
    };
};
