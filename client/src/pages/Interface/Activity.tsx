import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

type Activity = {
  eventId: string
  orderId: number
  eventType: 'SUBMITTED' | 'ACCEPTED' | 'CANCELLED'
  owner: string
  occurredAt: string // UTC LocalDateTime from the backend, no zone marker
}

type Props = {
  token: string // same Keycloak access token you send to /api/orders
}

const badgeClass: Record<Activity['eventType'], string> = {
  SUBMITTED: 'badge badge-submitted',
  ACCEPTED: 'badge badge-accepted',
  CANCELLED: 'badge badge-cancelled',
}

function formatTime(occurredAt: string): string {
  // Backend sends UTC without "Z"; add it so the browser converts to local time.
  const d = new Date(occurredAt.endsWith('Z') ? occurredAt : occurredAt + 'Z')
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function actorFor(e: Activity): string {
  // Only the simulator accepts orders; the owner submits and cancels.
  return e.eventType === 'ACCEPTED' ? 'SIMULATOR' : e.owner
}

export default function ActivityPage({ token }: Props) {
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId')

  const [events, setEvents] = useState<Activity[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!orderId) {
      setEvents([])
      setError('No order selected.')
      return
    }

    const controller = new AbortController()
    setLoading(true)
    setError(null)

    fetch(`/api/activity/${encodeURIComponent(orderId)}`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: controller.signal,
    })
      .then(async (r) => {
        if (r.status === 403)
          throw new Error('You are not authorized to view this order.')
        if (!r.ok)
          throw new Error(`Failed to load activity (HTTP ${r.status}).`)
        return (await r.json()) as Activity[]
      })
      .then((data) => setEvents(data))
      .catch((err: unknown) => {
        if (err instanceof DOMException && err.name === 'AbortError') return
        setEvents([])
        setError(
          err instanceof Error ? err.message : 'Failed to load activity.',
        )
      })
      .finally(() => setLoading(false))

    return () => controller.abort()
  }, [orderId, token])

  return (
    <div className="activity-card">
      <h1>Order Activity{orderId ? ` — #${orderId}` : ''}</h1>

      {loading && <p>Loading…</p>}
      {error && <p className="error">{error}</p>}

      {!loading && !error && (
        <table className="activity-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Order ID</th>
              <th>Event</th>
              <th>Actor</th>
            </tr>
          </thead>
          <tbody>
            {events.length === 0 ? (
              <tr>
                <td colSpan={4}>No activity recorded for this order yet.</td>
              </tr>
            ) : (
              events.map((e) => (
                <tr key={e.eventId}>
                  <td>{formatTime(e.occurredAt)}</td>
                  <td>{e.orderId}</td>
                  <td>
                    <span className={badgeClass[e.eventType]}>
                      {e.eventType}
                    </span>
                  </td>
                  <td
                    className={
                      e.eventType === 'ACCEPTED' ? 'actor-system' : undefined
                    }
                  >
                    {actorFor(e)}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}
