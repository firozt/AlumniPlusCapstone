import React from 'react'
import { Link } from 'react-router-dom'
import TradeOrder from '../Interface/TradeOrder'

const Home = () => {
  return (
    <div>
      <div>
        <div>Home</div>
        <div>test</div>
        <Link to="/TradeOrder">TradeOrder</Link>
      </div>
    </div>
  )
}

export default Home
