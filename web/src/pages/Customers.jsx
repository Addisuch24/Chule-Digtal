
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../services/api";

function Customers() {
  const [customers, setCustomers] = useState([]);
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function loadCustomers() {
    try {
      const response = await api.get("/customers");
      setCustomers(response.data.data);
    } catch (error) {
      setError(
        error.response?.data?.message ||
          "Failed to load customers"
      );
    }
  }

  useEffect(() => {
    loadCustomers();
  }, []);

  async function handleCreateCustomer(event) {
    event.preventDefault();

    setMessage("");
    setError("");

    try {
      await api.post("/customers", {
        full_name: fullName,
        phone: phone || null,
      });

      setMessage("Customer created successfully!");
      setFullName("");
      setPhone("");

      await loadCustomers();
    } catch (error) {
      setError(
        error.response?.data?.message ||
          "Failed to create customer"
      );
    }
  }

  return (
    <div>
      <h2>Customers</h2>

      <h3>Add Customer</h3>

      <form onSubmit={handleCreateCustomer}>
        <div>
          <label>Full Name</label>
          <br />

          <input
            type="text"
            value={fullName}
            onChange={(event) =>
              setFullName(event.target.value)
            }
            required
          />
        </div>

        <br />

        <div>
          <label>Phone</label>
          <br />

          <input
            type="text"
            value={phone}
            onChange={(event) =>
              setPhone(event.target.value)
            }
          />
        </div>

        <br />

        <button type="submit">
          Add Customer
        </button>
      </form>

      {message && <p>{message}</p>}
      {error && <p>{error}</p>}

      <hr />

      <h3>Customer List</h3>

      {customers.length === 0 && (
        <p>No customers found.</p>
      )}

      {customers.map((customer) => (
        <div key={customer.id}>
          <strong>
            <Link to={`/customers/${customer.id}`}>
              {customer.full_name}
            </Link>
          </strong>

          <p>
            Phone: {customer.phone || "N/A"}
          </p>

          <p>
            Balance: {customer.balance}
          </p>

          <hr />
        </div>
      ))}
    </div>
  );
}
export default Customers;
