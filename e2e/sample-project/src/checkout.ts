import { CHANNEL_LABELS, type CheckoutSummary, type OrderDraft } from "./orderTypes.js";
import { calculateDiscountCents, calculateSubtotalCents } from "./pricing.js";

export function validateOrder(order: OrderDraft): string[] {
  const errors: string[] = [];

  if (order.lines.length === 0) {
    errors.push("Order must contain at least one line.");
  }

  for (const line of order.lines) {
    if (line.quantity <= 0) {
      errors.push(`Line ${line.sku} must have a positive quantity.`);
    }
    if (line.unitPriceCents <= 0) {
      errors.push(`Line ${line.sku} must have a positive unit price.`);
    }
  }

  switch (order.channel) {
    case "web":
    case "store":
    case "partner":
      break;
    default:
      assertNever(order.channel);
  }

  return errors;
}

export function buildCheckoutSummary(order: OrderDraft): CheckoutSummary {
  const errors = validateOrder(order);
  if (errors.length > 0) {
    throw new Error(errors.join(" "));
  }

  const subtotalCents = calculateSubtotalCents(order);
  const discountCents = calculateDiscountCents(order);

  return {
    orderId: order.id,
    channelLabel: CHANNEL_LABELS[order.channel],
    subtotalCents,
    discountCents,
    payableCents: subtotalCents - discountCents,
  };
}

function assertNever(value: never): never {
  throw new Error(`Unhandled order channel: ${String(value)}`);
}
