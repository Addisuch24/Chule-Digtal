import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";
import FeedbackState from "../components/FeedbackState";
import PageHeader from "../components/PageHeader";

function getArrayData(response, key) {
  const data = response?.data;
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.data)) return data.data;
  if (Array.isArray(data?.[key])) return data[key];
  return [];
}

function Orders() {
  const [orders, setOrders] = useState([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [editingOrder, setEditingOrder] = useState(null);
  const [openMenuOrderId, setOpenMenuOrderId] = useState(null);
  const [autosaveStatus, setAutosaveStatus] = useState("");
  const [busyOrderId, setBusyOrderId] = useState(null);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [formMessage, setFormMessage] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const editSaveTimer = useRef(null);
  const latestEdit = useRef(null);
  const saveQueue = useRef(Promise.resolve());
  const ordersList = useRef(null);

  const loadOrders = useCallback(async (showSpinner = false) => {
    try {
      if (showSpinner) setLoading(true);
      setError("");
      const response = await api.get("/orders");
      setOrders(getArrayData(response, "orders"));
    } catch (requestError) {
      console.error("Load orders error:", requestError);
      setError(
        requestError.response?.data?.message || "We could not load orders."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  useEffect(() => {
    if (openMenuOrderId === null) return undefined;
    function handleMenuDismiss(event) {
      if (
        event.type === "keydown" &&
        event.key === "Escape"
      ) {
        setOpenMenuOrderId(null);
      } else if (
        event.type === "pointerdown" &&
        !ordersList.current?.contains(event.target)
      ) {
        setOpenMenuOrderId(null);
      }
    }
    document.addEventListener("keydown", handleMenuDismiss);
    document.addEventListener("pointerdown", handleMenuDismiss);
    return () => {
      document.removeEventListener("keydown", handleMenuDismiss);
      document.removeEventListener("pointerdown", handleMenuDismiss);
    };
  }, [openMenuOrderId]);

  const saveOrderEdit = useCallback((order) => {
    if (!order) return;
    setAutosaveStatus("Saving...");
    const request = saveQueue.current.then(() =>
      api.patch(`/orders/${order.id}`, {
        title: order.title.trim() || null,
        description: order.description.trim(),
      })
    );
    const handledRequest = request.then(
      () => {
        setOrders((currentOrders) =>
          currentOrders.map((currentOrder) =>
            currentOrder.id === order.id
              ? {
                  ...currentOrder,
                  title: order.title.trim() || null,
                  description: order.description.trim(),
                }
              : currentOrder
          )
        );
        setAutosaveStatus("Saved");
      },
      (requestError) => {
        console.error("Autosave order error:", requestError);
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
  }, []);

  useEffect(
    () => () => {
      if (editSaveTimer.current) {
        clearTimeout(editSaveTimer.current);
        saveOrderEdit(latestEdit.current);
      }
    },
    [saveOrderEdit]
  );

  function updateEditingOrder(field, value) {
    const nextEdit = { ...latestEdit.current, [field]: value };
    latestEdit.current = nextEdit;
    setEditingOrder(nextEdit);
    setAutosaveStatus("Unsaved changes");
    if (editSaveTimer.current) clearTimeout(editSaveTimer.current);
    editSaveTimer.current = setTimeout(() => {
      editSaveTimer.current = null;
      saveOrderEdit(nextEdit);
    }, 400);
  }

  function flushOrderEdit() {
    if (editSaveTimer.current) {
      clearTimeout(editSaveTimer.current);
      editSaveTimer.current = null;
      saveOrderEdit(latestEdit.current);
    }
  }

  async function handleCreateOrder(event) {
    event.preventDefault();
    setFormMessage("");
    setError("");
    if (!title.trim() || !description.trim()) {
      setFormMessage("Add a title and details before creating the order.");
      return;
    }
    try {
      setSubmitting(true);
      const generatedOrderNumber = `ORD-${Date.now().toString().slice(-6)}`;
      await api.post("/orders", {
        orderNumber: generatedOrderNumber,
        title: title.trim(),
        description: description.trim(),
      });
      setFormMessage("Order created successfully.");
      setTitle("");
      setDescription("");
      await loadOrders();
    } catch (requestError) {
      console.error("Create order error:", requestError);
      setFormMessage(
        requestError.response?.data?.message || "We could not create that order."
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteOrder(order) {
    const orderTitle = order.title || order.order_number || `Order #${order.id}`;
    if (!window.confirm(`Delete "${orderTitle}"? This cannot be undone.`)) {
      return;
    }
    setActionMessage("");
    setActionError("");
    try {
      setBusyOrderId(order.id);
      await api.delete(`/orders/${order.id}`);
      if (latestEdit.current?.id === order.id) {
        latestEdit.current = null;
        setEditingOrder(null);
      }
      setOpenMenuOrderId(null);
      setActionMessage("Order deleted successfully.");
      await loadOrders();
    } catch (requestError) {
      console.error("Delete order error:", requestError);
      setActionError(
        requestError.response?.data?.message || "We could not delete this order."
      );
    } finally {
      setBusyOrderId(null);
    }
  }

  function startEditingOrder(order) {
    flushOrderEdit();
    const nextEdit = {
      id: order.id,
      title:
        order.title ||
        order.order_number ||
        order.orderNumber ||
        "",
      description: order.description || "",
    };
    latestEdit.current = nextEdit;
    setEditingOrder(nextEdit);
    setAutosaveStatus("");
    setOpenMenuOrderId(null);
  }

  const filteredOrders = useMemo(() => {
    const query = search.trim().toLowerCase();
    return orders.filter((order) => {
      if (!query) return true;
      return [
        order.title,
        order.order_number,
        order.customer_name,
        order.full_name,
        order.description,
        order.taken_by_name,
      ].some((value) =>
        String(value || "")
          .toLowerCase()
          .includes(query)
      );
    });
  }, [orders, search]);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Work tracking"
        title="Orders"
        description="Create and keep track of shared work in one simple list."
        action={<span className="count-label">{orders.length} total</span>}
      />

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">New work</p>
            <h2>Create an order</h2>
          </div>
        </div>

        <form className="form-stack" onSubmit={handleCreateOrder}>
          <label>
            Order title
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Give this order a title"
              maxLength={150}
            />
          </label>

          <label>
            Details
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="e.g. 24 notebooks and delivery to Bole"
              rows="3"
            />
          </label>

          <div className="action-row">
            <button type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Create order"}
            </button>
          </div>
        </form>

        {formMessage && (
          <p
            className={`alert ${
              formMessage.includes("successfully")
                ? "alert-success"
                : "alert-error"
            }`}
          >
            {formMessage}
          </p>
        )}
      </section>

      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Shared queue</p>
            <h2>All orders</h2>
          </div>
          <input
            className="search-input"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search orders by number, customer..."
            aria-label="Search orders"
          />
        </div>

        {actionMessage && <p className="alert alert-success">{actionMessage}</p>}
        {actionError && <p className="alert alert-error">{actionError}</p>}

        {loading ? (
          <FeedbackState type="loading" title="Loading orders..." />
        ) : error ? (
          <FeedbackState
            type="error"
            title="Orders unavailable"
            message={error}
            action={
              <button type="button" onClick={loadOrders}>
                Try again
              </button>
            }
          />
        ) : filteredOrders.length === 0 ? (
          <FeedbackState
            title={search ? "No matching orders" : "No orders yet"}
            message={
              search
                ? "Try a different search term."
                : "Create an order above to start the queue."
            }
          />
        ) : (
          <div className="orders-notepad" ref={ordersList}>
            {filteredOrders.map((order) => (
              <article className="order-note" key={order.id}>
                {editingOrder?.id === order.id ? (
                  <div className="order-edit-form">
                    <label>
                      Order title
                      <input
                        value={editingOrder.title}
                        onChange={(event) =>
                          updateEditingOrder("title", event.target.value)
                        }
                        onBlur={flushOrderEdit}
                        maxLength={150}
                      />
                    </label>
                    <label>
                      Details
                      <textarea
                        value={editingOrder.description}
                        onChange={(event) =>
                          updateEditingOrder("description", event.target.value)
                        }
                        onBlur={flushOrderEdit}
                        rows="3"
                      />
                    </label>
                    <div className="order-edit-footer">
                      <span role="status">{autosaveStatus}</span>
                      <button
                        className="button-secondary"
                        type="button"
                        onClick={() => {
                          flushOrderEdit();
                          latestEdit.current = null;
                          setEditingOrder(null);
                        }}
                      >
                        Done
                      </button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="order-note-heading">
                      <h3>
                        <Link to={`/orders/${order.id}`}>
                          {order.title ||
                            order.order_number ||
                            order.orderNumber ||
                            `Order #${order.id}`}
                        </Link>
                      </h3>
                      <div className="order-note-menu">
                        <button
                          className="order-menu-trigger"
                          type="button"
                          aria-label={`More actions for ${order.title || order.order_number || `order ${order.id}`}`}
                          aria-haspopup="menu"
                          aria-expanded={openMenuOrderId === order.id}
                          onClick={() =>
                            setOpenMenuOrderId(
                              openMenuOrderId === order.id ? null : order.id
                            )
                          }
                        >
                          &hellip;
                        </button>
                        {openMenuOrderId === order.id && (
                          <div className="order-menu" role="menu">
                            <button
                              type="button"
                              role="menuitem"
                              onClick={() => startEditingOrder(order)}
                            >
                              Edit
                            </button>
                            <button
                              className="order-menu-delete"
                              type="button"
                              role="menuitem"
                              onClick={() => handleDeleteOrder(order)}
                              disabled={busyOrderId === order.id}
                            >
                              Delete
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  </>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default Orders;
