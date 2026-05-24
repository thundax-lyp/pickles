#!/usr/bin/env node

const SUPPORTED_EVENTS = new Set(["SessionStart", "PostToolUse", "Stop"]);

async function main() {
  const input = await readHookInput();
  mapHookEvent(input);
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

main()
  .then(() => {
    process.exit(0);
  })
  .catch((error) => {
    process.stderr.write(`Pickles hook failed: ${error.message}\n`);
    process.exit(1);
  });
