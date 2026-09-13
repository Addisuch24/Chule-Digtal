 import { useEffect, useState } from "react";
import api from "../services/api";

function Dashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadDashboard() {
      try {
        const response = await api.get("/dashboard");
        setDashboard(response.data.data);
      } catch (error) {
        setError(
          error.response?.data?.message ||
            "Failed to load dashboard"
        );
      }
    }

    loadDashboard();
  }, []);

  if (error) {
    return <p>{error}</p>;
  }

  if (!dashboard) {
    return <p>Loading dashboard...</p>;
  }

  return (
    <div>
      <h1>Chule Digita</h1>
      <h2>Dashboard</h2>

      <h3>Customers</h3>
      <p>Total: {dashboard.customers.total}</p>
      <p>With Balance: {dashboard.customers.withBalance}</p>
      <p>Settled: {dashboard.customers.settled}</p>

      <h3>Orders</h3>
      <p>Pending: {dashboard.orders.pending}</p>
      <p>Completed: {dashboard.orders.completed}</p>

      <h3>Financial</h3>
      <p>Total Debit: {dashboard.financialMovements.totalDebit}</p>
      <p>Total Credit: {dashboard.financialMovements.totalCredit}</p>
      <p>Balance: {dashboard.financialMovements.balance}</p>

      <h3>Notifications</h3>
      <p>Unread: {dashboard.notifications.unread}</p>
    </div>
  );
}

export default Dashboard;