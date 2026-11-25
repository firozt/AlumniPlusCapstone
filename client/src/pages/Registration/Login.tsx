import HomeNav from '../../components/HomeNav'
import Footer from '../../components/Footer'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { AUTH_MODE } from '../../auth/oidcConfig'

const Login = () => {
  const { user, isLoading, error, signIn } = useAuth()

  // Already signed in: skip the login page.
  if (user) return <Navigate to="/TradeOrder" replace />

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
          {/* sign-in panel */}
          <div className="flex-1 bg-surface p-10 flex-grow">
            <div className="text-[32px] mb-5 pt-5 mt-5">Login</div>
            <div className="text-[24px]">Welcome Back</div>

            {AUTH_MODE === 'oidc' ? (
              <>
                <div className="text-white/60 mb-10">
                  You'll be taken to the secure sign-in page, then brought back
                  here.
                </div>
                <button
                  onClick={() => signIn()}
                  disabled={isLoading}
                  className="w-full bg-accent text-page font-medium rounded-lg p-3 mb-5 hover:bg-accent/90 duration-250 disabled:opacity-50"
                >
                  Sign In
                </button>
              </>
            ) : (
              <>
                <div className="text-white/60 mb-10">
                  Mock mode: pick a test user. No provider is connected yet.
                </div>
                <div className="flex flex-col gap-3">
                  <button
                    onClick={() => signIn('team-1-trader1')}
                    className="w-full bg-accent text-page font-medium rounded-lg p-3 hover:bg-accent/90 duration-250"
                  >
                    Sign in as team-1-trader1 (Trader)
                  </button>
                  <button
                    onClick={() => signIn('team-1-trader2')}
                    className="w-full border border-white/20 rounded-lg p-3 hover:border-accent hover:text-accent duration-250"
                  >
                    Sign in as team-1-trader2 (Trader)
                  </button>
                  <button
                    onClick={() => signIn('team-1-ops1')}
                    className="w-full border border-white/20 rounded-lg p-3 hover:border-accent hover:text-accent duration-250"
                  >
                    Sign in as team-1-ops1 (Operations)
                  </button>
                </div>
              </>
            )}

            {error && <div className="mt-4 text-rose-300 text-sm">{error}</div>}
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}

export default Login
