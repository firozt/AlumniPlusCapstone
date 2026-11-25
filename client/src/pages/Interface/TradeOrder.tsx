import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import DashboardNav from '../../components/DashboardNav'
import Footer from '../../components/Footer'
import { useAuth } from '../../auth/AuthContext'
import {
  errorMessage,
  httpStatus,
  ordersApi,
  type CreateOrderRequest,
  type Instrument,
  type Order,
  type OrderStatus,
  type OrderSummary,
  type Side,
} from '../../api/orders'

// The simulator flips SUBMITTED -> ACCEPTED after a delay, so the list polls.
const POLL_MS = 3000

const statusBadge: Record<OrderStatus, string> = {
  SUBMITTED: 'bg-white/10 text-ink',
  ACCEPTED: 'bg-emerald-400/10 text-emerald-300',
  CANCELLED: 'bg-rose-400/10 text-rose-300',
}

const inputClass =
  'rounded-lg p-3 bg-page border border-white/20 placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-accent'

const fmtPrice = (n: number) => n.toFixed(4)
const fmtMoney = (n: number) =>
  n.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })

// ---------------------------------------------------------------------------
// Order list. Traders: an action column with Cancel. Operations: an Owner column instead.
// ---------------------------------------------------------------------------
function OrderList({
  isOps,
  orders,
  loading,
  error,
  cancellingId,
  onCancel,
}: {
  isOps: boolean
  orders: Order[]
  loading: boolean
  error: string | null
  cancellingId: number | null
  onCancel: (orderId: number) => void
}) {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<'ALL' | OrderStatus>('ALL')
  const visible =
    filter === 'ALL' ? orders : orders.filter((o) => o.status === filter)

  return (
    <div className="min-w-0 border border-white/10 rounded-[20px] p-2 overflow-auto bg-surface">
      <div className="flex items-center justify-between p-2">
        <div className="text-[24px]">{isOps ? 'All Orders' : 'My Orders'}</div>
        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value as 'ALL' | OrderStatus)}
          className="rounded-lg px-3 py-2 bg-page border border-white/20 focus:outline-none focus:ring-2 focus:ring-accent"
        >
          <option value="ALL">All statuses</option>
          <option value="SUBMITTED">SUBMITTED</option>
          <option value="ACCEPTED">ACCEPTED</option>
          <option value="CANCELLED">CANCELLED</option>
        </select>
      </div>

      {error && <div className="p-2 text-sm text-rose-300">{error}</div>}

      <div className="grid grid-cols-7 gap-3 p-2 text-sm text-white/60 border-b border-white/10">
        <div>ID</div>
        {isOps && <div>Owner</div>}
        <div>Symbol</div>
        <div>Side</div>
        <div className="text-right">Qty</div>
        <div className="text-right">Limit</div>
        <div>Status</div>
        {!isOps && <div />}
      </div>

      {loading && <div className="p-4 text-white/60">Loading…</div>}
      {!loading && visible.length === 0 && (
        <div className="p-4 text-white/60">
          {orders.length === 0
            ? 'No orders yet.'
            : 'No orders with this status.'}
        </div>
      )}

      {visible.map((o) => (
        <div
          key={o.orderId}
          onClick={() => navigate(`/Activity?orderId=${o.orderId}`)}
          className="grid grid-cols-7 gap-3 p-2 items-center rounded hover:bg-white/5 cursor-pointer"
        >
          <div className="text-white/60 tabular-nums">{o.orderId}</div>
          {isOps && <div className="truncate">{o.owner}</div>}
          <div>{o.instrumentId}</div>
          <div>{o.side}</div>
          <div className="text-right tabular-nums">{o.quantity}</div>
          <div className="text-right tabular-nums">
            {fmtPrice(o.limitPrice)}
          </div>
          <div>
            <span
              className={`text-xs px-2 py-1 rounded-full ${statusBadge[o.status]}`}
            >
              {o.status}
            </span>
          </div>
          {!isOps && (
            <div className="text-right">
              {o.status !== 'CANCELLED' && (
                <button
                  disabled={cancellingId === o.orderId}
                  onClick={(e) => {
                    e.stopPropagation() // don't also open the activity view
                    onCancel(o.orderId)
                  }}
                  className="text-sm border border-white/20 rounded-lg px-3 py-1 hover:border-accent hover:text-accent duration-250 disabled:opacity-50"
                >
                  {cancellingId === o.orderId ? 'Cancelling…' : 'Cancel'}
                </button>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

// ---------------------------------------------------------------------------
// New order form. Traders only.
// ---------------------------------------------------------------------------
function NewOrderForm({
  instruments,
  onSubmit,
}: {
  instruments: Instrument[]
  onSubmit: (req: CreateOrderRequest) => Promise<string>
}) {
  const [instrumentId, setInstrumentId] = useState('')
  const [side, setSide] = useState<Side>('BUY')
  const [quantity, setQuantity] = useState('')
  const [price, setPrice] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(
    null,
  )

  // Default to the first instrument once they load.
  useEffect(() => {
    if (!instrumentId && instruments.length > 0)
      setInstrumentId(instruments[0].instrumentId)
  }, [instruments, instrumentId])

  const qty = Number(quantity)
  const px = Number(price)
  const estValue = qty > 0 && px > 0 ? fmtMoney(qty * px) : '—'

  // Mirrors CreateOrderRequest's annotations so users get instant feedback.
  // The backend still validates; this is only for UX.
  function validate(): string | null {
    if (!instrumentId) return 'Choose an instrument.'
    if (!Number.isInteger(qty) || qty <= 0)
      return 'Quantity must be a whole number greater than 0.'
    if (!(px > 0)) return 'Limit price must be greater than 0.'
    if (!/^\d{1,8}(\.\d{1,4})?$/.test(price))
      return 'Limit price allows up to 8 whole digits and 4 decimals.'
    return null
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const problem = validate()
    if (problem) {
      setMessage({ ok: false, text: problem })
      return
    }
    setSubmitting(true)
    setMessage(null)
    try {
      const text = await onSubmit({
        instrumentId,
        side,
        quantity: qty,
        limitPrice: px,
      })
      setMessage({ ok: true, text })
      setQuantity('')
      setPrice('')
    } catch (err) {
      setMessage({ ok: false, text: errorMessage(err) })
    } finally {
      setSubmitting(false)
    }
  }

  const sideClass = (s: Side) =>
    s === side
      ? 'flex-1 rounded-lg p-3 bg-accent text-page font-medium'
      : 'flex-1 rounded-lg p-3 border border-white/20 hover:border-accent hover:text-accent duration-250'

  return (
    <div className="p-5 rounded-[20px] bg-surface border border-white/10">
      <div className="text-[24px] mb-4">New Order</div>

      <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
        <label className="flex flex-col gap-2">
          Instrument
          <select
            className={inputClass}
            value={instrumentId}
            onChange={(e) => setInstrumentId(e.target.value)}
          >
            {instruments.length === 0 && (
              <option value="">Loading instruments…</option>
            )}
            {instruments.map((i) => (
              <option key={i.instrumentId} value={i.instrumentId}>
                {i.instrumentId} · {i.name}
              </option>
            ))}
          </select>
        </label>

        <div className="flex flex-col gap-2">
          Side
          <div className="flex gap-3">
            <button
              type="button"
              className={sideClass('BUY')}
              onClick={() => setSide('BUY')}
            >
              Buy
            </button>
            <button
              type="button"
              className={sideClass('SELL')}
              onClick={() => setSide('SELL')}
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
              min="1"
              step="1"
              placeholder="0"
              className={inputClass}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
            />
          </label>
          <label className="flex flex-col gap-2">
            Limit price
            <input
              type="number"
              min="0"
              step="0.0001"
              placeholder="0.0000"
              className={inputClass}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </label>
        </div>

        <div className="text-sm text-white/60">Est. value: {estValue}</div>

        <button
          type="submit"
          disabled={submitting}
          className="bg-accent text-page font-medium rounded-lg p-3 hover:bg-accent/90 duration-250 disabled:opacity-50"
        >
          {submitting ? 'Submitting…' : 'Submit Order'}
        </button>

        {message && (
          <div
            className={`text-sm ${message.ok ? 'text-emerald-300' : 'text-rose-300'}`}
          >
            {message.text}
          </div>
        )}
      </form>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Summary: count and total of quantity × limit price, computed by the backend.
// ---------------------------------------------------------------------------
function Summary({
  isOps,
  summary,
}: {
  isOps: boolean
  summary: OrderSummary | null
}) {
  return (
    <div className="grid grid-cols-2 gap-10">
      <div className="p-5 rounded-[20px] bg-surface border border-white/10">
        <div className="text-sm text-white/60">
          {isOps ? 'Total orders' : 'My orders'}
        </div>
        <div className="text-[28px] tabular-nums">
          {summary ? summary.orderCount : '—'}
        </div>
      </div>
      <div className="p-5 rounded-[20px] bg-surface border border-white/10">
        <div className="text-sm text-white/60">
          {isOps ? 'Total requested value' : 'My requested value'}
        </div>
        <div className="text-[28px] tabular-nums">
          {summary ? fmtMoney(summary.totalRequestedValue) : '—'}
        </div>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------
const TradeOrder = () => {
  // The access token lives in tokenStore; lib/api.ts attaches it to every request.
  const { user } = useAuth()
  const isOps = user?.role === 'OPERATIONS'
  // Depend on the username string, not the user object: AuthContext rebuilds
  // the object on every render, which would restart polling constantly.
  const username = user?.username

  const [orders, setOrders] = useState<Order[]>([])
  const [summary, setSummary] = useState<OrderSummary | null>(null)
  const [instruments, setInstruments] = useState<Instrument[]>([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState<string | null>(null)
  const [cancellingId, setCancellingId] = useState<number | null>(null)

  const refresh = useCallback(async () => {
    if (!username) return
    try {
      const [o, s] = await Promise.all([
        isOps ? ordersApi.all() : ordersApi.mine(),
        ordersApi.summary(),
      ])
      setOrders(o)
      setSummary(s)
      setListError(null)
    } catch (e) {
      setListError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [isOps, username])

  // Initial load + polling.
  useEffect(() => {
    refresh()
    const id = setInterval(refresh, POLL_MS)
    return () => clearInterval(id)
  }, [refresh])

  // Instruments only matter to traders (the form).
  useEffect(() => {
    if (!username || isOps) return
    ordersApi
      .instruments()
      .then(setInstruments)
      .catch(() =>
        // Fallback until /api/instruments is deployed on the server
        setInstruments([
          {
            instrumentId: 'AAPL',
            name: 'Apple Inc.',
            currency: 'USD',
            active: true,
          },
          {
            instrumentId: 'MSFT',
            name: 'Microsoft Corp.',
            currency: 'USD',
            active: true,
          },
          {
            instrumentId: 'VOD.L',
            name: 'Vodafone Group',
            currency: 'GBP',
            active: true,
          },
        ]),
      )
  }, [isOps, username])

  async function handleCancel(orderId: number) {
    setCancellingId(orderId)
    try {
      await ordersApi.cancel(orderId)
    } catch (e) {
      setListError(errorMessage(e))
    } finally {
      setCancellingId(null)
      refresh()
    }
  }

  async function handleSubmit(req: CreateOrderRequest): Promise<string> {
    try {
      const created = await ordersApi.submit(req)
      return `Order ${created.orderId} submitted.`
    } catch (e) {
      // 503 = saved in Oracle but the Kafka publish wasn't confirmed.
      if (httpStatus(e) === 503) {
        throw new Error(
          'Order was saved, but its event was not published. Tell the team.',
        )
      }
      throw e
    } finally {
      refresh()
    }
  }

  return (
    <div className="flex flex-col min-h-svh bg-page text-ink">
      <DashboardNav />

      {isOps ? (
        // Operations: read-only. Summary on top, table full width.
        <div className="container mx-auto flex flex-col gap-10 my-10 flex-grow">
          <Summary isOps summary={summary} />
          <OrderList
            isOps
            orders={orders}
            loading={loading}
            error={listError}
            cancellingId={null}
            onCancel={() => {}}
          />
        </div>
      ) : (
        // Trader: table on the left, form + summary on the right.
        <div className="container mx-auto grid grid-cols-2 gap-10 my-10 flex-grow">
          <OrderList
            isOps={false}
            orders={orders}
            loading={loading}
            error={listError}
            cancellingId={cancellingId}
            onCancel={handleCancel}
          />
          <div className="min-w-0 flex flex-col gap-10">
            <NewOrderForm instruments={instruments} onSubmit={handleSubmit} />
            <Summary isOps={false} summary={summary} />
          </div>
        </div>
      )}

      <Footer />
    </div>
  )
}

export default TradeOrder
