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
