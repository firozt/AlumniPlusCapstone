import HomeNav from '../../components/HomeNav'
import Footer from '../../components/Footer'
import { useState } from 'react'
import { Link } from 'react-router-dom'

const Login = () => {
  const [userName, setUserName] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error] = useState('')

  return (
    <div className="flex flex-col min-h-svh bg-surface text-ink">
      <HomeNav />
      <div className="flex-grow flex flex-col">
        <div className="container mx-auto flex flex-grow">
          {/* info panel */}
          <div className="flex-1 flex-col justify-center p-10 bg-accent text-page flex-grow">
            <div className="text-[32px] mb-5 pt-5 mt-5">
              Your orders, start to finish
            </div>
            <ul className="list-disc pl-5 space-y-1 pt-3 text-page/80 marker:text-page">
              <li>
                See where each of your orders stands: Submitted, Accepted or
                Cancelled
              </li>
              <li>Cancel any order that's still open</li>
              <li>Review the full timestamped activity history</li>
              <li>
                Traders manage their own orders; Operations can view every order
              </li>
            </ul>
          </div>
          {/* form panel */}
          <div className="flex-1 bg-surface p-10 flex-grow">
            <div className="text-[32px] mb-5 pt-5 mt-5">Login</div>
            <div className="text-[24px]">Welcome Back</div>
            <div className="flex gap-1">
              <div className="text-white/60">Don't have an account?</div>
              <Link
                to="/Register"
                className="font-bold text-accent underline hover:no-underline duration-250 mb-10"
              >
                sign up here
              </Link>
            </div>
            <form className="flex flex-col">
              <label className="flex flex-col gap-2">
                Username
                <input
                  className="rounded-lg p-3 mb-5 bg-page border border-white/20 placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-accent"
                  value={userName}
                  onChange={(e) => setUserName(e.target.value)}
                  placeholder="Enter username"
                  type="text"
                  autoComplete="username"
                />
              </label>

              <label className="flex flex-col gap-2">
                Password
                <input
                  className="rounded-lg p-3 mb-3 bg-page border border-white/20 placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-accent"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                />
              </label>

              <label className="flex items-center gap-3 mb-5 text-sm text-white/60 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={showPassword}
                  onChange={(e) => setShowPassword(e.target.checked)}
                  className="size-4 accent-accent"
                />
                Show password
              </label>

              {error && (
                <div className="mb-4 text-rose-300 text-sm">{error}</div>
              )}

              <button
                type="submit"
                className="bg-accent text-page font-medium rounded-lg p-3 mb-5 hover:bg-accent/90 duration-250"
              >
                Sign In
              </button>
            </form>
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Login
