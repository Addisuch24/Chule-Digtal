import { io } from "socket.io-client";

const PROD_SOCKET_URL = "https://chule-digita-backend.vercel.app";
const DEV_SOCKET_URL = "http://localhost:5000";

const metaEnv = (typeof import.meta !== "undefined" && import.meta.env) || {};

export function getSocketUrl() {
  const envSocketUrl = (metaEnv.VITE_SOCKET_URL || "").trim();
  const envApiUrl = (metaEnv.VITE_API_URL || "").trim();

  const isBrowser = typeof window !== "undefined" && Boolean(window.location?.hostname);
  const isRemoteHost =
    isBrowser &&
    window.location.hostname !== "localhost" &&
    window.location.hostname !== "127.0.0.1";

  if (envSocketUrl) {
    if (isRemoteHost && /^(https?:\/\/)?(localhost|127\.0\.0\.1)(:\d+)?/i.test(envSocketUrl)) {
      console.warn("[Socket] Localhost socket URL detected on remote host; falling back to production socket URL.");
      return PROD_SOCKET_URL;
    }
    return envSocketUrl;
  }

  if (envApiUrl) {
    const derived = envApiUrl.replace(/\/api\/?$/, "");
    if (isRemoteHost && /^(https?:\/\/)?(localhost|127\.0\.0\.1)(:\d+)?/i.test(derived)) {
      return PROD_SOCKET_URL;
    }
    return derived;
  }

  if (metaEnv.PROD) {
    return PROD_SOCKET_URL;
  }

  return DEV_SOCKET_URL;
}

export function isVercelServerlessUrl(url) {
  if (!url) return false;
  try {
    const parsed = new URL(url, typeof window !== "undefined" ? window.location?.origin : "http://localhost");
    return parsed.hostname.endsWith(".vercel.app");
  } catch {
    return false;
  }
}

export function isSocketSupported() {
  if (metaEnv.VITE_DISABLE_SOCKET === "true") {
    return false;
  }

  const socketUrl = getSocketUrl();
  // Vercel serverless deployments do not support persistent Socket.IO / WebSocket connections
  if (!socketUrl || isVercelServerlessUrl(socketUrl)) {
    return false;
  }

  return true;
}

const socket = io(getSocketUrl(), {
  autoConnect: false,
  reconnection: isSocketSupported(),
});

export function connectSocket() {
  if (!isSocketSupported()) {
    return;
  }

  const token = typeof localStorage !== "undefined" ? localStorage.getItem("token") : null;
  if (token) {
    socket.auth = { token };
    if (!socket.connected) {
      socket.connect();
    }
  }
}

export default socket;
