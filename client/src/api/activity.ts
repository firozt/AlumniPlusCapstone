import api from '../lib/api'

// Matches OrderActivity returned by Activity Service.
export interface OrderActivity {
  eventId: string
  orderId: number
  eventType: string
  owner: string
  occurredAt: string
  receivedAt: string
}

export const activityApi = {
  history: (orderId: string, signal?: AbortSignal) =>
    api
      .get<OrderActivity[]>(`/api/activity/${orderId}`, { signal })
      .then((response) => response.data),
}
