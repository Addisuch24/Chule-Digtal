require("dotenv").config();
const dns = require("dns");
dns.setDefaultResultOrder("ipv4first");
const fs = require("fs");
const path = require("path");
const { Pool, types } = require("pg");

// Parse PostgreSQL 64-bit integers (int8/bigint, OID 20) as JavaScript numbers
// for backward compatibility with existing API responses and models
types.setTypeParser(types.builtins.INT8, (val) => (val === null ? null : parseInt(val, 10)));

const host = process.env.PGHOST || process.env.DB_HOST || "127.0.0.1";
const port = Number(process.env.PGPORT || process.env.DB_PORT) || 5432;
const user = process.env.PGUSER || process.env.DB_USER || "postgres";
const password = process.env.PGPASSWORD || process.env.DB_PASSWORD || "";
const database = process.env.PGDATABASE || process.env.DB_NAME || "postgres";
const connectionString = process.env.DATABASE_URL || process.env.SUPABASE_DB_URL;

// Load CA root certificate for Supabase strict TLS verification
let sslConfig = false;
const certRelativePath = process.env.PGSSLROOTCERT || "certs/prod-ca-2021.crt";
const certFullPath = path.isAbsolute(certRelativePath)
  ? certRelativePath
  : path.resolve(__dirname, "../../", certRelativePath);

if (fs.existsSync(certFullPath)) {
  const ca = fs.readFileSync(certFullPath, "utf8");
  sslConfig = {
    ca,
    rejectUnauthorized: true,
  };
} else if (
  process.env.NODE_ENV === "production" ||
  host.includes("supabase.co") ||
  host.includes("pooler.supabase.com") ||
  (connectionString && (connectionString.includes("supabase.co") || connectionString.includes("pooler.supabase.com")))
) {
  sslConfig = {
    rejectUnauthorized: true,
  };
}

const poolConfig = connectionString
  ? {
      connectionString,
      ssl: sslConfig,
      max: 10,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 30000,
      keepAlive: true,
      keepAliveInitialDelayMillis: 10000,
    }
  : {
      host,
      port,
      user,
      password,
      database,
      ssl: sslConfig,
      max: 10,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 30000,
      keepAlive: true,
      keepAliveInitialDelayMillis: 10000,
    };

const pool = new Pool(poolConfig);

// Handle unexpected errors on idle pool clients so process does not crash
pool.on("error", (err) => {
  console.warn("[POSTGRES POOL] Unexpected error on idle client:", err.message);
});

// Provide a seamless getConnection helper for backwards compatibility with transactions
pool.getConnection = async function () {
  const client = await pool.connect();
  return {
    query: client.query.bind(client),
    execute: client.query.bind(client),
    beginTransaction: async () => {
      await client.query("BEGIN");
    },
    commit: async () => {
      await client.query("COMMIT");
    },
    rollback: async () => {
      await client.query("ROLLBACK");
    },
    release: () => {
      client.release();
    },
  };
};

module.exports = pool;
