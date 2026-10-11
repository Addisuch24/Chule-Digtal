const statusLabels = {
  COMPLETED: "Completed",
  DRAFT: "Draft",
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  PROCESSING: "Processing",
  READY: "Ready",
  CANCELLED: "Cancelled",
  VERIFIED: "Verified",
  REJECTED: "Rejected",
  DEBIT: "Debit",
  CREDIT: "Credit",
  READ: "Read",
  UNREAD: "Unread",
};

function StatusBadge({ value, label }) {
  const normalizedValue = String(value || "UNKNOWN").toUpperCase();
  const tone = normalizedValue.toLowerCase().replace(/\s+/g, "-");

  return (
    <span className={`status-badge status-${tone}`}>
      <span className="status-dot" aria-hidden="true" />
      {label || statusLabels[normalizedValue] || normalizedValue}
    </span>
  );
}

export default StatusBadge;
