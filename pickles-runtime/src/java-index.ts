import { JavaSyntaxParser } from "./java-syntax-parser.ts";
import type { ChangedFile, JavaSyntaxFile, JavaTypeDeclaration } from "./types.ts";

interface JavaIndex {
    filesByPath: Map<string, JavaSyntaxFile>;
    typesByQualifiedName: Map<string, JavaTypeDeclaration>;
    typeNamesByAnnotation: Map<string, Set<string>>;
    filePathsByImport: Map<string, Set<string>>;
}

export const createJavaIndex = (changedFiles: ChangedFile[]): JavaIndex => {
    const parser = new JavaSyntaxParser();
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

        const javaFile = parser.parse(changedFile.path, changedFile.after);
        index.filesByPath.set(javaFile.path, javaFile);

        for (const type of flattenTypes(javaFile.types)) {
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
};

const flattenTypes = (types: JavaTypeDeclaration[]): JavaTypeDeclaration[] => {
    return types.flatMap((type) => [type, ...flattenTypes(type.nestedTypes ?? [])]);
};

export const javaFiles = (index: JavaIndex): JavaSyntaxFile[] => {
    return [...index.filesByPath.values()];
};

export const findType = (index: JavaIndex, qualifiedName: string): JavaTypeDeclaration | null => {
    return index.typesByQualifiedName.get(qualifiedName) ?? null;
};

export const findTypesByAnnotation = (
    index: JavaIndex,
    annotationName: string,
): JavaTypeDeclaration[] => {
    const names = index.typeNamesByAnnotation.get(annotationName) ?? new Set<string>();
    return [...names]
        .map((name) => index.typesByQualifiedName.get(name))
        .filter((type) => type !== undefined);
};

export const findFilesByImport = (index: JavaIndex, importTarget: string): JavaSyntaxFile[] => {
    const paths = index.filePathsByImport.get(importTarget) ?? new Set<string>();
    return [...paths]
        .map((path) => index.filesByPath.get(path))
        .filter((file) => file !== undefined);
};
