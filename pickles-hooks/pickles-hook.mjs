#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import path from "node:path";

const SUPPORTED_EVENTS = new Set(["SessionStart", "PostToolUse", "Stop"]);

async function main() {
  const input = await readHookInput();
  const event = mapHookEvent(input);
  const workspace = discoverWorkspace(event.cwd);
  const server = await readServerFile(workspace);
  const client = createHttpClient(server.baseUrl);

  if (event.hookEventName === "SessionStart") {
    await client.getJson("/health");
    return;
  }
}

async function readHookInput() {
  const stdin = await readStdin();
  if (stdin.trim().length === 0) {
    throw new Error("Hook stdin JSON is required.");
  }

  try {
    return JSON.parse(stdin);
  } catch (error) {
    throw new Error(`Hook stdin JSON is invalid: ${error.message}`);
  }
}

function readStdin() {
  return new Promise((resolve, reject) => {
    let data = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => {
      data += chunk;
    });
    process.stdin.on("end", () => resolve(data));
    process.stdin.on("error", reject);
  });
}

function mapHookEvent(input) {
  const hookEventName = requireString(input.hook_event_name, "hook_event_name");
  if (!SUPPORTED_EVENTS.has(hookEventName)) {
    throw new Error(`Unsupported hook event: ${hookEventName}`);
  }

  const sessionId = requireString(input.session_id, "session_id");
  const turnId = input.turn_id ?? null;
  if (turnId !== null && typeof turnId !== "string") {
    throw new Error("turn_id must be a string when present.");
  }
  if (hookEventName !== "SessionStart" && turnId === null) {
    throw new Error(`${hookEventName} requires turn_id.`);
  }

  return {
    sessionId,
    turnId,
    hookEventName,
    cwd: typeof input.cwd === "string" && input.cwd.length > 0 ? input.cwd : process.cwd(),
  };
}

function requireString(value, fieldName) {
  if (typeof value !== "string" || value.length === 0) {
    throw new Error(`${fieldName} must be a non-empty string.`);
  }
  return value;
}

function discoverWorkspace(cwd) {
  const gitRoot = runGitRoot(cwd);
  if (hasServerFile(gitRoot)) {
    return gitRoot;
  }

  const nestedWorkspace = findNearestServerWorkspace(cwd, gitRoot);
  if (nestedWorkspace !== null) {
    return nestedWorkspace;
  }

  return gitRoot;
}

function runGitRoot(cwd) {
  const result = spawnSync("git", ["rev-parse", "--show-toplevel"], {
    cwd,
    encoding: "utf8",
  });

  if (result.status !== 0) {
    const reason = result.stderr.trim() || "git rev-parse --show-toplevel failed.";
    throw new Error(`Unable to locate git root: ${reason}`);
  }

  return path.resolve(result.stdout.trim());
}

function findNearestServerWorkspace(cwd, stopAt) {
  let current = path.resolve(cwd);
  const boundary = path.resolve(stopAt);

  while (current.startsWith(boundary)) {
    if (hasServerFile(current)) {
      return current;
    }
    if (current === boundary) {
      return null;
    }
    current = path.dirname(current);
  }

  return null;
}

function hasServerFile(workspace) {
  return existsSync(path.join(workspace, ".pickles", "server.json"));
}

async function readServerFile(workspace) {
  const serverPath = path.join(workspace, ".pickles", "server.json");
  let raw;
  try {
    raw = await readFile(serverPath, "utf8");
  } catch (error) {
    throw new Error(`Unable to read ${serverPath}: ${error.message}`);
  }

  let server;
  try {
    server = JSON.parse(raw);
  } catch (error) {
    throw new Error(`${serverPath} is not valid JSON: ${error.message}`);
  }

  if (typeof server.port !== "number" || !Number.isInteger(server.port) || server.port <= 0) {
    throw new Error(`${serverPath} must contain a positive numeric port.`);
  }

  return {
    port: server.port,
    baseUrl: `http://127.0.0.1:${server.port}`,
  };
}

function createHttpClient(baseUrl) {
  return {
    getJson: (route) => requestJson(baseUrl, route, "GET"),
    postJson: (route, body) => requestJson(baseUrl, route, "POST", body),
  };
}

async function requestJson(baseUrl, route, method, body = undefined) {
  if (typeof fetch !== "function") {
    throw new Error("Node.js built-in fetch is required.");
  }

  const response = await fetch(`${baseUrl}${route}`, {
    method,
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  const text = await response.text();
  const payload = parseJsonResponse(route, text);
  if (!response.ok) {
    throw new Error(`${method} ${route} failed with HTTP ${response.status}: ${text}`);
  }
  return payload;
}

function parseJsonResponse(route, text) {
  try {
    return text.length === 0 ? null : JSON.parse(text);
  } catch (error) {
    throw new Error(`${route} returned invalid JSON: ${error.message}`);
  }
}

main()
  .then(() => {
    process.exit(0);
  })
  .catch((error) => {
    process.stderr.write(`Pickles hook failed: ${error.message}\n`);
    process.exit(1);
  });
