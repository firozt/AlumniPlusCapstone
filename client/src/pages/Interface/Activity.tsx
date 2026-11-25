import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import DashboardNav from '../../components/DashboardNav'
import Footer from '../../components/Footer'
import { activityApi, type OrderActivity } from '../../api/activity'
import { errorMessage } from '../../api/orders'

// Same tinted badges as TradeOrder so statuses look identical across screens.
const eventBadge: Record<string, string> = {
  SUBMITTED: 'bg-white/10 text-ink',
  ACCEPTED: 'bg-emerald-400/10 text-emerald-300',
  CANCELLED: 'bg-rose-400/10 text-rose-300',
}

const Activity = () => {
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId') ?? ''
  const validOrderId =
    /^\d+$/.test(orderId) &&
    Number.isSafeInteger(Number(orderId)) &&
    Number(orderId) > 0
  const [events, setEvents] = useState<OrderActivity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!validOrderId) return
    const controller = new AbortController()

    async function loadHistory() {
      setLoading(true)
      setError(null)
      setEvents([])
      try {
        const history = await activityApi.history(orderId, controller.signal)
        if (!controller.signal.aborted) setEvents(history)
      } catch (err) {
        if (controller.signal.aborted) return
        setError(
          axios.isAxiosError(err) && !err.response
            ? 'Could not reach the Activity Service.'
            : errorMessage(err),
        )
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadHistory()
    return () => controller.abort()
  }, [orderId, validOrderId])

  return (
    <div className="flex flex-col min-h-svh bg-page text-ink">
      <DashboardNav />
      <div className="container mx-auto flex flex-col flex-grow my-10">
        <div className="flex-grow flex flex-col border border-white/10 rounded-[20px] bg-surface p-5">
          <div className="text-[32px] pb-2 mb-2 border-b border-white/10">
            Order Activity{validOrderId && ` · ${orderId}`}
          </div>

          <Link to="/TradeOrder" className="mb-4 text-sm text-accent">
            Back to orders
          </Link>

          {!validOrderId && (
            <div className="p-4 text-white/60">
              Select an order from the Orders page to view its activity.
            </div>
          )}

          {validOrderId && loading && (
            <div role="status" className="p-4 text-white/60">
              Loading…
            </div>
          )}

          {validOrderId && error && (
            <div role="alert" className="p-4 text-rose-300">
              {error}
            </div>
          )}

          {validOrderId && !loading && !error && events.length === 0 && (
            <div className="p-4 text-white/60">No activity yet.</div>
          )}

          {validOrderId && !loading && !error && events.length > 0 && (
            <div>
              <div className="grid grid-cols-4 gap-6 p-2 pl-6 text-sm text-white/60 border-b border-white/10">
                <div>Time</div>
                <div>Order ID</div>
                <div>Event</div>
                <div>Owner</div>
              </div>
              {events.map((e) => (
                <div
                  key={e.eventId}
                  className="grid grid-cols-4 gap-6 p-2 pl-6 items-center rounded hover:bg-white/5"
                >
                  <div className="tabular-nums text-white/80">
                    {new Date(e.occurredAt).toLocaleString()}
                  </div>
                  <div className="tabular-nums">{e.orderId}</div>
                  <div>
                    <span
                      className={`text-xs px-2 py-1 rounded-full ${eventBadge[e.eventType] ?? 'bg-white/10 text-ink'}`}
                    >
                      {e.eventType}
                    </span>
                  </div>
                  <div>{e.owner}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Activity
