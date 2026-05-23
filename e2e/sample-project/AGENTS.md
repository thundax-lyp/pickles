# Sample Project Agent Rules

This sample exists to exercise Pickles while Codex edits a small TypeScript project.

## Working Rules

- Treat this directory as the target user project.
- Do not edit files outside `e2e/sample-project/`.
- Type changes must keep `src/orderTypes.ts`, `src/pricing.ts`, `src/checkout.ts`, and `src/scenarios.ts` synchronized.
- Adding a new order channel requires updating the channel union, channel labels, discount matrix, checkout validation, and scenario table.
- Partial edits are expected to fail `npm run typecheck`.

## Validation

- Run `npm install` once if dependencies are missing.
- Run `npm run typecheck` after TypeScript edits.
- Run `npm run lint` before reporting success.
