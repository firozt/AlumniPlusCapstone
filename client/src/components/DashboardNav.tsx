import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const DashboardNav = () => {
  const { user, logout } = useAuth()

  return (
    <div>
      <div className="flex justify-between p-5 bg-page text-ink border-b border-white/10">
        {/* left side */}
        <div className="flex gap-5">
          <div className="p-2 font-medium">
            <Link to="/TradeOrder">Tradr</Link>
          </div>
          {/* <div className="flex">
            <NavLink to="/TradeOrder" className={navLinkClass}>
              Orders
            </NavLink>
            <NavLink to="/Activity" className={navLinkClass}>
              Activity
            </NavLink>
          </div> */}
        </div>
        {/* right side */}
        <div className="flex gap-5 items-center">
          {/* who's signed in, and as which role */}
          <span className="text-sm text-white/60">
            {user?.username} · {user?.role}
          </span>
          {/* a button, not a Link: it has to end the session, not just change pages */}
          <button
            onClick={() => logout()}
            className="border border-white/20 rounded p-2 hover:border-accent hover:text-accent duration-250"
          >
            Logout
          </button>
        </div>
      </div>
    </div>
  )
}

export default DashboardNav
