import HomeNav from '../../components/HomeNav'
import Footer from '../../components/Footer'
import { useState } from 'react'
import { Link } from 'react-router-dom'

const Register = () => {
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
            <div className="text-[32px] mb-5 pt-5 mt-5">Sign up for free</div>
            <ul className="list-disc pl-5 space-y-1 pt-3 text-page/80 marker:text-page">
              <li>
                Place simulated buy and sell limit orders on five sample
                instruments
              </li>
              <li>
                Follow each order from Submitted to Accepted, or cancel it while
                it's still open
              </li>
              <li>
                Review a timestamped history of every submission, acceptance and
                cancellation
              </li>
              <li>
                No real money and no real trades: all prices and data are
                simulated
              </li>
            </ul>
          </div>
          {/* form panel */}
          <div className="flex-1 p-10 flex-grow bg-surface">
            <div className="text-[32px] mb-5 pt-5 mt-5">Register</div>
            <div className="text-[24px]">Create an Account</div>
            <div className="flex gap-1">
              <div className="text-white/60">Already have an account?</div>
              <Link
                to="/Login"
                className="font-bold text-accent underline hover:no-underline duration-250 mb-10"
              >
                log in here
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
                  autoComplete="new-password"
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
                Sign Up
              </button>
            </form>
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Register
