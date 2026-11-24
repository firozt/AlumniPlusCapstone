import HomeNav from '../../components/HomeNav'
import Footer from '../../components/Footer'
// import MultiDevice from '../../assets/Multi_Device.png'
import { useState } from 'react'
// import axios from 'axios'
import { Link, useNavigate } from 'react-router-dom'
// import { saveUser } from '../../lib/session'

const Login = () => {
  let [userName, setUserName] = useState('')
  let [error, setError] = useState('')
  const navigate = useNavigate()

  return (
    <div className="flex flex-col min-h-svh bg-surface text-ink">
      <HomeNav />
      <div className="flex-grow flex flex-col">
        <div className="container mx-auto flex flex-grow">
          {/* image */}
          <div className="flex-1 flex-col justify-center p-10 bg-accent text-page flex-grow">
            <div className="text-[32px] mb-5 pt-5 mt-5">
              Unlock the Power of Investing
            </div>
            <ul className="list-disc pl-5 space-y-1 pt-3 text-page/80 marker:text-page">
              <li>
                Practice trading stocks, cryptos and options with virtual money
              </li>
              <li>Gain confidence before risking your own money</li>
              <li>Learn how the market works in a safe space with no risk</li>
            </ul>
            {/* <div className="mt-10 pl-10">
              <img src={MultiDevice} className="h-auto w-100" alt="" />
            </div> */}
          </div>
          {/* content */}
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
                />
              </label>

              {error && (
                <div className="mb-4 text-red-400 text-sm">{error}</div>
              )}

              <label className="flex flex-col">
                <button
                  type="submit"
                  className="bg-accent text-page font-medium rounded-lg p-3 mb-5 hover:bg-accent/90 duration-250"
                >
                  Sign In
                </button>
              </label>
            </form>
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Login
