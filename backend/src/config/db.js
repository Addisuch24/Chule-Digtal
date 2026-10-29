const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });
require("dotenv").config({ path: path.resolve(__dirname, "../../.env") });
require("dotenv").config();
const dns = require("dns");
dns.setDefaultResultOrder("ipv4first");
const fs = require("fs");
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

// Official Supabase Root 2021 CA certificate (CN=Supabase Root 2021 CA, valid 2021-2031)
const SUPABASE_ROOT_CA_2021 = `-----BEGIN CERTIFICATE-----
MIIDxDCCAqygAwIBAgIUbLxMod62P2ktCiAkxnKJwtE9VPYwDQYJKoZIhvcNAQEL
BQAwazELMAkGA1UEBhMCVVMxEDAOBgNVBAgMB0RlbHdhcmUxEzARBgNVBAcMCk5l
dyBDYXN0bGUxFTATBgNVBAoMDFN1cGFiYXNlIEluYzEeMBwGA1UEAwwVU3VwYWJh
c2UgUm9vdCAyMDIxIENBMB4XDTIxMDQyODEwNTY1M1oXDTMxMDQyNjEwNTY1M1ow
azELMAkGA1UEBhMCVVMxEDAOBgNVBAgMB0RlbHdhcmUxEzARBgNVBAcMCk5ldyBD
YXN0bGUxFTATBgNVBAoMDFN1cGFiYXNlIEluYzEeMBwGA1UEAwwVU3VwYWJhc2Ug
Um9vdCAyMDIxIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqQXW
QyHOB+qR2GJobCq/CBmQ40G0oDmCC3mzVnn8sv4XNeWtE5XcEL0uVih7Jo4Dkx1Q
DmGHBH1zDfgs2qXiLb6xpw/CKQPypZW1JssOTMIfQppNQ87K75Ya0p25Y3ePS2t2
GtvHxNjUV6kjOZjEn2yWEcBdpOVCUYBVFBNMB4YBHkNRDa/+S4uywAoaTWnCJLUi
cvTlHmMw6xSQQn1UfRQHk50DMCEJ7Cy1RxrZJrkXXRP3LqQL2ijJ6F4yMfh+Gyb4
O4XajoVj/+R4GwywKYrrS8PrSNtwxr5StlQO8zIQUSMiq26wM8mgELFlS/32Uclt
NaQ1xBRizkzpZct9DwIDAQABo2AwXjALBgNVHQ8EBAMCAQYwHQYDVR0OBBYEFKjX
uXY32CztkhImng4yJNUtaUYsMB8GA1UdIwQYMBaAFKjXuXY32CztkhImng4yJNUt
aUYsMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAB8spzNn+4VU
tVxbdMaX+39Z50sc7uATmus16jmmHjhIHz+l/9GlJ5KqAMOx26mPZgfzG7oneL2b
VW+WgYUkTT3XEPFWnTp2RJwQao8/tYPXWEJDc0WVQHrpmnWOFKU/d3MqBgBm5y+6
jB81TU/RG2rVerPDWP+1MMcNNy0491CTL5XQZ7JfDJJ9CCmXSdtTl4uUQnSuv/Qx
Cea13BX2ZgJc7Au30vihLhub52De4P/4gonKsNHYdbWjg7OWKwNv/zitGDVDB9Y2
CMTyZKG3XEu5Ghl1LEnI3QmEKsqaCLv12BnVjbkSeZsMnevJPs1Ye6TjjJwdik5P
o/bKiIz+Fq8=
-----END CERTIFICATE-----`;

function loadCaCertificate() {
  const envCert = process.env.PGSSLROOTCERT;
  // 1. Support certificate PEM string passed directly via environment variable
  if (envCert && envCert.includes("BEGIN CERTIFICATE")) {
    return envCert.replace(/\\n/g, "\n").trim();
  }

  // 2. Resolve certificate file path across local and serverless runtime directories
  const certFileName = envCert && !envCert.includes("\n") ? envCert : "certs/prod-ca-2021.crt";
  const candidatePaths = [
    path.isAbsolute(certFileName) ? certFileName : null,
    path.join(__dirname, "../../", certFileName),
    path.join(__dirname, "../../certs/prod-ca-2021.crt"),
    path.join(__dirname, "../", certFileName),
    path.join(__dirname, certFileName),
    path.join(process.cwd(), certFileName),
    path.join(process.cwd(), "backend", certFileName),
  ].filter(Boolean);

  for (const candidate of candidatePaths) {
    try {
      if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
        const fileContent = fs.readFileSync(candidate, "utf8").trim();
        if (fileContent.includes("BEGIN CERTIFICATE")) {
          return fileContent;
        }
      }
    } catch (_) {
      // Continue checking next candidate path
    }
  }

  // 3. Fallback: If connecting to Supabase and external cert file wasn't bundled, use verified CA
  const isSupabase =
    host.includes("supabase.co") ||
    host.includes("pooler.supabase.com") ||
    (connectionString && (connectionString.includes("supabase.co") || connectionString.includes("pooler.supabase.com")));

  if (isSupabase || process.env.NODE_ENV === "production") {
    return SUPABASE_ROOT_CA_2021;
  }

  return null;
}

// Load CA root certificate for Supabase strict TLS verification
const isSupabaseHost =
  host.includes("supabase.co") ||
  host.includes("pooler.supabase.com") ||
  (connectionString && (connectionString.includes("supabase.co") || connectionString.includes("pooler.supabase.com")));

const ca = loadCaCertificate();
let sslConfig = false;

if (ca) {
  sslConfig = {
    ca,
    rejectUnauthorized: true,
  };
} else if (process.env.NODE_ENV === "production" || isSupabaseHost) {
  sslConfig = {
    rejectUnauthorized: true,
  };
}

const maxConnections = process.env.PGMAX_CONNECTIONS
  ? Number(process.env.PGMAX_CONNECTIONS)
  : (process.env.NODE_ENV === "production" ? 5 : 10);

const poolConfig = connectionString
  ? {
    connectionString,
    ssl: sslConfig,
    max: maxConnections,
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
      max: maxConnections,
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
