import { Route, BrowserRouter as Router, Routes } from 'react-router-dom'

import Home from './pages/Registration/Home'
import TradeOrder from './pages/Interface/TradeOrder'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/TradeOrder" element={<TradeOrder />} />
      </Routes>
    </Router>
  )
}

export default App
