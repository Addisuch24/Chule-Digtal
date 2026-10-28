import { describe, it } from "node:test";
import assert from "node:assert";
import socket, {
  isVercelServerlessUrl,
  isSocketSupported,
  connectSocket,
  getSocketUrl,
} from "../src/services/socket.js";
import { normalizeApiUrl } from "../src/services/api.js";

describe("Socket Service Configuration & Fallback", () => {
  it("correctly identifies Vercel serverless URLs as unsupported for WebSockets", () => {
    assert.strictEqual(
      isVercelServerlessUrl("https://chule-digita-backend.vercel.app"),
      true,
      "Production Vercel backend domain must be identified as serverless"
    );
    assert.strictEqual(
      isVercelServerlessUrl("https://chule-digtal.vercel.app"),
      true,
      "Vercel frontend domain must be identified as serverless"
    );
    assert.strictEqual(
      isVercelServerlessUrl("http://localhost:5000"),
      false,
      "Localhost backend must not be identified as serverless"
    );
    assert.strictEqual(
      isVercelServerlessUrl("http://127.0.0.1:5000"),
      false,
      "Local IP backend must not be identified as serverless"
    );
    assert.strictEqual(
      isVercelServerlessUrl("https://socket.chuledigital.com"),
      false,
      "Custom dedicated socket host must not be blocked"
    );
  });

  it("ensures socket does not initiate unsupported retries when targeting serverless backend", () => {
    const isSupported = isSocketSupported();
    const url = getSocketUrl();

    if (isVercelServerlessUrl(url)) {
      assert.strictEqual(isSupported, false, "Must return false for serverless URL");
      assert.strictEqual(socket.connected, false, "Socket must not be connected");
      // Invoking connectSocket must remain an inert no-op
      connectSocket();
      assert.strictEqual(socket.connected, false, "Socket connection must not be initiated on serverless backend");
    }
  });

  it("ensures socket instance is safely initialized without autoConnect", () => {
    assert.strictEqual(socket.connected, false, "Socket must not auto-connect on import");
  });

  it("normalizes API URLs to ensure /api suffix is present exactly once", () => {
    assert.strictEqual(normalizeApiUrl("https://chule-digita-backend.vercel.app"), "https://chule-digita-backend.vercel.app/api");
    assert.strictEqual(normalizeApiUrl("https://chule-digita-backend.vercel.app/"), "https://chule-digita-backend.vercel.app/api");
    assert.strictEqual(normalizeApiUrl("https://chule-digita-backend.vercel.app/api"), "https://chule-digita-backend.vercel.app/api");
    assert.strictEqual(normalizeApiUrl("https://chule-digita-backend.vercel.app/api/"), "https://chule-digita-backend.vercel.app/api");
    assert.strictEqual(normalizeApiUrl("http://localhost:5000"), "http://localhost:5000/api");
    assert.strictEqual(normalizeApiUrl(""), "");
  });
});
