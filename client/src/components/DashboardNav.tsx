import { Link, NavLink } from 'react-router-dom'

// Active link gets the accent; inactive links are muted and brighten on hover.
const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  isActive
    ? 'p-2 rounded text-accent'
    : 'p-2 rounded text-white/60 hover:text-ink duration-250'

const DashboardNav = () => {
  return (
    <div>
      <div className="flex justify-between p-5 bg-page text-ink border-b border-white/10">
        {/* left side */}
        <div className="flex gap-5">
          <div className="p-2 font-medium">
            <Link to="/">StockSim</Link>
          </div>
          <div className="flex">
            <NavLink to="/Activity" className={navLinkClass}>
              Activity
            </NavLink>
            <NavLink to="/TradeOrder" className={navLinkClass}>
              TradeOrder
            </NavLink>
          </div>
        </div>
        {/* right side */}
        <div className="flex gap-5">
          {/* <div className="p-2">Welcome Dennis</div> */}
          <Link
            to="/"
            className="border border-white/20 rounded p-2 hover:border-accent hover:text-accent duration-250"
          >
            Logout
          </Link>
        </div>
      </div>
    </div>
  )
}

export default DashboardNav
