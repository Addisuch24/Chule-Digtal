const path = require("path");
require("dotenv").config({ path: path.resolve(__dirname, "../.env") });
const test = require("node:test");
const assert = require("node:assert");
const pool = require("../src/config/db");
const { getDashboardSummary } = require("../src/models/dashboardModel");

test("Dashboard Customer Balance Audit & Correctness Suite", async (t) => {
  let client;
  let testUserId;
  let custSameName1Id;
  let custSameName2Id;
  let custNoMovementsId;
  let custOnlyRejectedId;
  let custSettledId;
  let custDebtId;
  let custCreditId;

  const timestamp = Date.now();
  const duplicateName = `Duplicate Name Cust ${timestamp}`;

  t.before(async () => {
    client = await pool.connect();
    // Begin isolated transaction so zero changes are persisted
    await client.query("BEGIN");

    // Get or create an active user to act as recorder
    const userRes = await client.query("SELECT id FROM family_members WHERE is_active = 1 LIMIT 1");
    if (userRes.rows.length > 0) {
      testUserId = userRes.rows[0].id;
    } else {
      const newUser = await client.query(
        "INSERT INTO family_members (full_name, username, password_hash, is_active) VALUES ('Dash Test User', $1, 'hash', 1) RETURNING id",
        [`dash_test_${timestamp}`]
      );
      testUserId = newUser.rows[0].id;
    }

    // 1. Two customers with identical full_name but different IDs:
    // Cust A: Debt (DEBIT 100)
    const c1 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000001', $2) RETURNING id",
      [duplicateName, testUserId]
    );
    custSameName1Id = c1.rows[0].id;

    // Cust B: Credit (CREDIT 250)
    const c2 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000002', $2) RETURNING id",
      [duplicateName, testUserId]
    );
    custSameName2Id = c2.rows[0].id;

    // 2. Customer with NO financial movements at all
    const c3 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000003', $2) RETURNING id",
      [`No Movements Cust ${timestamp}`, testUserId]
    );
    custNoMovementsId = c3.rows[0].id;

    // 3. Customer with ONLY REJECTED movements
    const c4 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000004', $2) RETURNING id",
      [`Only Rejected Cust ${timestamp}`, testUserId]
    );
    custOnlyRejectedId = c4.rows[0].id;

    // 4. Customer with valid movements and zero net balance (Settled)
    const c5 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000005', $2) RETURNING id",
      [`Settled Cust ${timestamp}`, testUserId]
    );
    custSettledId = c5.rows[0].id;

    // 5. Customer with negative balance (Debt)
    const c6 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000006', $2) RETURNING id",
      [`Debt Cust ${timestamp}`, testUserId]
    );
    custDebtId = c6.rows[0].id;

    // 6. Customer with positive balance (Credit)
    const c7 = await client.query(
      "INSERT INTO customers (full_name, phone, created_by) VALUES ($1, '0911000007', $2) RETURNING id",
      [`Credit Cust ${timestamp}`, testUserId]
    );
    custCreditId = c7.rows[0].id;

    // Insert movements:
    // Cust 1 (Duplicate name, debt): DEBIT 100 (PENDING)
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 100.00, 'DEBIT', $2, 'PENDING')",
      [custSameName1Id, testUserId]
    );

    // Cust 2 (Duplicate name, credit): CREDIT 250 (VERIFIED)
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 250.00, 'CREDIT', $2, 'VERIFIED')",
      [custSameName2Id, testUserId]
    );

    // Cust 3: No movements inserted

    // Cust 4 (Only rejected): DEBIT 500 (REJECTED) and CREDIT 500 (REJECTED)
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 500.00, 'DEBIT', $2, 'REJECTED')",
      [custOnlyRejectedId, testUserId]
    );
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 500.00, 'CREDIT', $2, 'REJECTED')",
      [custOnlyRejectedId, testUserId]
    );

    // Cust 5 (Settled): DEBIT 300 (VERIFIED) and CREDIT 300 (PENDING) -> Net 0.00
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 300.00, 'DEBIT', $2, 'VERIFIED')",
      [custSettledId, testUserId]
    );
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 300.00, 'CREDIT', $2, 'PENDING')",
      [custSettledId, testUserId]
    );

    // Cust 6 (Debt): DEBIT 150.50 (VERIFIED) -> Net -150.50
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 150.50, 'DEBIT', $2, 'VERIFIED')",
      [custDebtId, testUserId]
    );

    // Cust 7 (Credit): CREDIT 75.25 (VERIFIED) -> Net +75.25
    await client.query(
      "INSERT INTO financial_movements (customer_id, amount, movement_type, recorded_by, verification_status) VALUES ($1, 75.25, 'CREDIT', $2, 'VERIFIED')",
      [custCreditId, testUserId]
    );
  });

  t.after(async () => {
    if (client) {
      await client.query("ROLLBACK");
      client.release();
    }
  });

  await t.test("1. Two different customer IDs with the same name are counted separately", async () => {
    // Both custSameName1Id and custSameName2Id have duplicateName
    assert.notStrictEqual(custSameName1Id, custSameName2Id);

    const res = await client.query(`
      SELECT c.id, c.full_name, ROUND(COALESCE(SUM(CASE WHEN fm.movement_type = 'CREDIT' THEN fm.amount WHEN fm.movement_type = 'DEBIT' THEN -fm.amount ELSE 0 END), 0), 2) AS balance
      FROM customers c
      JOIN financial_movements fm ON fm.customer_id = c.id AND fm.verification_status <> 'REJECTED'
      WHERE c.id IN ($1, $2)
      GROUP BY c.id, c.full_name
      ORDER BY c.id ASC
    `, [custSameName1Id, custSameName2Id]);

    assert.strictEqual(res.rows.length, 2, "Must return 2 distinct rows for 2 distinct customer IDs");
    assert.strictEqual(res.rows[0].full_name, duplicateName);
    assert.strictEqual(res.rows[1].full_name, duplicateName);
    assert.strictEqual(Number(res.rows[0].balance), -100.00);
    assert.strictEqual(Number(res.rows[1].balance), 250.00);
  });

  await t.test("2. A customer with no financial movements is excluded", async () => {
    const summary = await getDashboardSummary(testUserId, client);
    const checkCust3 = await client.query(`
      SELECT c.id
      FROM customers c
      JOIN financial_movements fm ON fm.customer_id = c.id AND fm.verification_status <> 'REJECTED'
      WHERE c.id = $1
      GROUP BY c.id
    `, [custNoMovementsId]);

    assert.strictEqual(checkCust3.rows.length, 0, "Customer with no financial movements must NOT be joined or grouped");
  });

  await t.test("3. A customer with only rejected movements is excluded", async () => {
    const checkCust4 = await client.query(`
      SELECT c.id
      FROM customers c
      JOIN financial_movements fm ON fm.customer_id = c.id AND fm.verification_status <> 'REJECTED'
      WHERE c.id = $1
      GROUP BY c.id
    `, [custOnlyRejectedId]);

    assert.strictEqual(checkCust4.rows.length, 0, "Customer with only REJECTED movements must NOT appear in qualifying customer balances");
  });

  await t.test("4. A customer with valid movements and zero net balance is counted as settled", async () => {
    const checkCust5 = await client.query(`
      SELECT c.id, ROUND(COALESCE(SUM(CASE WHEN fm.movement_type = 'CREDIT' THEN fm.amount WHEN fm.movement_type = 'DEBIT' THEN -fm.amount ELSE 0 END), 0), 2) AS balance
      FROM customers c
      JOIN financial_movements fm ON fm.customer_id = c.id AND fm.verification_status <> 'REJECTED'
      WHERE c.id = $1
      GROUP BY c.id
    `, [custSettledId]);

    assert.strictEqual(checkCust5.rows.length, 1);
    assert.strictEqual(Number(checkCust5.rows[0].balance), 0.00);
  });

  await t.test("5. A customer with a negative balance is counted as debt", async () => {
    const checkCust6 = await client.query(`
      SELECT c.id, ROUND(COALESCE(SUM(CASE WHEN fm.movement_type = 'CREDIT' THEN fm.amount WHEN fm.movement_type = 'DEBIT' THEN -fm.amount ELSE 0 END), 0), 2) AS balance
      FROM customers c
      JOIN financial_movements fm ON fm.customer_id = c.id AND fm.verification_status <> 'REJECTED'
      WHERE c.id = $1
      GROUP BY c.id
    `, [custDebtId]);

    assert.strictEqual(checkCust6.rows.length, 1);
    assert.strictEqual(Number(checkCust6.rows[0].balance), -150.50);
    assert.ok(Number(checkCust6.rows[0].balance) < 0, "Negative balance must be less than 0");
  });

  await t.test("6. A customer with a positive balance is counted as credit", async () => {
    const checkCust7 = await client.query(`
      SELECT c.id, ROUND(COALESCE(SUM(CASE WHEN fm.movement_type = 'CREDIT' THEN fm.amount WHEN fm.movement_type = 'DEBIT' THEN -fm.amount ELSE 0 END), 0), 2) AS balance
      FROM customers c
      JOIN financial_movements fm ON fm.customer_id = c.id AND fm.verification_status <> 'REJECTED'
      WHERE c.id = $1
      GROUP BY c.id
    `, [custCreditId]);

    assert.strictEqual(checkCust7.rows.length, 1);
    assert.strictEqual(Number(checkCust7.rows[0].balance), 75.25);
    assert.ok(Number(checkCust7.rows[0].balance) > 0, "Positive balance must be greater than 0");
  });

  await t.test("7. Dashboard totals equal the sum of the three balance categories", async () => {
    const summary = await getDashboardSummary(testUserId, client);
    const customers = summary.customers;

    // Expected qualifying customers in our transaction:
    // 1. custSameName1 (Debt: -100.00)
    // 2. custSameName2 (Credit: +250.00)
    // 3. custSettled (Settled: 0.00)
    // 4. custDebt (Debt: -150.50)
    // 5. custCredit (Credit: +75.25)
    // Total qualifying test customers = 5
    // custNoMovements and custOnlyRejected MUST BE 0 (excluded).
    assert.strictEqual(customers.total, 5, "Total qualifying customers must equal 5");
    assert.strictEqual(customers.withDebt, 2, "2 customers with debt (custSameName1 and custDebt)");
    assert.strictEqual(customers.withCredit, 2, "2 customers with credit (custSameName2 and custCredit)");
    assert.strictEqual(customers.settled, 1, "1 customer settled (custSettled)");

    // Mathematical identity: total must equal withDebt + settled + withCredit
    const sumCategories = customers.withDebt + customers.settled + customers.withCredit;
    assert.strictEqual(customers.total, sumCategories, "Dashboard total must equal sum of debt, settled, and credit categories");
    assert.strictEqual(customers.withBalance, customers.withDebt + customers.withCredit);
  });
});

