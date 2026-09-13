import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";

import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Customers from "./pages/Customers";
import CustomerDetails from "./pages/CustomerDetails";
import Orders from "./pages/Orders";
import OrderDetails from "./pages/OrderDetails";
import Finance from "./pages/Finance";
import Notifications from "./pages/Notifications";

import MainLayout from "./layouts/MainLayout";
import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />

        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route
              path="/dashboard"
              element={<Dashboard />}
            />

            <Route
              path="/customers"
              element={<Customers />}
            />

            <Route
              path="/customers/:customerId"
              element={<CustomerDetails />}
            />

            <Route
              path="/orders"
              element={<Orders />}
            />

            <Route
              path="/orders/:orderId"
              element={<OrderDetails />}
            />

            <Route
              path="/finance"
              element={<Finance />}
            />

            <Route
              path="/notifications"
              element={<Notifications />}
            />
          </Route>
        </Route>

        <Route path="*" element={<Login />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;