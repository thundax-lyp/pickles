import { buildCheckoutSummary } from "./checkout.js";
import type { OrderDraft } from "./orderTypes.js";

const SCENARIOS: OrderDraft[] = [
  {
    id: "web-gold-001",
    channel: "web",
    customerTier: "gold",
    couponCode: "PICKLES10",
    lines: [
      { sku: "TS-BOOK", quantity: 1, unitPriceCents: 4500 },
      { sku: "LINT-MUG", quantity: 2, unitPriceCents: 1800 },
    ],
  },
  {
    id: "store-standard-001",
    channel: "store",
    customerTier: "standard",
    lines: [
      { sku: "AGENT-NOTEBOOK", quantity: 3, unitPriceCents: 1200 },
    ],
  },
  {
    id: "partner-enterprise-001",
    channel: "partner",
    customerTier: "enterprise",
    lines: [
      { sku: "GOVERNANCE-SEAT", quantity: 5, unitPriceCents: 9900 },
    ],
  },
];

export function summarizeScenarios(): string[] {
  return SCENARIOS.map((scenario) => {
    const summary = buildCheckoutSummary(scenario);
    return `${summary.orderId}: ${summary.channelLabel} payable=${summary.payableCents}`;
  });
}

console.log(summarizeScenarios().join("\n"));
