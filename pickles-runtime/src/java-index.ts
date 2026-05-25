import { JavaSyntaxParser } from "./java-syntax-parser.ts";
import type { ChangedFile, JavaSyntaxFile, JavaTypeDeclaration } from "./types.ts";

interface JavaIndex {
    filesByPath: Map<string, JavaSyntaxFile>;
    typesByQualifiedName: Map<string, JavaTypeDeclaration>;
    typeNamesByAnnotation: Map<string, Set<string>>;
    filePathsByImport: Map<string, Set<string>>;
    contributionsByPath: Map<string, JavaIndexContribution>;
}

interface JavaIndexContribution {
    qualifiedTypes: Set<string>;
    annotations: Set<string>;
    imports: Set<string>;
}

export class WorkspaceIndexService {
    private readonly parser = new JavaSyntaxParser();
    private readonly index: JavaIndex = {
        filesByPath: new Map(),
        typesByQualifiedName: new Map(),
        typeNamesByAnnotation: new Map(),
        filePathsByImport: new Map(),
        contributionsByPath: new Map(),
    };

    update(changedFiles: ChangedFile[]): JavaIndex {
        for (const changedFile of changedFiles) {
            if (changedFile.changeType === "unchanged") {
                continue;
            }

            if (!changedFile.path.endsWith(".java")) {
                continue;
            }

            this.removePathContributions(changedFile.path);

            if (changedFile.changeType === "deleted" || changedFile.after === null) {
                continue;
            }

            this.addJavaFile(this.parser.parse(changedFile.path, changedFile.after));
        }

        return this.index;
    }

    private addJavaFile(javaFile: JavaSyntaxFile): void {
        const contribution: JavaIndexContribution = {
            qualifiedTypes: new Set(),
            annotations: new Set(),
            imports: new Set(),
        };

        this.index.filesByPath.set(javaFile.path, javaFile);

        for (const type of flattenTypes(javaFile.types)) {
            contribution.qualifiedTypes.add(type.qualifiedName);
            this.index.typesByQualifiedName.set(type.qualifiedName, type);

            for (const annotation of type.annotations) {
                contribution.annotations.add(annotation);
                const typeNames =
                    this.index.typeNamesByAnnotation.get(annotation) ?? new Set<string>();
                typeNames.add(type.qualifiedName);
                this.index.typeNamesByAnnotation.set(annotation, typeNames);
            }
        }

        for (const javaImport of javaFile.imports) {
            contribution.imports.add(javaImport.name);
            const filePaths =
                this.index.filePathsByImport.get(javaImport.name) ?? new Set<string>();
            filePaths.add(javaFile.path);
            this.index.filePathsByImport.set(javaImport.name, filePaths);
        }

        this.index.contributionsByPath.set(javaFile.path, contribution);
    }

    private removePathContributions(path: string): void {
        const contribution = this.index.contributionsByPath.get(path);

        if (contribution === undefined) {
            this.index.filesByPath.delete(path);
            return;
        }

        for (const qualifiedType of contribution.qualifiedTypes) {
            this.index.typesByQualifiedName.delete(qualifiedType);
        }

        for (const annotation of contribution.annotations) {
            const typeNames = this.index.typeNamesByAnnotation.get(annotation);
            if (typeNames === undefined) {
                continue;
            }

            for (const qualifiedType of contribution.qualifiedTypes) {
                typeNames.delete(qualifiedType);
            }

            if (typeNames.size === 0) {
                this.index.typeNamesByAnnotation.delete(annotation);
            }
        }

        for (const javaImport of contribution.imports) {
            const filePaths = this.index.filePathsByImport.get(javaImport);
            if (filePaths === undefined) {
                continue;
            }

            filePaths.delete(path);

            if (filePaths.size === 0) {
                this.index.filePathsByImport.delete(javaImport);
            }
        }

        this.index.filesByPath.delete(path);
        this.index.contributionsByPath.delete(path);
    }
}

export const createJavaIndex = (changedFiles: ChangedFile[]): JavaIndex => {
    return new WorkspaceIndexService().update(changedFiles);
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
