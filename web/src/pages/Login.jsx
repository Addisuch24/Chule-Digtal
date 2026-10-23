import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";
import { connectSocket } from "../services/socket";
import "../layouts/MainLayout.css";

function Login() {
  const navigate = useNavigate();
  const [mode, setMode] = useState("login"); // 'login' | 'register'

  // Registration 5-field state
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

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
      // 5-field registration validation
      const trimmedName = fullName.trim();
      const trimmedPhone = phone.trim();
      const trimmedEmail = email.trim();
      const trimmedUsername = username.trim();

      if (!trimmedName) {
        setError("Name is required.");
        return;
      }

      if (!trimmedEmail) {
        setError("Google account email is required.");
        return;
      }

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(trimmedEmail)) {
        setError("Please enter a valid Google account email address.");
        return;
      }

      if (!trimmedUsername) {
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
          fullName: trimmedName,
          phone: trimmedPhone || null,
          email: trimmedEmail,
          googleAccount: trimmedEmail,
          username: trimmedUsername,
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
          setSuccessMessage("Account registered successfully! Please sign in with your username and password.");
          switchMode("login");
        }
      } catch (requestError) {
        console.error("Registration error:", requestError);
        setError(
          requestError.response?.data?.message ||
            "Registration failed. Please choose another username or email."
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
          <strong>Chule Digital</strong>
        </div>
        <div className="login-copy">
          <p lang="en">Manage your business together.</p>
          <p lang="am">ንግድዎን በአንድነት ያስተዳድሩ።</p>
        </div>
      </section>

      <section className="login-panel">
        <div className="login-card">
          <div className="login-card-header">
            <button
              type="button"
              className="login-register-button"
              onClick={() => switchMode(mode === "login" ? "register" : "login")}
            >
              {mode === "login" ? "Register Account" : "Back to Sign In"}
            </button>
          </div>

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
            {mode === "register" ? (
              <>
                {/* Field 1: Name */}
                <label htmlFor="fullName">
                  1. Name <span className="required">*</span>
                  <input
                    id="fullName"
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Enter your full name"
                    autoComplete="name"
                    required
                  />
                </label>

                {/* Field 2: Phone */}
                <label htmlFor="phone">
                  2. Phone Number <span className="muted">(optional)</span>
                  <input
                    id="phone"
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="e.g. 0911..."
                    autoComplete="tel"
                  />
                </label>

                {/* Field 3: Google Account Email */}
                <label htmlFor="email">
                  3. Google Account Email <span className="required">*</span>
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="example@gmail.com"
                    autoComplete="email"
                    required
                  />
                </label>

                {/* Field 4: Username */}
                <label htmlFor="username">
                  4. Username <span className="required">*</span>
                  <input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Choose a unique username"
                    autoComplete="username"
                    required
                  />
                </label>

                {/* Field 5: Password */}
                <label htmlFor="password">
                  5. Password <span className="required">*</span>
                  <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="At least 4 characters"
                    autoComplete="new-password"
                    required
                  />
                </label>
              </>
            ) : (
              <>
                {/* Login Field 1: Username */}
                <label htmlFor="username">
                  Username
                  <input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Enter your username"
                    autoComplete="username"
                    required
                  />
                </label>

                {/* Login Field 2: Password */}
                <label htmlFor="password">
                  Password
                  <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    autoComplete="current-password"
                    required
                  />
                </label>
              </>
            )}

            <button type="submit" disabled={loading}>
              {loading
                ? mode === "login"
                  ? "Signing in..."
                  : "Registering..."
                : mode === "login"
                ? "Sign in"
                : "Register Account"}
              <span aria-hidden="true"> -&gt;</span>
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

export default Login;
