export type OrderChannel = "web" | "store" | "partner";

export type CustomerTier = "standard" | "gold" | "enterprise";

export interface OrderLine {
  sku: string;
  quantity: number;
  unitPriceCents: number;
}

export interface OrderDraft {
  id: string;
  channel: OrderChannel;
  customerTier: CustomerTier;
  lines: OrderLine[];
  couponCode?: string;
}

export interface CheckoutSummary {
  orderId: string;
  channelLabel: string;
  subtotalCents: number;
  discountCents: number;
  payableCents: number;
}

export const CHANNEL_LABELS: Record<OrderChannel, string> = {
  web: "Web checkout",
  store: "In-store counter",
  partner: "Partner marketplace",
};
