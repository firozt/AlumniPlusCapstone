import DashboardNav from '../../components/DashboardNav'
import Footer from '../../components/Footer'

// Static visual shell only: no state, no API calls.
// Placeholder rows so the table has something to render; replace with real data later.
const placeholderOrders = [
  {
    id: 1000,
    symbol: 'ABC',
    side: 'BUY',
    qty: 100,
    price: '25.5000',
    status: 'SUBMITTED',
  },
  {
    id: 1001,
    symbol: 'EUB10',
    side: 'SELL',
    qty: 50,
    price: '98.2500',
    status: 'ACCEPTED',
  },
  {
    id: 1002,
    symbol: 'TCH',
    side: 'BUY',
    qty: 10,
    price: '310.0000',
    status: 'CANCELLED',
  },
]

// Tinted badges: soft background + matching text, so they don't fight the pink accent.
const statusBadge: Record<string, string> = {
  SUBMITTED: 'bg-white/10 text-ink',
  ACCEPTED: 'bg-emerald-400/10 text-emerald-300',
  CANCELLED: 'bg-rose-400/10 text-rose-300',
}

const TradeOrder = () => {
  return (
    <div className="flex flex-col min-h-svh bg-page text-ink">
      <DashboardNav />
      <div className="container mx-auto grid grid-cols-2 gap-10 my-10 flex-grow">
        {/* order list */}
        <div className="min-w-0 border border-white/10 rounded-[20px] p-2 overflow-auto bg-surface">
          <div className="flex items-center justify-between p-2">
            <div className="text-[24px]">Orders</div>
            <select className="rounded-lg px-3 py-2 bg-page border border-white/20 focus:outline-none focus:ring-2 focus:ring-accent">
              <option>All statuses</option>
              <option>SUBMITTED</option>
              <option>ACCEPTED</option>
              <option>CANCELLED</option>
            </select>
          </div>

          <div className="grid grid-cols-7 gap-3 p-2 text-sm text-white/60 border-b border-white/10">
            <div>ID</div>
            <div>Symbol</div>
            <div>Side</div>
            <div className="text-right">Qty</div>
            <div className="text-right">Limit</div>
            <div>Status</div>
            <div />
          </div>

          {placeholderOrders.map((o) => (
            <div
              key={o.id}
              className="grid grid-cols-7 gap-3 p-2 items-center rounded hover:bg-white/5"
            >
              <div className="text-white/60">{o.id}</div>
              <div>{o.symbol}</div>
              <div>{o.side}</div>
              <div className="text-right tabular-nums">{o.qty}</div>
              <div className="text-right tabular-nums">{o.price}</div>
              <div>
                <span
                  className={`text-xs px-2 py-1 rounded-full ${statusBadge[o.status]}`}
                >
                  {o.status}
                </span>
              </div>
              <div className="text-right">
                {o.status !== 'CANCELLED' && (
                  <button className="text-sm border border-white/20 rounded-lg px-3 py-1 hover:border-accent hover:text-accent duration-250">
                    Cancel
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* order entry + summary */}
        <div className="min-w-0 flex flex-col gap-10">
          <div className="p-5 rounded-[20px] bg-surface border border-white/10">
            <div className="text-[24px] mb-4">New Order</div>

            <form className="flex flex-col gap-4">
              <label className="flex flex-col gap-2">
                Instrument
                <select className="rounded-lg p-3 bg-page border border-white/20 focus:outline-none focus:ring-2 focus:ring-accent">
                  <option>ABC · Alpha Bank Corp</option>
                </select>
              </label>

              <div className="flex flex-col gap-2">
                Side
                <div className="flex gap-3">
                  {/* selected state */}
                  <button
                    type="button"
                    className="flex-1 rounded-lg p-3 bg-accent text-page font-medium"
                  >
                    Buy
                  </button>
                  {/* unselected state */}
                  <button
                    type="button"
                    className="flex-1 rounded-lg p-3 border border-white/20 hover:border-accent hover:text-accent duration-250"
                  >
                    Sell
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <label className="flex flex-col gap-2">
                  Quantity
                  <input
                    type="number"
                    placeholder="0"
                    className="rounded-lg p-3 bg-page border border-white/20 placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-accent"
                  />
                </label>
                <label className="flex flex-col gap-2">
                  Limit price
                  <input
                    type="number"
                    placeholder="0.0000"
                    className="rounded-lg p-3 bg-page border border-white/20 placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-accent"
                  />
                </label>
              </div>

              <div className="text-sm text-white/60">Est. value: —</div>

              <button
                type="submit"
                className="bg-accent text-page font-medium rounded-lg p-3 hover:bg-accent/90 duration-250"
              >
                Submit Order
              </button>

              {/* message slot (success / error) */}
              <div className="text-sm text-rose-300">
                Error message goes here
              </div>
            </form>
          </div>

          {/* summary: two values, per the spec */}
          <div className="grid grid-cols-2 gap-10">
            <div className="p-5 rounded-[20px] bg-surface border border-white/10">
              <div className="text-sm text-white/60">Orders</div>
              <div className="text-[28px] tabular-nums">—</div>
            </div>
            <div className="p-5 rounded-[20px] bg-surface border border-white/10">
              <div className="text-sm text-white/60">Total requested value</div>
              <div className="text-[28px] tabular-nums">—</div>
            </div>
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default TradeOrder
