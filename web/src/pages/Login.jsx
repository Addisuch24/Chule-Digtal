import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

function Login() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();

    setError("");
    setLoading(true);

    try {
      const response = await api.post("/auth/login", {
        username: username.trim(),
        password,
      });

      console.log("Login response:", response.data);

      const loginData = response.data?.data;

      if (!loginData?.token) {
        throw new Error(
          "Login succeeded but no authentication token was returned."
        );
      }

      // Save JWT token
      localStorage.setItem("token", loginData.token);

      // Save logged-in user
      if (loginData.user) {
        localStorage.setItem(
          "user",
          JSON.stringify(loginData.user)
        );
      }

      // Verify token was actually saved
      console.log(
        "Token saved:",
        Boolean(localStorage.getItem("token"))
      );

      // Go to protected dashboard
      navigate("/dashboard", { replace: true });
    } catch (requestError) {
      console.error("Login error:", requestError);

      setError(
        requestError.response?.data?.message ||
          requestError.message ||
          "Login failed. Please check your username and password."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "#f5f6fa",
      }}
    >
      <div
        style={{
          width: "100%",
          maxWidth: "400px",
          padding: "30px",
          backgroundColor: "#ffffff",
          borderRadius: "10px",
          boxShadow: "0 4px 15px rgba(0, 0, 0, 0.08)",
        }}
      >
        <h1>Chule Digita</h1>

        <h2>Login</h2>

        <p>
          Family Business Management System
        </p>

        {error && (
          <div
            style={{
              marginBottom: "15px",
              padding: "10px",
              color: "#b91c1c",
              backgroundColor: "#fee2e2",
              borderRadius: "6px",
            }}
          >
            {error}
          </div>
        )}

        <form
          onSubmit={handleSubmit}
          style={{
            display: "grid",
            gap: "15px",
          }}
        >
          <label>
            Username

            <input
              type="text"
              value={username}
              onChange={(event) =>
                setUsername(event.target.value)
              }
              placeholder="Enter username"
              autoComplete="username"
              required
              style={{
                display: "block",
                width: "100%",
                padding: "10px",
                marginTop: "5px",
              }}
            />
          </label>

          <label>
            Password

            <input
              type="password"
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              placeholder="Enter password"
              autoComplete="current-password"
              required
              style={{
                display: "block",
                width: "100%",
                padding: "10px",
                marginTop: "5px",
              }}
            />
          </label>

          <button
            type="submit"
            disabled={loading}
            style={{
              padding: "11px",
              cursor: loading
                ? "not-allowed"
                : "pointer",
            }}
          >
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>
      </div>
    </div>
  );
}

export default Login;
