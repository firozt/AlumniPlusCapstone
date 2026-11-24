import { Link } from 'react-router-dom'

const Home = () => {
  return (
    <div>
      <div>
        <div>Home</div>
        <div>test</div>
        <div className="flex-col">
          <div>
            <Link to="/TradeOrder">TradeOrder</Link>
          </div>
          <div>
            <Link to="/Login">Login</Link>
          </div>
          <div>
            <Link to="/Activity">Activity</Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Home
