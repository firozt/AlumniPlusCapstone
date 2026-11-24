import { Route, BrowserRouter as Router, Routes } from 'react-router-dom'

import Home from './pages/Registration/Home'
import Login from './pages/Registration/Login'
import Register from './pages/Registration/Register'
import TradeOrder from './pages/Interface/TradeOrder'
import Activity from './pages/Interface/Activity'
import Callback from './pages/Callback'
import RequireAuth from './auth/RequireAuth'

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/Login" element={<Login />} />
        <Route path="/callback" element={<Callback />} />
        <Route element={<RequireAuth />}>
          <Route path="/TradeOrder" element={<TradeOrder />} />
          <Route path="/Activity" element={<Activity />} />
        </Route>
        <Route path="/Register" element={<Register />} />
      </Routes>
    </Router>
  )
}

export default App
