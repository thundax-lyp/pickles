import assert from "node:assert/strict";
import test from "node:test";

import { JavaSyntaxParser } from "../src/java-syntax-parser.ts";

test("java syntax parser can parse a minimal Java class without exposing tree-sitter objects", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/App.java",
        `
        package com.example;

        public class App {
        }
        `,
    );

    assert.equal(file.path, "src/main/java/com/example/App.java");
    assert.equal(file.packageName, "com.example");
    assert.deepEqual(file.imports, []);
    assert.equal(file.types[0].qualifiedName, "com.example.App");
    assert.deepEqual(file.types[0].extendsTypes, []);
    assert.deepEqual(file.types[0].implementsTypes, []);
    assert.deepEqual(file.diagnostics, []);
});

test("java syntax parser extracts package imports and top-level type declarations", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/web/OrderController.java",
        `
        package com.example.web;

        import com.example.data.OrderRepository;
        import java.util.List;

        @Deprecated
        public class OrderController {
        }

        interface OrderService {
        }

        enum OrderKind {
            STANDARD
        }

        @interface Audited {
        }

        record OrderSummary(String id) {
        }
        `,
    );

    assert.equal(file.packageName, "com.example.web");
    assert.deepEqual(
        file.imports.map((importDeclaration) => importDeclaration.name),
        ["com.example.data.OrderRepository", "java.util.List"],
    );
    assert.deepEqual(
        file.types.map((type) => ({
            name: type.name,
            qualifiedName: type.qualifiedName,
            annotations: type.annotations,
        })),
        [
            {
                name: "OrderController",
                qualifiedName: "com.example.web.OrderController",
                annotations: ["Deprecated"],
            },
            {
                name: "OrderService",
                qualifiedName: "com.example.web.OrderService",
                annotations: [],
            },
            {
                name: "OrderKind",
                qualifiedName: "com.example.web.OrderKind",
                annotations: [],
            },
            {
                name: "Audited",
                qualifiedName: "com.example.web.Audited",
                annotations: [],
            },
            {
                name: "OrderSummary",
                qualifiedName: "com.example.web.OrderSummary",
                annotations: [],
            },
        ],
    );
});

test("java syntax parser extracts normal static and wildcard imports", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/App.java",
        `package com.example;

import java.util.List;
import java.util.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.*;

public class App {
}
`,
    );

    assert.deepEqual(
        file.imports.map((importDeclaration) => importDeclaration.name),
        [
            "java.util.List",
            "java.util.*",
            "java.util.Collections.emptyList",
            "java.util.Collections.*",
        ],
    );
});

test("java syntax parser extracts members annotations modifiers nested types and ranges", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/App.java",
        `package com.example;

@Deprecated
public final class App {
    private final String id;

    @Inject
    public App(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    static class Nested {
    }
}
`,
    );

    const app = file.types[0];

    assert.deepEqual(app.annotations, ["Deprecated"]);
    assert.deepEqual(app.modifiers, ["public", "final"]);
    assert.deepEqual(app.position, { line: 3, column: 1 });
    assert.deepEqual(app.range?.start, { line: 3, column: 1 });
    assert.equal(app.range?.end.line, 19);
    assert.deepEqual(
        app.fields?.map((field) => ({
            name: field.name,
            modifiers: field.modifiers,
            rangeStart: field.range.start,
        })),
        [
            {
                name: "id",
                modifiers: ["private", "final"],
                rangeStart: { line: 5, column: 5 },
            },
        ],
    );
    assert.deepEqual(
        app.constructors?.map((constructor) => ({
            name: constructor.name,
            annotations: constructor.annotations,
            modifiers: constructor.modifiers,
        })),
        [
            {
                name: "App",
                annotations: ["Inject"],
                modifiers: ["public"],
            },
        ],
    );
    assert.deepEqual(
        app.methods?.map((method) => ({
            name: method.name,
            annotations: method.annotations,
            modifiers: method.modifiers,
        })),
        [
            {
                name: "toString",
                annotations: ["Override"],
                modifiers: ["public"],
            },
        ],
    );
    assert.deepEqual(
        app.nestedTypes?.map((type) => ({
            name: type.name,
            qualifiedName: type.qualifiedName,
            modifiers: type.modifiers,
        })),
        [
            {
                name: "Nested",
                qualifiedName: "com.example.App.Nested",
                modifiers: ["static"],
            },
        ],
    );
});

test("java syntax parser extracts names from generic type and method declarations", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/Repository.java",
        `package com.example;

public class Repository<T extends Entity> {
    public <R> R map(T input) {
        return null;
    }
}
`,
    );

    assert.equal(file.types[0].name, "Repository");
    assert.equal(file.types[0].qualifiedName, "com.example.Repository");
    assert.deepEqual(
        file.types[0].methods?.map((method) => method.name),
        ["map"],
    );
});

test("java syntax parser extracts field and annotation variants", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/PackageLocal.java",
        `package com.example;

@com.example.Audited
class PackageLocal {
    String firstName, lastName;

    void touch() {
    }
}
`,
    );

    const type = file.types[0];

    assert.deepEqual(type.annotations, ["com.example.Audited"]);
    assert.deepEqual(type.modifiers, []);
    assert.deepEqual(
        type.fields?.map((field) => ({
            name: field.name,
            modifiers: field.modifiers,
        })),
        [
            { name: "firstName", modifiers: [] },
            { name: "lastName", modifiers: [] },
        ],
    );
    assert.deepEqual(
        type.methods?.map((method) => ({
            name: method.name,
            modifiers: method.modifiers,
        })),
        [{ name: "touch", modifiers: [] }],
    );
});

test("java syntax parser extracts class extends and implements declarations", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/App.java",
        `package com.example;

public class App extends BaseApp implements Runnable, java.io.Closeable {
}
`,
    );

    assert.deepEqual(file.types[0].extendsTypes, ["BaseApp"]);
    assert.deepEqual(file.types[0].implementsTypes, ["Runnable", "java.io.Closeable"]);
});

test("java syntax parser extracts interface extends and record implements declarations", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/Types.java",
        `package com.example;

interface OrderService extends BaseService, java.io.Closeable {
}

record OrderSummary(String id) implements Identified, java.io.Serializable {
}
`,
    );

    assert.deepEqual(file.types[0].extendsTypes, ["BaseService", "java.io.Closeable"]);
    assert.deepEqual(file.types[0].implementsTypes, []);
    assert.deepEqual(file.types[1].extendsTypes, []);
    assert.deepEqual(file.types[1].implementsTypes, ["Identified", "java.io.Serializable"]);
});

test("java syntax parser returns diagnostics for syntax errors", () => {
    const parser = new JavaSyntaxParser();
    const file = parser.parse(
        "src/main/java/com/example/Broken.java",
        `package com.example;

public class Broken {
    public void run(
}
`,
    );

    assert.equal(file.diagnostics?.length, 1);
    assert.deepEqual(file.diagnostics?.[0], {
        message: "Java parser encountered syntax error.",
        severity: "WARN",
        file: "src/main/java/com/example/Broken.java",
        position: {
            line: 4,
            column: 5,
        },
        source: {
            tool: "tree-sitter-java",
            rule: null,
        },
        fixHint: null,
    });
});
