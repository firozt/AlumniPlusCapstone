import DashboardNav from '../../components/DashboardNav'
import Footer from '../../components/Footer'

// Static visual shell only: no state, no API calls.
// Placeholder events so the table has something to render; replace with real data later.
const placeholderEvents = [
  {
    eventId: 'a1',
    orderId: 1000,
    eventType: 'SUBMITTED',
    eventTime: '2026-11-22 09:30:00',
    actor: 'trader1',
  },
  {
    eventId: 'a2',
    orderId: 1000,
    eventType: 'ACCEPTED',
    eventTime: '2026-11-22 09:30:10',
    actor: 'SIMULATOR',
  },
  {
    eventId: 'a3',
    orderId: 1002,
    eventType: 'SUBMITTED',
    eventTime: '2026-11-22 09:31:05',
    actor: 'trader1',
  },
  {
    eventId: 'a4',
    orderId: 1002,
    eventType: 'CANCELLED',
    eventTime: '2026-11-22 09:31:09',
    actor: 'trader1',
  },
]

// Same tinted badges as TradeOrder so statuses look identical across screens.
const eventBadge: Record<string, string> = {
  SUBMITTED: 'bg-white/10 text-ink',
  ACCEPTED: 'bg-emerald-400/10 text-emerald-300',
  CANCELLED: 'bg-rose-400/10 text-rose-300',
}

const Activity = () => {
  return (
    <div className="flex flex-col min-h-svh bg-page text-ink">
      <DashboardNav />
      <div className="container mx-auto flex flex-col flex-grow my-10">
        <div className="flex-grow flex flex-col border border-white/10 rounded-[20px] bg-surface p-5">
          <div className="text-[32px] pb-2 mb-2 border-b border-white/10">
            Order Activity
          </div>

          {/* Loading state (swap in while fetching):
          <div className="p-4 text-white/60">Loading…</div>
          */}

          {/* Empty state (swap in when there are no events):
          <div className="p-4 text-white/60">No activity yet.</div>
          */}

          <div>
            <div className="grid grid-cols-4 gap-6 p-2 pl-6 text-sm text-white/60 border-b border-white/10">
              <div>Time</div>
              <div>Order ID</div>
              <div>Event</div>
              <div>Actor</div>
            </div>
            {placeholderEvents.map((e) => (
              <div
                key={e.eventId}
                className="grid grid-cols-4 gap-6 p-2 pl-6 items-center rounded hover:bg-white/5"
              >
                <div className="tabular-nums text-white/80">{e.eventTime}</div>
                <div className="tabular-nums">{e.orderId}</div>
                <div>
                  <span
                    className={`text-xs px-2 py-1 rounded-full ${eventBadge[e.eventType]}`}
                  >
                    {e.eventType}
                  </span>
                </div>
                {/* SIMULATOR is muted so automatic events read differently from user actions */}
                <div
                  className={
                    e.actor === 'SIMULATOR' ? 'text-white/50 italic' : ''
                  }
                >
                  {e.actor}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Activity
