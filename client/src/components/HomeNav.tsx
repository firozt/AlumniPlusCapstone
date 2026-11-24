import { Link } from 'react-router-dom'

const HomeNav = () => {
  return (
    <div>
      <div className="flex justify-between p-5 bg-page text-ink border-b border-white/10">
        {/* left side */}
        <div className="flex gap-5">
          <Link to="/">
            <div className="p-2 font-medium">Tradr</div>
          </Link>
          {/* <div className="flex">
            <Link
              to="/Dashboard"
              className="p-2 text-gray-300 hover:text-white"
            >
              To Dashboard Page
            </Link>
            <Link
              to="/BuyStocks"
              className="p-2 text-gray-300 hover:text-white"
            >
              To Buy Stocks Page
            </Link>
          </div> */}
        </div>
        {/* right side */}
        <div className="flex gap-5">
          <Link
            to="/Register"
            className="rounded-lg bg-accent text-page font-medium px-4 py-2 hover:bg-accent/90 transition-colors"
          >
            Get Started
          </Link>
          <Link
            to="/Login"
            className="rounded-lg border border-white/20 px-4 py-2 hover:border-accent hover:text-accent transition-colors"
          >
            Login
          </Link>
        </div>
      </div>
    </div>
  )
}

export default HomeNav
