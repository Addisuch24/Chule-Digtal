import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";

const formatAmount = (value) =>
  `${Number(value || 0).toLocaleString("en-US")} ETB`;
const formatDate = (value) =>
  value
    ? new Date(value).toLocaleString("en-US", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Not available";

function getArrayData(response, key) {
  const data = response?.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.[key])) return data[key];
  return [];
}

function Finance() {
  const [movements, setMovements] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [customerName, setCustomerName] = useState("");
  const [phone, setPhone] = useState("");
  const [signedAmount, setSignedAmount] = useState("");
  const [description, setDescription] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [editingMovement, setEditingMovement] = useState(null);
  const [expandedCustomerId, setExpandedCustomerId] = useState(null);
  const [openMenuMovementId, setOpenMenuMovementId] = useState(null);
  const [openMenuCustomerKey, setOpenMenuCustomerKey] = useState(null);
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [settlingCustomer, setSettlingCustomer] = useState(null);
  const [selectedMovementIds, setSelectedMovementIds] = useState([]);
  const [autosaveStatus, setAutosaveStatus] = useState("");
  const [busyMovementId, setBusyMovementId] = useState(null);
  const movementList = useRef(null);
  const editSaveTimer = useRef(null);
  const latestEdit = useRef(null);
  const saveQueue = useRef(Promise.resolve());

  const loadMovements = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/financial-movements");
      setMovements(getArrayData(response, "movements"));
    } catch (requestError) {
      console.error("Load movements error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not load financial movements."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  const loadCustomers = useCallback(async () => {
    try {
      const response = await api.get("/customers");
      setCustomers(getArrayData(response, "customers"));
    } catch (requestError) {
      console.error("Load customers error:", requestError);
    }
  }, []);

  useEffect(() => {
    loadMovements();
    loadCustomers();
  }, [loadMovements, loadCustomers]);

  useEffect(() => {
    if (openMenuMovementId === null && openMenuCustomerKey === null) return undefined;

    function handleMenuDismiss(event) {
      if (event.type === "keydown" && event.key === "Escape") {
        setOpenMenuMovementId(null);
        setOpenMenuCustomerKey(null);
      } else if (
        event.type === "pointerdown" &&
        !movementList.current?.contains(event.target)
      ) {
        setOpenMenuMovementId(null);
        setOpenMenuCustomerKey(null);
      }
    }

    document.addEventListener("keydown", handleMenuDismiss);
    document.addEventListener("pointerdown", handleMenuDismiss);

    return () => {
      document.removeEventListener("keydown", handleMenuDismiss);
      document.removeEventListener("pointerdown", handleMenuDismiss);
    };
  }, [openMenuMovementId, openMenuCustomerKey]);

  const saveMovementEdit = useCallback(
    (movement) => {
      if (!movement || !movement.id) return;

      const nextCustomerId = Number(movement.customer_id);
      const signedAmountValue = Number(movement.amount);
      if (
        !Number.isFinite(signedAmountValue) ||
        signedAmountValue === 0 ||
        !nextCustomerId
      ) {
        setAutosaveStatus("Enter a customer and a non-zero amount.");
        return;
      }
      const nextAmount = Math.abs(signedAmountValue);
      const nextType = signedAmountValue < 0 ? "DEBIT" : "CREDIT";
      const nextDescription = String(movement.description || "").trim();
      const customerName =
        customers.find((customer) => Number(customer.id) === nextCustomerId)
          ?.full_name ||
        customers.find((customer) => Number(customer.id) === nextCustomerId)
          ?.name ||
        `Customer #${nextCustomerId}`;

      setAutosaveStatus("Saving...");

      const request = saveQueue.current.then(() =>
        api.patch(`/financial-movements/${movement.id}`, {
          customerId: nextCustomerId,
          amount: nextAmount,
          movementType: nextType,
          description: nextDescription,
        })
      );

      const handledRequest = request.then(
        () => {
          setMovements((currentMovements) =>
            currentMovements.map((currentMovement) =>
              currentMovement.id === movement.id
                ? {
                    ...currentMovement,
                    customer_id: nextCustomerId,
                    customer_name: customerName,
                        customer_phone:
                          customers.find(
                            (customer) => Number(customer.id) === nextCustomerId
                          )?.phone || "",
                    amount: nextAmount,
                    movement_type: nextType,
                    description: nextDescription,
                    verification_status: "PENDING",
                    verified_by: null,
                    verified_at: null,
                    verification_note: null,
                  }
                : currentMovement
            )
          );
          setAutosaveStatus("Saved");
        },
        (requestError) => {
          console.error("Autosave movement error:", requestError);
          setAutosaveStatus(
            requestError.response?.data?.message ||
              "Could not save changes. Keep this page open and try again."
          );
        }
      );

      saveQueue.current = handledRequest.then(
        () => undefined,
        () => undefined
      );
    },
    [customers]
  );

  useEffect(
    () => () => {
      if (editSaveTimer.current) {
        clearTimeout(editSaveTimer.current);
        saveMovementEdit(latestEdit.current);
      }
    },
    [saveMovementEdit]
  );

  function updateEditingMovement(field, value) {
    const nextEdit = { ...latestEdit.current, [field]: value };
    latestEdit.current = nextEdit;
    setEditingMovement(nextEdit);
    setAutosaveStatus("Unsaved changes");

    if (editSaveTimer.current) clearTimeout(editSaveTimer.current);
    editSaveTimer.current = setTimeout(() => {
      editSaveTimer.current = null;
      saveMovementEdit(nextEdit);
    }, 400);
  }

  function flushMovementEdit() {
    if (editSaveTimer.current) {
      clearTimeout(editSaveTimer.current);
      editSaveTimer.current = null;
      saveMovementEdit(latestEdit.current);
    }
  }

  async function handleCreateMovement(event) {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!customerName.trim()) {
      setError("Please enter the customer's name.");
      return;
    }
    const enteredAmount = Number(signedAmount);
    if (
      signedAmount === "" ||
      !Number.isFinite(enteredAmount) ||
      enteredAmount === 0
    ) {
      setError("Enter an amount greater than or less than zero.");
      return;
    }

    try {
      setSubmitting(true);
      const normalizedName = customerName.trim().toLocaleLowerCase();
      const normalizedPhone = phone.replace(/\D/g, "");

      // 1. Check if an existing customer matches by phone (if valid digits provided)
      let customer = normalizedPhone && normalizedPhone.length >= 7
        ? customers.find(
            (item) =>
              String(item.phone || "").replace(/\D/g, "") === normalizedPhone
          )
        : null;

      // 2. If not found by phone, match by customer name (case-insensitive)
      if (!customer) {
        customer = customers.find(
          (item) =>
            String(item.full_name || item.name || "").trim().toLocaleLowerCase() ===
            normalizedName
        );
      }

      // 3. If customer exists and new/changed phone was entered, update their phone in-place
      if (customer) {
        const trimmedPhone = phone.trim();
        if (trimmedPhone && trimmedPhone !== (customer.phone || "")) {
          try {
            await api.patch(`/customers/${customer.id}`, { phone: trimmedPhone });
            customer.phone = trimmedPhone;
          } catch (phoneUpdateErr) {
            console.warn("Could not update customer phone:", phoneUpdateErr);
          }
        }
      } else {
        // Customer truly does not exist yet, create them
        const customerResponse = await api.post("/customers", {
          full_name: customerName.trim(),
          phone: phone.trim() || null,
        });
        customer = customerResponse.data?.data;
        if (!customer?.id) {
          throw new Error("Customer was created without a valid ID.");
        }
      }

      const nextMovementType = enteredAmount < 0 ? "DEBIT" : "CREDIT";
      const payload = {
        customerId: Number(customer.id),
        movementType: nextMovementType,
        amount: Math.abs(enteredAmount),
        description: description.trim() || null,
      };

      await api.post("/financial-movements", payload);

      setMessage("Saved successfully.");
      setCustomerName("");
      setPhone("");
      setSignedAmount("");
      setDescription("");
      await Promise.all([loadMovements(), loadCustomers()]);
    } catch (requestError) {
      console.error("Create financial movement error:", requestError);
      setError(
        requestError.response?.data?.message ||
          requestError.message ||
          "We could not record that movement."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteMovement(movement) {
    const customerLabel =
      movement.customer_name ||
      movement.full_name ||
      `Customer #${movement.customer_id}`;
    if (
      !window.confirm(
        `Delete the ${movement.movement_type || "movement"} for ${customerLabel}? This cannot be undone.`
      )
    ) {
      return;
    }

    try {
      setBusyMovementId(movement.id);
      await api.delete(`/financial-movements/${movement.id}`);
      if (latestEdit.current?.id === movement.id) {
        latestEdit.current = null;
        setEditingMovement(null);
      }
      setOpenMenuMovementId(null);
      setMessage("Financial movement deleted successfully.");
      await loadMovements();
    } catch (requestError) {
      console.error("Delete movement error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "We could not delete this financial movement."
      );
    } finally {
      setBusyMovementId(null);
    }
  }

  async function handleDeleteCustomer(group) {
    if (
      !window.confirm(
        `Delete customer "${group.name}" and all associated ledger records? This will decrease the customer count on the dashboard.`
      )
    ) {
      return;
    }

    try {
      setSubmitting(true);
      setMessage("");
      setError("");
      await api.delete(`/customers/${group.id}`);
      setMessage(`Customer "${group.name}" was deleted successfully.`);
      await Promise.all([loadMovements(), loadCustomers()]);
    } catch (requestError) {
      console.error("Delete customer error:", requestError);
      setError(
        requestError.response?.data?.message ||
          "Could not delete this customer."
      );
    } finally {
      setSubmitting(false);
    }
  }

  function getCustomerStatus(group) {
    const balance = Number(group.balance || 0);
    if (balance < 0) {
      return {
        statusKey: "DEBT",
        amharic: "ተበድሩዋል",
        english: "Tebedruwal",
        badgeClass: "customer-status-badge badge-debt",
      };
    }
    if (balance > 0) {
      return {
        statusKey: "CREDIT",
        amharic: "ቀር ሂሳብ",
        english: "Ker Hisab",
        badgeClass: "customer-status-badge badge-credit",
      };
    }
    // Settled / Zero balance: inspect non-rejected transaction history
    const nonRejected = (group.movements || [])
      .filter((m) => (m.verification_status || m.verificationStatus) !== "REJECTED")
      .sort((a, b) => new Date(b.created_at || b.date || 0) - new Date(a.created_at || a.date || 0));

    const lastMovement = nonRejected[0];
    if (lastMovement) {
      const type = (lastMovement.movement_type || lastMovement.movementType || "").toUpperCase();
      if (type === "CREDIT") {
        return {
          statusKey: "PAID",
          amharic: "ከፍለዋል",
          english: "Kefluwal",
          badgeClass: "customer-status-badge badge-settled-default",
        };
      } else if (type === "DEBIT") {
        return {
          statusKey: "RECEIVED",
          amharic: "ተቀብለዋል",
          english: "Tekebluwal",
          badgeClass: "customer-status-badge badge-settled-default",
        };
      }
    }

    return {
      statusKey: "SETTLED",
      amharic: "ተስተካክሏል",
      english: "Settled",
      badgeClass: "customer-status-badge badge-settled-default",
    };
  }

  async function handleSaveCustomerEdit(event) {
    event.preventDefault();
    if (!editingCustomer || !editingCustomer.id) return;
    const trimmedName = (editingCustomer.full_name || "").trim();
    if (!trimmedName) {
      setError("Customer name cannot be empty.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      setMessage("");
      await api.patch(`/customers/${editingCustomer.id}`, {
        full_name: trimmedName,
        phone: (editingCustomer.phone || "").trim() || null,
      });
      setMessage(`Customer "${trimmedName}" updated successfully.`);
      setEditingCustomer(null);
      await Promise.all([loadMovements(), loadCustomers()]);
    } catch (requestError) {
      console.error("Update customer error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to update customer."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleConfirmSettleCustomer(event) {
    event.preventDefault();
    if (!settlingCustomer || !settlingCustomer.id) return;

    const absAmount = Math.abs(Number(settlingCustomer.balance || 0));
    if (absAmount <= 0) {
      setSettlingCustomer(null);
      return;
    }

    const movementType = settlingCustomer.balance < 0 ? "CREDIT" : "DEBIT";
    const defaultDesc =
      settlingCustomer.balance < 0
        ? "የዕዳ ክፍያ (ከፍለዋል - Settled)"
        : "የቀሪ ብር ክፍያ (ተቀብለዋል - Settled)";

    try {
      setSubmitting(true);
      setError("");
      setMessage("");
      await api.post("/financial-movements", {
        customerId: Number(settlingCustomer.id),
        amount: absAmount,
        movementType,
        description: defaultDesc,
      });
      setMessage(
        `Successfully settled balance for ${settlingCustomer.name}. Marked as ${movementType === "CREDIT" ? "ከፍለዋል (Kefluwal)" : "ተቀብለዋል (Tekebluwal)"}.`
      );
      setSettlingCustomer(null);
      await Promise.all([loadMovements(), loadCustomers()]);
    } catch (requestError) {
      console.error("Settle customer balance error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to settle customer balance."
      );
    } finally {
      setSubmitting(false);
    }
  }

  function handleToggleSelectMovement(id, event) {
    event.stopPropagation();
    setSelectedMovementIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  function handleSelectAllMovements() {
    if (selectedMovementIds.length === filteredMovements.length) {
      setSelectedMovementIds([]);
    } else {
      setSelectedMovementIds(filteredMovements.map((m) => m.id));
    }
  }

  async function handleBulkDeleteMovements() {
    if (selectedMovementIds.length === 0) return;
    if (
      !window.confirm(
        `Delete ${selectedMovementIds.length} selected financial movement(s)? This will adjust customer balances and ledger records permanently.`
      )
    ) {
      return;
    }

    try {
      setSubmitting(true);
      setMessage("");
      setError("");
      const response = await api.post("/financial-movements/bulk-delete", {
        movementIds: selectedMovementIds,
      });
      setMessage(
        `Successfully deleted ${response.data?.data?.deletedCount || selectedMovementIds.length} financial movement(s).`
      );
      setSelectedMovementIds([]);
      await Promise.all([loadMovements(), loadCustomers()]);
    } catch (requestError) {
      console.error("Bulk delete movements error:", requestError);
      setError(
        requestError.response?.data?.message || "Failed to delete movements."
      );
    } finally {
      setSubmitting(false);
    }
  }

  function startEditingMovement(movement) {
    flushMovementEdit();
    const nextEdit = {
      id: movement.id,
      customer_id: movement.customer_id ?? "",
      amount:
        (movement.movement_type || movement.movementType) === "DEBIT"
          ? -Number(movement.amount ?? 0)
          : Number(movement.amount ?? 0),
      description: movement.description || "",
    };
    latestEdit.current = nextEdit;
    setEditingMovement(nextEdit);
    setAutosaveStatus("");
    setOpenMenuMovementId(null);
  }

  const query = search.trim().toLowerCase();
  const filteredMovements = query
    ? movements.filter((movement) =>
        [
          movement.customer_name,
          movement.customer_phone,
          movement.full_name,
          movement.description,
          movement.movement_type,
          movement.verification_status,
        ].some((value) =>
          String(value || "")
            .toLowerCase()
            .includes(query)
        )
      )
    : movements;

  const customerGroups = useMemo(() => {
    // Group all balance records by normalized customer identity so records under one customer name are counted as ONE customer
    const balances = new Map();
    movements.forEach((movement) => {
      const matchedCust = customers.find(
        (c) =>
          Number(c.id) === Number(movement.customer_id) ||
          (c.full_name && movement.customer_name && c.full_name.trim().toLowerCase() === movement.customer_name.trim().toLowerCase())
      );
      const nameKey = (
        matchedCust?.full_name ||
        movement.customer_name ||
        movement.full_name ||
        `customer-${movement.customer_id}`
      ).trim().toLowerCase();

      const balance = balances.get(nameKey) || 0;
      if (movement.verification_status !== "REJECTED") {
        const amountValue = Number(movement.amount || 0);
        balances.set(
          nameKey,
          balance +
            ((movement.movement_type || movement.movementType) === "CREDIT"
              ? amountValue
              : -amountValue)
        );
      }
    });

    const groups = new Map();
    filteredMovements.forEach((movement) => {
      const matchedCust = customers.find(
        (c) =>
          Number(c.id) === Number(movement.customer_id) ||
          (c.full_name && movement.customer_name && c.full_name.trim().toLowerCase() === movement.customer_name.trim().toLowerCase())
      );
      const nameKey = (
        matchedCust?.full_name ||
        movement.customer_name ||
        movement.full_name ||
        `customer-${movement.customer_id}`
      ).trim().toLowerCase();

      if (!groups.has(nameKey)) {
        groups.set(nameKey, {
          id: matchedCust?.id || Number(movement.customer_id),
          name:
            matchedCust?.full_name ||
            movement.customer_name ||
            movement.full_name ||
            `Customer #${movement.customer_id}`,
          phone: matchedCust?.phone || movement.customer_phone || "",
          balance: balances.get(nameKey) || 0,
          movements: [],
        });
      }
      groups.get(nameKey).movements.push(movement);
    });
    return [...groups.values()].sort((first, second) =>
      first.name.localeCompare(second.name)
    );
  }, [customers, filteredMovements, movements]);

  const customersOwed = customerGroups.filter((group) => group.balance < 0);
  const customersInCredit = customerGroups.filter(
    (group) => group.balance > 0
  );
  const customersSettled = customerGroups.filter(
    (group) => group.balance === 0
  );

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Financial Overview"
        title="Customer Balance"
        description="Track income, expenses and keep your customer balances healthy."
        action={<span className="count-label">{movements.length} movements</span>}
        theme="finance"
      />

      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Record an entry</h2>
          </div>
        </div>

        <form className="form-stack" onSubmit={handleCreateMovement}>
          <div className="form-grid finance-entry-grid">
            <label>
              Customer name
              <input
                value={customerName}
                onChange={(event) => {
                  const val = event.target.value;
                  setCustomerName(val);
                  const matched = customers.find(
                    (c) =>
                      String(c.full_name || c.name || "").trim().toLowerCase() ===
                      val.trim().toLowerCase()
                  );
                  if (matched && matched.phone && !phone) {
                    setPhone(matched.phone);
                  }
                }}
                list="finance-customer-suggestions"
                placeholder="Enter customer name"
                autoComplete="off"
                required
              />
              <datalist id="finance-customer-suggestions">
                {customers.map((c) => (
                  <option key={c.id} value={c.full_name || c.name}>
                    {c.phone ? `${c.full_name || c.name} (${c.phone})` : c.full_name || c.name}
                  </option>
                ))}
              </datalist>
            </label>

            <label>
              Phone <span className="muted">(optional)</span>
              <input
                type="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="Enter phone number"
                autoComplete="tel"
              />
            </label>

            <label>
              Amount (birr)
              <input
                type="number"
                step="0.01"
                value={signedAmount}
                onChange={(event) => setSignedAmount(event.target.value)}
                placeholder="-100 or 100"
                required
              />
            </label>

            <label>
              Description <span className="muted">(optional)</span>
              <input
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Add a short note"
              />
            </label>
          </div>
          <p className="finance-sign-hint">
            Negative amount is a debit; positive amount is a credit.
          </p>
          <div className="action-row">
            <button type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Save"}
            </button>
          </div>
        </form>

        {message && <p className="alert alert-success">{message}</p>}
        {error && <p className="alert alert-error">{error}</p>}
      </section>

      <section className="panel" ref={movementList}>
        <div className="section-heading">
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
            {filteredMovements.length > 0 && (
              <input
                type="checkbox"
                checked={selectedMovementIds.length > 0 && selectedMovementIds.length === filteredMovements.length}
                onChange={handleSelectAllMovements}
                aria-label="Select all movements"
                title="Select all movements"
                style={{ width: "18px", height: "18px", cursor: "pointer" }}
              />
            )}
            <div>
              <p className="eyebrow">Shared ledger</p>
              <h2>Customer balances</h2>
            </div>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search movements by customer, description, type..."
            aria-label="Search financial movements"
          />
        </div>

        {selectedMovementIds.length > 0 && (
          <div
            className="bulk-action-bar"
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "0.6rem 1rem",
              marginBottom: "1rem",
              background: "rgba(239, 68, 68, 0.12)",
              border: "1px solid rgba(239, 68, 68, 0.3)",
              borderRadius: "8px",
            }}
          >
            <span>
              <strong>{selectedMovementIds.length}</strong> movement(s) selected
            </span>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button
                type="button"
                className="button button-danger"
                style={{
                  backgroundColor: "#ef4444",
                  color: "#fff",
                  border: "none",
                  padding: "0.4rem 0.8rem",
                  borderRadius: "6px",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
                onClick={handleBulkDeleteMovements}
                disabled={submitting}
              >
                {submitting ? "Deleting..." : `Delete Selected (${selectedMovementIds.length})`}
              </button>
              <button
                type="button"
                className="button button-secondary"
                style={{ padding: "0.4rem 0.8rem", borderRadius: "6px" }}
                onClick={() => setSelectedMovementIds([])}
              >
                Clear
              </button>
            </div>
          </div>
        )}

        {loading ? (
          <FeedbackState type="loading" title="Loading financial movements..." />
        ) : filteredMovements.length === 0 ? (
          <FeedbackState
            title={search ? "No matching movements" : "No movements yet"}
            message={
              search
                ? "Try a different search term."
                : "Add an entry above."
            }
          />
        ) : (
          <div className="finance-balance-columns">
            {[
              {
                key: "negative",
                label: "Customers with Debt — Tebedruwal (ተበድሩዋል)",
                sign: "−",
                groups: customersOwed,
                balanceClass: "balance-negative",
              },
              {
                key: "positive",
                label: "Customers in Credit — Ker Hisab (ቀር ሂሳብ)",
                sign: "+",
                groups: customersInCredit,
                balanceClass: "balance-positive",
              },
              ...(customersSettled.length > 0 || query
                ? [
                    {
                      key: "settled",
                      label: "Settled Accounts (የተስተካከሉ ሂሳቦች)",
                      sign: "✓",
                      groups: customersSettled,
                      balanceClass: "balance-settled",
                    },
                  ]
                : []),
            ].map((column) => (
              <section
                className={`finance-balance-column finance-${column.key}`}
                key={column.key}
              >
                <h3 className={`finance-balance-heading ${column.balanceClass}`}>
                  <span aria-hidden="true">{column.sign}</span>
                  {column.label}
                </h3>
                {column.groups.length === 0 ? (
                  <p className="finance-empty-column">No customers</p>
                ) : (
                  <div className="finance-customer-list">
                    {column.groups.map((group) => (
                      <section className="finance-customer-group" key={group.id}>
                        <header
                          className={`finance-customer-heading ${
                            group.balance > 0
                              ? "balance-card-positive"
                              : group.balance < 0
                              ? "balance-card-negative"
                              : "balance-card-zero"
                          }`}
                        >
                          <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                            <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                              <button
                                className="finance-customer-name"
                                type="button"
                                aria-expanded={expandedCustomerId === group.id}
                                aria-controls={`finance-customer-${group.id}-notes`}
                                onClick={() =>
                                  setExpandedCustomerId(
                                    expandedCustomerId === group.id ? null : group.id
                                  )
                                }
                              >
                                {group.name}
                              </button>
                              {(() => {
                                const status = getCustomerStatus(group);
                                return (
                                  <span
                                    className={status.badgeClass}
                                    title={`Status: ${status.english}`}
                                  >
                                    {status.amharic}
                                  </span>
                                );
                              })()}
                            </div>
                            {group.phone && <small>{group.phone}</small>}
                          </div>
                          <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                            <strong
                              className={`finance-customer-balance ${
                                group.balance > 0
                                  ? "balance-positive"
                                  : group.balance < 0
                                  ? "balance-negative"
                                  : "balance-settled"
                              }`}
                            >
                              {group.balance > 0 ? "+" : group.balance < 0 ? "−" : ""}
                              {formatAmount(Math.abs(group.balance))}
                            </strong>
                            {/* Rightmost Action Button with Dropdown Menu */}
                            <div className="order-note-menu" style={{ position: "relative" }}>
                              <button
                                className="order-menu-trigger"
                                type="button"
                                aria-label={`Actions for ${group.name}`}
                                aria-haspopup="menu"
                                aria-expanded={openMenuCustomerKey === group.name}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setOpenMenuCustomerKey(
                                    openMenuCustomerKey === group.name ? null : group.name
                                  );
                                }}
                              >
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                                  <circle cx="12" cy="5" r="2.2" />
                                  <circle cx="12" cy="12" r="2.2" />
                                  <circle cx="12" cy="19" r="2.2" />
                                </svg>
                              </button>
                              {openMenuCustomerKey === group.name && (
                                <div className="order-menu" role="menu">
                                  {group.balance !== 0 && (
                                    <button
                                      type="button"
                                      role="menuitem"
                                      onClick={(e) => {
                                        e.stopPropagation();
                                        setOpenMenuCustomerKey(null);
                                        setSettlingCustomer(group);
                                      }}
                                    >
                                      {group.balance < 0
                                        ? "Settle Debt (ክፍያ መዝግብ)"
                                        : "Settle Credit (ቀሪ አስረክብ)"}
                                    </button>
                                  )}
                                  <button
                                    type="button"
                                    role="menuitem"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setOpenMenuCustomerKey(null);
                                      setEditingCustomer({
                                        id: group.id,
                                        full_name: group.name,
                                        phone: group.phone || "",
                                      });
                                    }}
                                  >
                                    Edit Customer
                                  </button>
                                  <button
                                    className="order-menu-delete"
                                    type="button"
                                    role="menuitem"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setOpenMenuCustomerKey(null);
                                      handleDeleteCustomer(group);
                                    }}
                                    disabled={submitting}
                                  >
                                    Delete Customer
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        </header>
                        {expandedCustomerId === group.id && (
                          <div
                            className="finance-customer-notepad"
                            id={`finance-customer-${group.id}-notes`}
                          >
                            <h4 className="order-reader-title">{group.name}</h4>
                            <div className="finance-notepad-entries">
                              {group.movements.map((movement) => (
                                <article className="movement-card" key={movement.id}>
                {editingMovement?.id === movement.id ? (
                  <div className="order-edit-form">
                    <label>
                      Customer
                      <select
                        value={editingMovement.customer_id}
                        onChange={(event) =>
                          updateEditingMovement("customer_id", event.target.value)
                        }
                      >
                        {customers.map((customer) => (
                          <option key={customer.id} value={customer.id}>
                            {customer.full_name || customer.name || `Customer #${customer.id}`}
                          </option>
                        ))}
                      </select>
                    </label>

                    <div className="form-grid">
                      <label>
                        Amount (birr)
                        <input
                          type="number"
                          step="0.01"
                          value={editingMovement.amount}
                          onChange={(event) =>
                            updateEditingMovement("amount", event.target.value)
                          }
                        />
                      </label>
                    </div>

                    <label>
                      Description
                      <textarea
                        value={editingMovement.description}
                        onChange={(event) =>
                          updateEditingMovement("description", event.target.value)
                        }
                        onBlur={flushMovementEdit}
                        rows="3"
                      />
                    </label>

                    <div className="order-edit-footer">
                      <span role="status">{autosaveStatus}</span>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => {
                          flushMovementEdit();
                          latestEdit.current = null;
                          setEditingMovement(null);
                        }}
                      >
                        Done
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="movement-main" style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                      <input
                        type="checkbox"
                        checked={selectedMovementIds.includes(movement.id)}
                        onChange={(e) => handleToggleSelectMovement(movement.id, e)}
                        aria-label={`Select movement ${movement.id}`}
                        style={{ cursor: "pointer", width: "18px", height: "18px", flexShrink: 0 }}
                      />
                      <div style={{ flex: 1 }}>
                        <div className="movement-title">
                          <strong>
                            {movement.customer_name ||
                              movement.full_name ||
                              `Customer #${movement.customer_id}`}
                          </strong>
                        </div>
                        <p>{movement.description || "No description"}</p>
                        <small>
                          {movement.recorded_by_name ||
                            movement.creator_name ||
                            "Family member"}{" "}
                          · {formatDate(movement.created_at)}
                        </small>
                      </div>
                      <div className="movement-amount">
                        <strong
                          className={
                            (movement.movement_type || movement.movementType) ===
                            "DEBIT"
                              ? "balance-negative"
                              : "balance-positive"
                          }
                        >
                          {(movement.movement_type || movement.movementType) ===
                          "DEBIT"
                            ? "-"
                            : "+"}
                          {formatAmount(movement.amount)}
                        </strong>
                        <div className="order-note-menu">
                          <button
                            className="order-menu-trigger"
                            type="button"
                            aria-label={`More actions for ${movement.customer_name || `customer ${movement.customer_id}`}`}
                            aria-haspopup="menu"
                            aria-expanded={openMenuMovementId === movement.id}
                            onClick={() =>
                              setOpenMenuMovementId(
                                openMenuMovementId === movement.id ? null : movement.id
                              )
                            }
                          >
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                              <circle cx="12" cy="5" r="2.2" />
                              <circle cx="12" cy="12" r="2.2" />
                              <circle cx="12" cy="19" r="2.2" />
                            </svg>
                          </button>
                          {openMenuMovementId === movement.id && (
                            <div className="order-menu" role="menu">
                              <button
                                type="button"
                                role="menuitem"
                                onClick={() => startEditingMovement(movement)}
                              >
                                Edit
                              </button>
                              <button
                                className="order-menu-delete"
                                type="button"
                                role="menuitem"
                                onClick={() => handleDeleteMovement(movement)}
                                disabled={busyMovementId === movement.id}
                              >
                                Delete
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </>
                )}
                                </article>
                              ))}
                            </div>
                          </div>
                        )}
                      </section>
                    ))}
                  </div>
                )}
              </section>
            ))}
          </div>
        )}
      </section>
      {editingCustomer && (
        <div className="modal-backdrop" onClick={() => setEditingCustomer(null)}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-customer-modal-title"
          >
            <div className="modal-header">
              <h3 id="edit-customer-modal-title">Edit Customer</h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setEditingCustomer(null)}
                aria-label="Close dialog"
              >
                &times;
              </button>
            </div>
            <form onSubmit={handleSaveCustomerEdit} className="form-stack">
              <label>
                Customer Name
                <input
                  type="text"
                  value={editingCustomer.full_name}
                  onChange={(e) =>
                    setEditingCustomer({
                      ...editingCustomer,
                      full_name: e.target.value,
                    })
                  }
                  required
                />
              </label>
              <label>
                Phone Number <span className="muted">(optional)</span>
                <input
                  type="tel"
                  value={editingCustomer.phone}
                  onChange={(e) =>
                    setEditingCustomer({
                      ...editingCustomer,
                      phone: e.target.value,
                    })
                  }
                  placeholder="09..."
                />
              </label>
              <div className="action-row" style={{ marginTop: "1rem" }}>
                <button type="submit" disabled={submitting}>
                  {submitting ? "Saving..." : "Save changes"}
                </button>
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => setEditingCustomer(null)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {settlingCustomer && (
        <div className="modal-backdrop" onClick={() => setSettlingCustomer(null)}>
          <div
            className="modal-dialog"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="settle-customer-modal-title"
          >
            <div className="modal-header">
              <h3 id="settle-customer-modal-title">
                {settlingCustomer.balance < 0
                  ? "Settle Debt (ክፍያ መዝግብ)"
                  : "Settle Credit (ቀሪ አስረክብ)"}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setSettlingCustomer(null)}
                aria-label="Close dialog"
              >
                &times;
              </button>
            </div>
            <form onSubmit={handleConfirmSettleCustomer} className="form-stack">
              <p style={{ margin: 0, fontSize: "0.95rem", color: "var(--ink-soft)" }}>
                {settlingCustomer.balance < 0
                  ? `Record a payment of ${formatAmount(Math.abs(settlingCustomer.balance))} for ${settlingCustomer.name} to settle their debt? Their status will update to "ከፍለዋል (Kefluwal)".`
                  : `Record a payout/clearance of ${formatAmount(Math.abs(settlingCustomer.balance))} for ${settlingCustomer.name} to clear their credit balance? Their status will update to "ተቀብለዋል (Tekebluwal)".`}
              </p>
              <div
                style={{
                  padding: "12px",
                  borderRadius: "8px",
                  background:
                    settlingCustomer.balance < 0
                      ? "rgba(239, 68, 68, 0.08)"
                      : "rgba(16, 185, 129, 0.08)",
                  fontWeight: 600,
                  fontSize: "1.05rem",
                  textAlign: "center",
                }}
              >
                Settlement Amount: {formatAmount(Math.abs(settlingCustomer.balance))}
              </div>
              <div className="action-row" style={{ marginTop: "1rem" }}>
                <button
                  type="submit"
                  disabled={submitting}
                  style={{
                    backgroundColor:
                      settlingCustomer.balance < 0 ? "#10b981" : "#17324D",
                    color: "#fff",
                  }}
                >
                  {submitting
                    ? "Recording..."
                    : settlingCustomer.balance < 0
                    ? "Confirm Payment (ከፍለዋል)"
                    : "Confirm Clearance (ተቀብለዋል)"}
                </button>
                <button
                  type="button"
                  className="button-secondary"
                  onClick={() => setSettlingCustomer(null)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Finance;
