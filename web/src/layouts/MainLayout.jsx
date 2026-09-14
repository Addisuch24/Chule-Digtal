import { useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import "./MainLayout.css";

const navigation = [
  { to: "/dashboard", label: "Dashboard", marker: "01" },
  { to: "/customers", label: "Customers", marker: "02" },
  { to: "/orders", label: "Orders", marker: "03" },
  { to: "/finance", label: "Finance", marker: "04" },
  { to: "/notifications", label: "Notifications", marker: "05" },
];

const pageTitles = {
  "/dashboard": "Overview",
  "/customers": "Customer directory",
  "/orders": "Order workspace",
  "/finance": "Finance workspace",
  "/notifications": "Notifications",
};

function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const user = JSON.parse(localStorage.getItem("user") || "null");
  const pageTitle = pageTitles[location.pathname] || "Workspace";

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/login", { replace: true });
  }

  return (
    <div className="layout">
      <button
        className="mobile-menu-button"
        type="button"
        aria-label="Open navigation"
        aria-expanded={mobileNavOpen}
        onClick={() => setMobileNavOpen(true)}
      >
        <span />
        <span />
        <span />
      </button>
      {mobileNavOpen && <button className="nav-scrim" type="button" aria-label="Close navigation" onClick={() => setMobileNavOpen(false)} />}
      <aside className={`sidebar ${mobileNavOpen ? "sidebar-open" : ""}`}>
        <div className="brand-lockup">
          <span className="brand-mark">CD</span>
          <div><strong>Chule Digita</strong><small>Family business hub</small></div>
        </div>
        <div className="profile-block">
          <span className="profile-avatar">{(user?.full_name || user?.username || "F").charAt(0).toUpperCase()}</span>
          <div><strong>{user?.full_name || "Family member"}</strong><small>@{user?.username || "member"}</small></div>
        </div>
        <nav className="primary-nav" aria-label="Primary navigation">
          <span className="nav-label">Workspace</span>
          {navigation.map((item) => <NavLink key={item.to} to={item.to} onClick={() => setMobileNavOpen(false)}><span className="nav-marker">{item.marker}</span><span>{item.label}</span></NavLink>)}
        </nav>
        <div className="sidebar-footer"><p>Shared records, clearer days.</p><button className="logout-button" type="button" onClick={handleLogout}>Log out <span aria-hidden="true">-&gt;</span></button></div>
      </aside>
      <section className="main-section">
        <header className="main-header"><div><span className="header-kicker">Chule Digita / {pageTitle}</span><h2>{pageTitle}</h2></div><div className="header-user"><span className="online-dot" />{user?.full_name || user?.username || "Family member"}</div></header>
        <main className="main-content"><Outlet /></main>
      </section>
    </div>
  );
}

export default MainLayout;
