import { NavLink, Outlet, useNavigate } from "react-router-dom";
import "./MainLayout.css";

function MainLayout() {
  const navigate = useNavigate();

  const user = JSON.parse(
    localStorage.getItem("user") || "null"
  );

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    navigate("/login", { replace: true });
  }

  function getNavLinkStyle({ isActive }) {
    return {
      display: "block",
      padding: "10px 12px",
      borderRadius: "6px",
      textDecoration: "none",
      color: isActive ? "#ffffff" : "#333333",
      backgroundColor: isActive ? "#2563eb" : "transparent",
      fontWeight: isActive ? "bold" : "normal",
    };
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <h2>Chule Digita</h2>

        <p style={{ color: "#666" }}>
          {user?.full_name ||
            user?.username ||
            "Family Member"}
        </p>

        <nav
          style={{
            display: "grid",
            gap: "8px",
            marginTop: "25px",
          }}
        >
          <NavLink
            to="/dashboard"
            style={getNavLinkStyle}
          >
            Dashboard
          </NavLink>

          <NavLink
            to="/customers"
            style={getNavLinkStyle}
          >
            Customers
          </NavLink>

          <NavLink
            to="/orders"
            style={getNavLinkStyle}
          >
            Orders
          </NavLink>

          <NavLink
            to="/finance"
            style={getNavLinkStyle}
          >
            Finance
          </NavLink>

          <NavLink
            to="/notifications"
            style={getNavLinkStyle}
          >
            Notifications
          </NavLink>
        </nav>

        <hr
          style={{
            margin: "25px 0",
            border: "none",
            borderTop: "1px solid #ddd",
          }}
        />

        <button
          onClick={handleLogout}
          style={{
            width: "100%",
            padding: "10px",
            border: "none",
            borderRadius: "6px",
            backgroundColor: "#dc2626",
            color: "#ffffff",
            cursor: "pointer",
          }}
        >
          Logout
        </button>
      </aside>

      <section className="main-section">
        <header className="main-header">
          <h1 style={{ margin: 0 }}>
            Family Business Management System
          </h1>
        </header>

        <main className="main-content">
          <Outlet />
        </main>
      </section>
    </div>
  );
}

export default MainLayout;