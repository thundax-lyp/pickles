import type { CustomerTier, OrderChannel, OrderDraft } from "./orderTypes.js";

type DiscountMatrix = Record<OrderChannel, Record<CustomerTier, number>>;

const DISCOUNT_MATRIX: DiscountMatrix = {
  web: {
    standard: 0,
    gold: 0.08,
    enterprise: 0.12,
  },
  store: {
    standard: 0,
    gold: 0.05,
    enterprise: 0.1,
  },
  partner: {
    standard: 0,
    gold: 0.03,
    enterprise: 0.07,
  },
};

export function calculateSubtotalCents(order: OrderDraft): number {
  return order.lines.reduce((total, line) => {
    return total + line.quantity * line.unitPriceCents;
  }, 0);
}

export function calculateDiscountCents(order: OrderDraft): number {
  const subtotal = calculateSubtotalCents(order);
  const discountRate = DISCOUNT_MATRIX[order.channel][order.customerTier];
  const couponBonus = order.couponCode === "PICKLES10" ? 0.1 : 0;
  return Math.floor(subtotal * Math.min(discountRate + couponBonus, 0.3));
}
