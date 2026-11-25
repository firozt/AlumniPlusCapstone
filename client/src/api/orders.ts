// Order Service calls. Uses the shared axios instance from lib/api.ts,
// which already attaches the Bearer token and handles 401 -> /Login.
import axios from 'axios'
import api from '../lib/api'

export type Side = 'BUY' | 'SELL'
export type OrderStatus = 'SUBMITTED' | 'ACCEPTED' | 'CANCELLED'

// Mirrors OrderResponse.java
export interface Order {
  orderId: number
  owner: string
  instrumentId: string
  side: Side
  quantity: number
  limitPrice: number // BigDecimal arrives as a JSON number (25.5000 -> 25.5); format with toFixed(4)
  status: OrderStatus
  createdAt: string // Instant -> ISO-8601 string
  updatedAt: string
  version: number
}

// Mirrors OrderSummaryResponse.java
export interface OrderSummary {
  orderCount: number
  totalRequestedValue: number
}

// Mirrors InstrumentResponse.java
export interface Instrument {
  instrumentId: string
  name: string
  currency: string
  active: boolean
}

// Mirrors CreateOrderRequest.java
export interface CreateOrderRequest {
  instrumentId: string
  side: Side
  quantity: number
  limitPrice: number
}

export const ordersApi = {
  mine: () => api.get<Order[]>('/api/orders/view').then((r) => r.data),
  all: () => api.get<Order[]>('/api/orders/view-all').then((r) => r.data),
  summary: () =>
    api.get<OrderSummary>('/api/orders/summary').then((r) => r.data),
  submit: (body: CreateOrderRequest) =>
    api.post<Order>('/api/orders/submit', body).then((r) => r.data),
  cancel: (orderId: number) =>
    api.post<Order>(`/api/orders/cancel/${orderId}`).then((r) => r.data),
  // Needs InstrumentController.java on the backend.
  instruments: () =>
    api.get<Instrument[]>('/api/instruments').then((r) => r.data),
}

// HTTP status of a failed call, or undefined if the server was never reached.
export function httpStatus(e: unknown): number | undefined {
  return axios.isAxiosError(e) ? e.response?.status : undefined
}

// Turns any failure into a message a user can read.
export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const status = e.response?.status
    if (status === undefined) return 'Could not reach the Order Service.'
    if (status === 403) return "You don't have permission to do that."

    // Spring Boot's error body: "message" (needs server.error.include-message=always),
    // "errors" for validation (needs server.error.include-binding-errors=always).
    const body = e.response?.data as
      | {
          message?: string
          detail?: string
          errors?: { defaultMessage?: string }[]
        }
      | undefined
    const fieldErrors =
      body?.errors?.map((x) => x.defaultMessage).filter(Boolean) ?? []
    if (fieldErrors.length) return fieldErrors.join('; ')
    return body?.detail || body?.message || `Request failed (${status}).`
  }
  return e instanceof Error ? e.message : 'Something went wrong.'
}
