import Parser from "tree-sitter";
import Java from "tree-sitter-java";

import type {
    JavaFieldDeclaration,
    JavaImportDeclaration,
    JavaMethodDeclaration,
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

const MODIFIER_NODES = new Set([
    "public",
    "protected",
    "private",
    "abstract",
    "static",
    "final",
    "strictfp",
    "default",
    "synchronized",
    "native",
    "transient",
    "volatile",
    "sealed",
    "non-sealed",
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
        .map((node) => parseTypeDeclaration(node, packageName));
};

const parseTypeDeclaration = (
    node: Parser.SyntaxNode,
    packageName: string | null,
): JavaTypeDeclaration => {
    const name = node.childForFieldName("name")?.text ?? "Anonymous";
    const qualifiedName = packageName === null ? name : `${packageName}.${name}`;
    const body = node.childForFieldName("body");

    return {
        name,
        qualifiedName,
        annotations: parseAnnotationNames(node),
        modifiers: parseModifiers(node),
        methods: body === null ? [] : parseMethods(body),
        constructors: body === null ? [] : parseConstructors(body),
        fields: body === null ? [] : parseFields(body),
        nestedTypes:
            body === null
                ? []
                : body.namedChildren
                      .filter((child) => TYPE_DECLARATION_NODES.has(child.type))
                      .map((child) => parseTypeDeclaration(child, qualifiedName)),
        position: toPosition(node.startPosition),
        range: toRange(node),
    };
};

const parseMethods = (body: Parser.SyntaxNode): JavaMethodDeclaration[] => {
    return body.namedChildren
        .filter((child) => child.type === "method_declaration")
        .map((node) => {
            return {
                name: node.childForFieldName("name")?.text ?? "anonymous",
                annotations: parseAnnotationNames(node),
                modifiers: parseModifiers(node),
                position: toPosition(node.startPosition),
                range: toRange(node),
            };
        });
};

const parseConstructors = (body: Parser.SyntaxNode): JavaMethodDeclaration[] => {
    return body.namedChildren
        .filter((child) => child.type === "constructor_declaration")
        .map((node) => {
            return {
                name: node.childForFieldName("name")?.text ?? "anonymous",
                annotations: parseAnnotationNames(node),
                modifiers: parseModifiers(node),
                position: toPosition(node.startPosition),
                range: toRange(node),
            };
        });
};

const parseFields = (body: Parser.SyntaxNode): JavaFieldDeclaration[] => {
    return body.namedChildren
        .filter((child) => child.type === "field_declaration")
        .flatMap((node) => {
            const declarators = node.namedChildren.filter((child) => child.type === "variable_declarator");

            return declarators.map((declarator) => {
                return {
                    name: declarator.childForFieldName("name")?.text ?? "anonymous",
                    annotations: parseAnnotationNames(node),
                    modifiers: parseModifiers(node),
                    position: toPosition(node.startPosition),
                    range: toRange(node),
                };
            });
        });
};

const parseAnnotationNames = (node: Parser.SyntaxNode): string[] => {
    const modifiers = node.namedChildren.find((child) => child.type === "modifiers");

    if (modifiers === undefined) {
        return [];
    }

    return modifiers.namedChildren
        .filter((child) => child.type.endsWith("annotation"))
        .map((annotation) => annotation.childForFieldName("name")?.text ?? annotation.namedChildren[0]?.text)
        .filter((name): name is string => name !== undefined);
};

const parseModifiers = (node: Parser.SyntaxNode): string[] => {
    const modifiers = node.namedChildren.find((child) => child.type === "modifiers");

    if (modifiers === undefined) {
        return [];
    }

    return modifiers.children
        .filter((child) => MODIFIER_NODES.has(child.type))
        .map((child) => child.type);
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
