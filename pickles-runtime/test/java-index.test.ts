import assert from "node:assert/strict";
import test from "node:test";

import {
    createJavaIndex,
    findFilesByImport,
    findType,
    findTypesByAnnotation,
    javaFiles,
} from "../src/java-index.ts";
import type { ChangedFile } from "../src/types.ts";

test("java index records added Java files", () => {
    const index = createJavaIndex([
        changedJavaFile(
            "src/App.java",
            `
            package com.example;

            import com.example.data.Repository;

            @Deprecated
            public class App {
            }
            `,
        ),
    ]);

    assert.equal(javaFiles(index).length, 1);
    assert.equal(findType(index, "com.example.App")?.name, "App");
    assert.deepEqual(
        findTypesByAnnotation(index, "Deprecated").map((type) => type.qualifiedName),
        ["com.example.App"],
    );
    assert.deepEqual(
        findFilesByImport(index, "com.example.data.Repository").map((file) => file.path),
        ["src/App.java"],
    );
});

test("java index clears old path contributions before modified files are reindexed", () => {
    const index = createJavaIndex([
        changedJavaFile(
            "src/App.java",
            `
            package com.example;
            import com.example.OldRepository;
            @Deprecated
            public class OldApp {
            }
            `,
            "added",
        ),
        changedJavaFile(
            "src/App.java",
            `
            package com.example;
            import com.example.NewRepository;
            @Audited
            public class NewApp {
            }
            `,
            "modified",
        ),
    ]);

    assert.equal(findType(index, "com.example.OldApp"), null);
    assert.equal(findType(index, "com.example.NewApp")?.name, "NewApp");
    assert.deepEqual(findTypesByAnnotation(index, "Deprecated"), []);
    assert.deepEqual(
        findTypesByAnnotation(index, "Audited").map((type) => type.qualifiedName),
        ["com.example.NewApp"],
    );
    assert.deepEqual(findFilesByImport(index, "com.example.OldRepository"), []);
    assert.deepEqual(
        findFilesByImport(index, "com.example.NewRepository").map((file) => file.path),
        ["src/App.java"],
    );
});

test("java index clears old path contributions for deleted files", () => {
    const index = createJavaIndex([
        changedJavaFile(
            "src/App.java",
            `
            package com.example;
            import com.example.Repository;
            public class App {
            }
            `,
            "added",
        ),
        {
            path: "src/App.java",
            changeType: "deleted",
            before: null,
            after: null,
        },
    ]);

    assert.deepEqual(javaFiles(index), []);
    assert.equal(findType(index, "com.example.App"), null);
    assert.deepEqual(findFilesByImport(index, "com.example.Repository"), []);
});

test("java index ignores unchanged files", () => {
    const index = createJavaIndex([
        changedJavaFile(
            "src/App.java",
            `
            package com.example;
            public class App {
            }
            `,
            "unchanged",
        ),
    ]);

    assert.deepEqual(javaFiles(index), []);
    assert.equal(findType(index, "com.example.App"), null);
});

const changedJavaFile = (
    path: string,
    after: string,
    changeType: ChangedFile["changeType"] = "added",
): ChangedFile => {
    return {
        path,
        changeType,
        before: null,
        after,
    };
};
