import axios from "axios";

const PROD_API_URL = "https://chule-digita-backend.vercel.app/api";
const DEV_API_URL = "http://localhost:5000/api";

export function normalizeApiUrl(url) {
  if (!url) return "";
  const trimmed = url.trim().replace(/\/+$/, "");
  return trimmed.endsWith("/api") ? trimmed : `${trimmed}/api`;
}

export function getApiBaseUrl() {
  const metaEnv = (typeof import.meta !== "undefined" && import.meta.env) || {};
  const envUrl = (metaEnv.VITE_API_URL || "").trim();

  // If running in a browser on a non-localhost host, protect against localhost leaks
  const isBrowser = typeof window !== "undefined" && Boolean(window.location?.hostname);
  const isRemoteHost =
    isBrowser &&
    window.location.hostname !== "localhost" &&
    window.location.hostname !== "127.0.0.1";

  if (envUrl) {
    if (isRemoteHost && /^(https?:\/\/)?(localhost|127\.0\.0\.1)(:\d+)?/i.test(envUrl)) {
      console.warn("[API] Localhost URL configured in remote environment; falling back to production backend.");
      return PROD_API_URL;
    }
    return normalizeApiUrl(envUrl);
  }

  // Safe fallback based on Vite build mode
  if (metaEnv.PROD) {
    return PROD_API_URL;
  }

  return DEV_API_URL;
}

const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");

      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

export default api;