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
    if (openMenuMovementId === null) return undefined;

    function handleMenuDismiss(event) {
      if (event.type === "keydown" && event.key === "Escape") {
        setOpenMenuMovementId(null);
      } else if (
        event.type === "pointerdown" &&
        !movementList.current?.contains(event.target)
      ) {
        setOpenMenuMovementId(null);
      }
    }

    document.addEventListener("keydown", handleMenuDismiss);
    document.addEventListener("pointerdown", handleMenuDismiss);

    return () => {
      document.removeEventListener("keydown", handleMenuDismiss);
      document.removeEventListener("pointerdown", handleMenuDismiss);
    };
  }, [openMenuMovementId]);

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
      let customer = normalizedPhone
        ? customers.find(
            (item) =>
              String(item.phone || "").replace(/\D/g, "") === normalizedPhone
          )
        : null;

      if (!customer) {
        customer = customers.find(
          (item) =>
            String(item.full_name || "").trim().toLocaleLowerCase() ===
              normalizedName &&
            (!normalizedPhone ||
              String(item.phone || "").replace(/\D/g, "") === normalizedPhone)
        );
      }

      if (!customer) {
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

  const filteredMovements = useMemo(() => {
    const query = search.trim().toLowerCase();
    return movements.filter((movement) => {
      if (!query) return true;
      return [
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
      );
    });
  }, [movements, search]);

  const customerGroups = useMemo(() => {
    const balances = new Map();
    movements.forEach((movement) => {
      const customerIdValue = Number(movement.customer_id);
      const balance = balances.get(customerIdValue) || 0;
      if (movement.verification_status !== "REJECTED") {
        const amountValue = Number(movement.amount || 0);
        balances.set(
          customerIdValue,
          balance +
            ((movement.movement_type || movement.movementType) === "CREDIT"
              ? amountValue
              : -amountValue)
        );
      }
    });

    const groups = new Map();
    filteredMovements.forEach((movement) => {
      const customerIdValue = Number(movement.customer_id);
      if (!groups.has(customerIdValue)) {
        groups.set(customerIdValue, {
          id: customerIdValue,
          name:
            movement.customer_name ||
            movement.full_name ||
            `Customer #${customerIdValue}`,
          phone: movement.customer_phone || "",
          balance: balances.get(customerIdValue) || 0,
          movements: [],
        });
      }
      groups.get(customerIdValue).movements.push(movement);
    });
    return [...groups.values()].sort((first, second) =>
      first.name.localeCompare(second.name)
    );
  }, [filteredMovements, movements]);

  const customersOwed = customerGroups.filter((group) => group.balance < 0);
  const customersInCredit = customerGroups.filter(
    (group) => group.balance > 0
  );

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="የገንዘብ ሂሳብ"
        title="ፋይናንስ"
        description="የደንበኞችን ዕዳና የተቀበሉትን ብር ይመዝግቡ።"
        action={<span className="count-label">{movements.length} movements</span>}
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
                onChange={(event) => setCustomerName(event.target.value)}
                placeholder="Enter customer name"
                autoComplete="name"
                required
              />
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
          <div>
            <p className="eyebrow">Shared ledger</p>
            <h2>Customer balances</h2>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search movements by customer, description, type..."
            aria-label="Search financial movements"
          />
        </div>

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
                label: "ዕዳ ያለባቸው ደንበኞች",
                sign: "−",
                groups: customersOwed,
                balanceClass: "balance-negative",
              },
              {
                key: "positive",
                label: "ቀሪ ብር ያላቸው ደንበኞች",
                sign: "+",
                groups: customersInCredit,
                balanceClass: "balance-positive",
              },
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
                        <header className="finance-customer-heading">
                          <div>
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
                            {group.phone && <small>{group.phone}</small>}
                          </div>
                          <strong
                            className={`finance-customer-balance ${column.balanceClass}`}
                          >
                            {group.balance > 0 ? "+" : "−"}
                            {formatAmount(Math.abs(group.balance))}
                          </strong>
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
                    <div className="movement-main">
                      <div>
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
                            &hellip;
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
    </div>
  );
}

export default Finance;
