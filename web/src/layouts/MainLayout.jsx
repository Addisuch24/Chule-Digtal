import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import "./MainLayout.css";

const navigation = [
  { to: "/dashboard", label: "Dashboard", marker: "01" },
  { to: "/customers", label: "ማስታወሻዎች", marker: "02" },
  { to: "/orders", label: "Orders", marker: "03" },
  { to: "/finance", label: "Finance", marker: "04" },
  { to: "/notifications", label: "Notifications", marker: "05" },
];

function MainLayout() {
  const navigate = useNavigate();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const user = JSON.parse(localStorage.getItem("user") || "null");
  const userName = user?.full_name || user?.username || "Family member";

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
          <div><strong>{userName}</strong></div>
        </div>
        <nav className="primary-nav" aria-label="Primary navigation">
          <span className="nav-label">Workspace</span>
          {navigation.map((item) => <NavLink key={item.to} to={item.to} onClick={() => setMobileNavOpen(false)}><span className="nav-marker">{item.marker}</span><span>{item.label}</span></NavLink>)}
        </nav>
        <div className="sidebar-footer"><p>Shared records, clearer days.</p><button className="logout-button" type="button" onClick={handleLogout}>Log out <span aria-hidden="true">-&gt;</span></button></div>
      </aside>
      <section className="main-section">
        <main className="main-content"><Outlet /></main>
      </section>
    </div>
  );
}

export default MainLayout;
