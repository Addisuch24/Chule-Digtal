import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import { connectSocket } from "../services/socket";

function Login() {
  const navigate = useNavigate();
  const [mode, setMode] = useState("login"); // 'login' | 'register'
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  function switchMode(newMode) {
    setMode(newMode);
    setError("");
    setSuccessMessage("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccessMessage("");

    if (mode === "login") {
      if (!username.trim() || !password) {
        setError("Enter your username and password to continue.");
        return;
      }
      setLoading(true);
      try {
        const response = await api.post("/auth/login", {
          username: username.trim(),
          password,
        });
        const loginData = response.data?.data;
        if (!loginData?.token) {
          throw new Error("Login succeeded but no authentication token was returned.");
        }
        localStorage.setItem("token", loginData.token);
        if (loginData.user) {
          localStorage.setItem("user", JSON.stringify(loginData.user));
        }
        connectSocket();
        navigate("/dashboard", { replace: true });
      } catch (requestError) {
        console.error("Login error:", requestError);
        setError(
          requestError.response?.data?.message ||
            "Login failed. Check your username and password."
        );
      } finally {
        setLoading(false);
      }
    } else {
      if (!fullName.trim()) {
        setError("Full name is required.");
        return;
      }
      if (!username.trim()) {
        setError("Username is required.");
        return;
      }
      if (!password || password.length < 4) {
        setError("Password must be at least 4 characters long.");
        return;
      }
      setLoading(true);
      try {
        const response = await api.post("/auth/register", {
          fullName: fullName.trim(),
          phone: phone.trim() || null,
          username: username.trim(),
          password,
        });
        const registerData = response.data?.data;
        if (registerData?.token) {
          localStorage.setItem("token", registerData.token);
          if (registerData.user) {
            localStorage.setItem("user", JSON.stringify(registerData.user));
          }
          connectSocket();
          navigate("/dashboard", { replace: true });
        } else {
          setSuccessMessage("Family member registered successfully. You can now sign in.");
          switchMode("login");
        }
      } catch (requestError) {
        console.error("Registration error:", requestError);
        setError(
          requestError.response?.data?.message ||
            "Registration failed. Please choose another username."
        );
      } finally {
        setLoading(false);
      }
    }
  }

  return (
    <main className="login-page">
      <section className="login-intro">
        <div className="login-brand">
          <span className="brand-mark">CD</span>
          <strong>Chule Digita</strong>
        </div>
        <div className="login-message">
          <p className="eyebrow">Family business hub</p>
          <h1>Keep the whole business in the room.</h1>
          <p>
            Customers, orders, money, notes, and the people behind every action
            in one shared workspace.
          </p>
        </div>
        <span className="login-footnote">
          Simple records. Shared responsibility.
        </span>
      </section>

      <section className="login-panel">
        <div className="login-panel-inner">
          <div className="segmented-control" role="tablist" style={{ marginBottom: "1.5rem" }}>
            <button
              type="button"
              className={mode === "login" ? "segment-active" : "button-secondary"}
              onClick={() => switchMode("login")}
            >
              Sign In
            </button>
            <button
              type="button"
              className={mode === "register" ? "segment-active" : "button-secondary"}
              onClick={() => switchMode("register")}
            >
              Register Family Member
            </button>
          </div>

          <p className="eyebrow">
            {mode === "login" ? "Welcome back" : "New family member"}
          </p>
          <h2>
            {mode === "login"
              ? "Sign in to your workspace"
              : "Create a family member account"}
          </h2>
          <p className="login-subtitle">
            {mode === "login"
              ? "Use your family member account to continue."
              : "Join the shared business workspace."}
          </p>

          {successMessage && (
            <div className="alert alert-success" role="status">
              {successMessage}
            </div>
          )}
          {error && (
            <div className="alert alert-error" role="alert">
              {error}
            </div>
          )}

          <form className="form-stack" onSubmit={handleSubmit}>
            {mode === "register" && (
              <>
                <label htmlFor="fullName">
                  Full Name
                  <input
                    id="fullName"
                    type="text"
                    value={fullName}
                    onChange={(event) => setFullName(event.target.value)}
                    placeholder="Enter your full name"
                    autoComplete="name"
                    required
                  />
                </label>

                <label htmlFor="phone">
                  Phone Number <span className="muted">(optional)</span>
                  <input
                    id="phone"
                    type="tel"
                    value={phone}
                    onChange={(event) => setPhone(event.target.value)}
                    placeholder="e.g. 0911..."
                    autoComplete="tel"
                  />
                </label>
              </>
            )}

            <label htmlFor="username">
              Username
              <input
                id="username"
                type="text"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="Enter your username"
                autoComplete="username"
                required
              />
            </label>

            <label htmlFor="password">
              Password
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder={
                  mode === "login"
                    ? "Enter your password"
                    : "At least 4 characters"
                }
                autoComplete={
                  mode === "login" ? "current-password" : "new-password"
                }
                required
              />
            </label>

            <button type="submit" disabled={loading}>
              {loading
                ? mode === "login"
                  ? "Signing in..."
                  : "Registering..."
                : mode === "login"
                ? "Sign in"
                : "Register account"}
              <span aria-hidden="true"> -&gt;</span>
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

export default Login;

