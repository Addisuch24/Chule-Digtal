import { useState } from "react";

function formatBirr(val) {
  return `${Number(val || 0).toLocaleString("en-US")} ETB`;
}

export default function DashboardCharts({
  notes = {},
  orders = {},
  customers = {},
  financial = {},
}) {
  const [isOpen, setIsOpen] = useState(true);

  // Business counts
  const notesCount = Number(notes.total || 0);
  const ordersCount = Number(orders.total || 0);
  const customersCount = Number(customers.total || 0);
  const maxOverview = Math.max(notesCount, ordersCount, customersCount, 1);

  // Financial amounts
  const debit = Number(financial.totalDebit || 0);
  const credit = Number(financial.totalCredit || 0);
  const netBalance = Number(financial.balance || 0);
  const maxFinance = Math.max(debit, credit, 1);


  // Customers breakdown
  const customersWithDebt = Number(customers.withDebt || 0);
  const settledCustomers = Number(customers.settled || 0);
  const customersWithCredit = Number(customers.withCredit || 0);
  const totalCustBreakdown = customersWithDebt + settledCustomers + customersWithCredit || 1;

  return (
    <section className="panel dashboard-analytics-panel" style={{ marginTop: "1rem" }}>
      <div
        className="section-heading"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          cursor: "pointer",
          userSelect: "none",
        }}
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <div>
          <p className="eyebrow" style={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Visual Analytics
          </p>
          <h2 style={{ margin: 0 }}>Business & Financial Insights</h2>
        </div>
        <button
          type="button"
          className="button button-secondary"
          style={{ fontSize: "0.85rem", padding: "0.35rem 0.85rem" }}
          aria-expanded={isOpen}
          onClick={(e) => {
            e.stopPropagation();
            setIsOpen((prev) => !prev);
          }}
        >
          {isOpen ? "Hide Visualizations ▲" : "Show Visualizations ▼"}
        </button>
      </div>

      {isOpen && (
        <div
          className="analytics-grid"
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
            gap: "1.25rem",
            marginTop: "1.25rem",
          }}
        >
          {/* Card 1: Overview Volume */}
          <div
            className="chart-card"
            style={{
              padding: "1rem",
              background: "rgba(255, 255, 255, 0.03)",
              border: "1px solid rgba(255, 255, 255, 0.08)",
              borderRadius: "12px",
            }}
          >
            <h3 style={{ fontSize: "1rem", marginBottom: "0.75rem", fontWeight: 600 }}>
              Records Overview
            </h3>
            <svg viewBox="0 0 300 140" style={{ width: "100%", height: "auto" }}>
              {/* Grid Lines */}
              <line x1="60" y1="20" x2="280" y2="20" stroke="rgba(255,255,255,0.06)" strokeDasharray="3,3" />
              <line x1="60" y1="60" x2="280" y2="60" stroke="rgba(255,255,255,0.06)" strokeDasharray="3,3" />
              <line x1="60" y1="100" x2="280" y2="100" stroke="rgba(255,255,255,0.06)" strokeDasharray="3,3" />

              {/* Bar 1: Notes */}
              <text x="5" y="32" fill="currentColor" fontSize="11" opacity="0.8">Notes</text>
              <rect
                x="60"
                y="20"
                width={Math.max((notesCount / maxOverview) * 200, 4)}
                height="16"
                rx="4"
                fill="#3b82f6"
              />
              <text x={70 + Math.max((notesCount / maxOverview) * 200, 4)} y="33" fill="currentColor" fontSize="11" fontWeight="bold">
                {notesCount}
              </text>

              {/* Bar 2: Orders */}
              <text x="5" y="72" fill="currentColor" fontSize="11" opacity="0.8">Orders</text>
              <rect
                x="60"
                y="60"
                width={Math.max((ordersCount / maxOverview) * 200, 4)}
                height="16"
                rx="4"
                fill="#8b5cf6"
              />
              <text x={70 + Math.max((ordersCount / maxOverview) * 200, 4)} y="73" fill="currentColor" fontSize="11" fontWeight="bold">
                {ordersCount}
              </text>

              {/* Bar 3: Customers */}
              <text x="5" y="112" fill="currentColor" fontSize="11" opacity="0.8">Customers</text>
              <rect
                x="60"
                y="100"
                width={Math.max((customersCount / maxOverview) * 200, 4)}
                height="16"
                rx="4"
                fill="#10b981"
              />
              <text x={70 + Math.max((customersCount / maxOverview) * 200, 4)} y="113" fill="currentColor" fontSize="11" fontWeight="bold">
                {customersCount}
              </text>
            </svg>
            <div style={{ display: "flex", justifyContent: "space-between", marginTop: "0.5rem", fontSize: "0.8rem", opacity: 0.7 }}>
              <span>Total items across business</span>
              <span>Live DB synced</span>
            </div>
          </div>

          {/* Card 2: Financial Health */}
          <div
            className="chart-card"
            style={{
              padding: "1rem",
              background: "rgba(255, 255, 255, 0.03)",
              border: "1px solid rgba(255, 255, 255, 0.08)",
              borderRadius: "12px",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <h3 style={{ fontSize: "1rem", marginBottom: "0.75rem", fontWeight: 600 }}>
                Ledger Breakdown
              </h3>
              <span
                style={{
                  fontSize: "0.75rem",
                  padding: "0.2rem 0.5rem",
                  borderRadius: "6px",
                  backgroundColor: netBalance >= 0 ? "rgba(16, 185, 129, 0.15)" : "rgba(239, 68, 68, 0.15)",
                  color: netBalance >= 0 ? "#10b981" : "#ef4444",
                  fontWeight: "bold",
                }}
              >
                {netBalance >= 0 ? "+ Net Credit" : "- Net Debt"}
              </span>
            </div>

            <svg viewBox="0 0 300 140" style={{ width: "100%", height: "auto" }}>
              {/* Debit Bar */}
              <text x="5" y="40" fill="currentColor" fontSize="11" opacity="0.8">Total Debit</text>
              <rect
                x="80"
                y="26"
                width={Math.max((debit / maxFinance) * 180, 4)}
                height="18"
                rx="4"
                fill="#ef4444"
              />
              <text x={90 + Math.max((debit / maxFinance) * 180, 4)} y="40" fill="#ef4444" fontSize="10" fontWeight="bold">
                {formatBirr(debit)}
              </text>

              {/* Credit Bar */}
              <text x="5" y="90" fill="currentColor" fontSize="11" opacity="0.8">Total Credit</text>
              <rect
                x="80"
                y="76"
                width={Math.max((credit / maxFinance) * 180, 4)}
                height="18"
                rx="4"
                fill="#10b981"
              />
              <text x={90 + Math.max((credit / maxFinance) * 180, 4)} y="90" fill="#10b981" fontSize="10" fontWeight="bold">
                {formatBirr(credit)}
              </text>

              {/* Baseline */}
              <line x1="80" y1="115" x2="280" y2="115" stroke="rgba(255,255,255,0.1)" />
            </svg>
            <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.8rem", opacity: 0.8 }}>
              <span>Balance: <strong>{formatBirr(netBalance)}</strong></span>
            </div>
          </div>

          {/* Card 3: Customer Account Distribution */}
          <div
            className="chart-card"
            style={{
              padding: "1rem",
              background: "rgba(255, 255, 255, 0.03)",
              border: "1px solid rgba(255, 255, 255, 0.08)",
              borderRadius: "12px",
            }}
          >
            <h3 style={{ fontSize: "1rem", marginBottom: "0.75rem", fontWeight: 600 }}>
              Customer Accounts ({customersCount} total)
            </h3>

            {/* Stacked Segment Bar */}
            <div
              style={{
                display: "flex",
                height: "16px",
                borderRadius: "8px",
                overflow: "hidden",
                margin: "1rem 0",
                background: "rgba(255,255,255,0.06)",
              }}
            >
              {customersWithDebt > 0 && (
                <div
                  title={`Debt: ${customersWithDebt}`}
                  style={{
                    width: `${(customersWithDebt / totalCustBreakdown) * 100}%`,
                    backgroundColor: "#ef4444",
                  }}
                />
              )}
              {settledCustomers > 0 && (
                <div
                  title={`Settled: ${settledCustomers}`}
                  style={{
                    width: `${(settledCustomers / totalCustBreakdown) * 100}%`,
                    backgroundColor: "#10b981",
                  }}
                />
              )}
              {customersWithCredit > 0 && (
                <div
                  title={`Credit: ${customersWithCredit}`}
                  style={{
                    width: `${(customersWithCredit / totalCustBreakdown) * 100}%`,
                    backgroundColor: "#3b82f6",
                  }}
                />
              )}
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem", fontSize: "0.8rem" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span><span style={{ color: "#ef4444" }}>●</span> With Debt:</span>
                <strong>{customersWithDebt}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span><span style={{ color: "#10b981" }}>●</span> Settled (0):</span>
                <strong>{settledCustomers}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span><span style={{ color: "#3b82f6" }}>●</span> In Credit (+):</span>
                <strong>{customersWithCredit}</strong>
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
