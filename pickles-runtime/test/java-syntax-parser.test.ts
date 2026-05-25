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

    assert.deepEqual(file, {
        path: "src/main/java/com/example/App.java",
        packageName: null,
        imports: [],
        types: [],
        diagnostics: [],
    });
});
