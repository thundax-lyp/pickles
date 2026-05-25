import type {
    ChangedFile,
    JavaImportDeclaration,
    JavaSyntaxFile,
    JavaTypeDeclaration,
} from "./types.ts";

interface JavaIndex {
    filesByPath: Map<string, JavaSyntaxFile>;
    typesByQualifiedName: Map<string, JavaTypeDeclaration>;
    typeNamesByAnnotation: Map<string, Set<string>>;
    filePathsByImport: Map<string, Set<string>>;
}

export function createJavaIndex(changedFiles: ChangedFile[]): JavaIndex {
    const index: JavaIndex = {
        filesByPath: new Map(),
        typesByQualifiedName: new Map(),
        typeNamesByAnnotation: new Map(),
        filePathsByImport: new Map(),
    };

    for (const changedFile of changedFiles) {
        if (changedFile.changeType === "unchanged" || changedFile.changeType === "deleted") {
            continue;
        }

        if (!changedFile.path.endsWith(".java") || changedFile.after === null) {
            continue;
        }

        const javaFile = parseJavaFile(changedFile.path, changedFile.after);
        index.filesByPath.set(javaFile.path, javaFile);

        for (const type of javaFile.types) {
            index.typesByQualifiedName.set(type.qualifiedName, type);

            for (const annotation of type.annotations) {
                const typeNames = index.typeNamesByAnnotation.get(annotation) ?? new Set<string>();
                typeNames.add(type.qualifiedName);
                index.typeNamesByAnnotation.set(annotation, typeNames);
            }
        }

        for (const javaImport of javaFile.imports) {
            const filePaths = index.filePathsByImport.get(javaImport.name) ?? new Set<string>();
            filePaths.add(javaFile.path);
            index.filePathsByImport.set(javaImport.name, filePaths);
        }
    }

    return index;
}

export function javaFiles(index: JavaIndex): JavaSyntaxFile[] {
    return [...index.filesByPath.values()];
}

export function findType(index: JavaIndex, qualifiedName: string): JavaTypeDeclaration | null {
    return index.typesByQualifiedName.get(qualifiedName) ?? null;
}

export function findTypesByAnnotation(
    index: JavaIndex,
    annotationName: string,
): JavaTypeDeclaration[] {
    const names = index.typeNamesByAnnotation.get(annotationName) ?? new Set<string>();
    return [...names]
        .map((name) => index.typesByQualifiedName.get(name))
        .filter((type) => type !== undefined);
}

export function findFilesByImport(index: JavaIndex, importTarget: string): JavaSyntaxFile[] {
    const paths = index.filePathsByImport.get(importTarget) ?? new Set<string>();
    return [...paths]
        .map((path) => index.filesByPath.get(path))
        .filter((file) => file !== undefined);
}

function parseJavaFile(path: string, content: string): JavaSyntaxFile {
    const packageName = content.match(/^\s*package\s+([a-zA-Z_][\w.]*)\s*;/m)?.[1] ?? null;
    const imports = parseImports(content);
    const types = parseTypes(content, packageName);

    return {
        path,
        packageName,
        imports,
        types,
    };
}

function parseImports(content: string): JavaImportDeclaration[] {
    const imports: JavaImportDeclaration[] = [];
    const importPattern = /^[ \t]*import\s+([a-zA-Z_][\w.*]*)\s*;/gm;
    let match: RegExpExecArray | null;

    while ((match = importPattern.exec(content)) !== null) {
        imports.push({
            name: match[1],
            position: positionAt(content, match.index),
        });
    }

    return imports;
}

function parseTypes(content: string, packageName: string | null): JavaTypeDeclaration[] {
    const types: JavaTypeDeclaration[] = [];
    const typePattern =
        /((?:^[ \t]*@([a-zA-Z_][\w.]*)[^\n]*\n)*)^[ \t]*(?:public\s+|private\s+|protected\s+|abstract\s+|final\s+)*?(?:class|interface|enum|record|@interface)\s+([a-zA-Z_]\w*)/gm;
    let match: RegExpExecArray | null;

    while ((match = typePattern.exec(content)) !== null) {
        const name = match[3];
        const annotations = [...match[1].matchAll(/^\s*@([a-zA-Z_][\w.]*)/gm)].map(
            (annotation) => annotation[1],
        );

        types.push({
            name,
            qualifiedName: packageName === null ? name : `${packageName}.${name}`,
            annotations,
            position: positionAt(content, match.index),
        });
    }

    return types;
}

function positionAt(content: string, offset: number) {
    const prefix = content.slice(0, offset);
    const lines = prefix.split("\n");

    return {
        line: lines.length,
        column: lines[lines.length - 1].length + 1,
    };
}
