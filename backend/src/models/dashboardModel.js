const pool = require("../config/db");

async function getDashboardSummary(userId) {
  const customerCountRes = await pool.query(`
    SELECT COUNT(DISTINCT LOWER(TRIM(full_name))) AS total
    FROM customers
  `);
  const customerCount = customerCountRes.rows[0] || { total: 0 };

  const totalOrdersRes = await pool.query(`
    SELECT COUNT(*) AS total
    FROM orders
  `);
  const totalOrders = totalOrdersRes.rows[0] || { total: 0 };

  const totalNotesRes = await pool.query(`
    SELECT COUNT(*) AS total
    FROM business_notes
  `);
  const totalNotes = totalNotesRes.rows[0] || { total: 0 };

  const pendingFinancialMovementsRes = await pool.query(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE verification_status = 'PENDING'
  `);
  const pendingFinancialMovements = pendingFinancialMovementsRes.rows[0] || { total: 0 };

  const financialSummaryRes = await pool.query(`
    SELECT
      ROUND(
        COALESCE(
          SUM(
            CASE
              WHEN movement_type = 'DEBIT'
                AND verification_status <> 'REJECTED'
              THEN amount
              ELSE 0
            END
          ),
          0
        ),
        2
      ) AS total_debit,

      ROUND(
        COALESCE(
          SUM(
            CASE
              WHEN movement_type = 'CREDIT'
                AND verification_status <> 'REJECTED'
              THEN amount
              ELSE 0
            END
          ),
          0
        ),
        2
      ) AS total_credit
    FROM financial_movements
  `);
  const financialSummary = financialSummaryRes.rows[0] || { total_debit: 0, total_credit: 0 };

  const customerBalanceSummaryRes = await pool.query(`
    SELECT
      SUM(
        CASE
          WHEN balance < 0 THEN 1
          ELSE 0
        END
      ) AS customers_with_debt,

      SUM(
        CASE
          WHEN balance = 0 THEN 1
          ELSE 0
        END
      ) AS settled_customers,

      SUM(
        CASE
          WHEN balance > 0 THEN 1
          ELSE 0
        END
      ) AS customers_with_credit
    FROM (
      SELECT
        LOWER(TRIM(c.full_name)) AS normalized_name,
        COALESCE(
          SUM(
            CASE
              WHEN fm.movement_type = 'CREDIT'
                AND fm.verification_status <> 'REJECTED'
              THEN fm.amount

              WHEN fm.movement_type = 'DEBIT'
                AND fm.verification_status <> 'REJECTED'
              THEN -fm.amount

              ELSE 0
            END
          ),
          0
        ) AS balance
      FROM customers c
      LEFT JOIN financial_movements fm
        ON fm.customer_id = c.id
      GROUP BY LOWER(TRIM(c.full_name))
    ) AS customer_balances
  `);
  const customerBalanceSummary = customerBalanceSummaryRes.rows[0] || {};

  const orderStatusRes = await pool.query(`
    SELECT order_status, COUNT(*) AS total
    FROM orders
    GROUP BY order_status
  `);

  const orderStatusBreakdown = {
    PENDING: 0,
    CONFIRMED: 0,
    PROCESSING: 0,
    READY: 0,
    COMPLETED: 0,
    CANCELLED: 0,
  };
  orderStatusRes.rows.forEach((row) => {
    if (row.order_status) {
      orderStatusBreakdown[row.order_status] = Number(row.total);
    }
  });

  const recentOrdersRes = await pool.query(`
    SELECT COUNT(*) AS total
    FROM orders
    WHERE created_at >= NOW() - INTERVAL '7 days'
  `);
  const recentOrders = recentOrdersRes.rows[0] || { total: 0 };

  const recentPaymentsRes = await pool.query(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE movement_type = 'CREDIT'
      AND created_at >= NOW() - INTERVAL '7 days'
  `);
  const recentPayments = recentPaymentsRes.rows[0] || { total: 0 };

  const unreadNotificationsRes = await pool.query(
    `
      SELECT COUNT(*) AS total
      FROM notifications
      WHERE recipient_id = $1
        AND is_read = FALSE
    `,
    [userId]
  );
  const unreadNotifications = unreadNotificationsRes.rows[0] || { total: 0 };

  const recentActivitiesRes = await pool.query(`
    SELECT
      a.id,
      a.actor_id,
      fm.full_name AS actor_name,
      a.activity_type,
      a.entity_type,
      a.entity_id,
      a.description,
      a.created_at,
      customer.full_name AS customer_name
    FROM activities a
    JOIN family_members fm
      ON fm.id = a.actor_id
    LEFT JOIN customers customer
      ON a.entity_type = 'CUSTOMER'
      AND a.entity_id = customer.id
    ORDER BY a.created_at DESC, a.id DESC
    LIMIT 10
  `);

  const recentOrdersListRes = await pool.query(`
    SELECT
      o.id,
      o.customer_id,
      c.full_name AS customer_name,
      o.title,
      o.order_status,
      o.created_at
    FROM orders o
    LEFT JOIN customers c
      ON c.id = o.customer_id
    ORDER BY o.created_at DESC, o.id DESC
    LIMIT 10
  `);

  const recentNotesListRes = await pool.query(`
    SELECT
      bn.id,
      bn.note_text,
      bn.created_by,
      fm.full_name AS created_by_name,
      bn.created_at,
      bn.updated_at
    FROM business_notes bn
    JOIN family_members fm
      ON fm.id = bn.created_by
    ORDER BY bn.updated_at DESC, bn.id DESC
    LIMIT 8
  `);

  const myActivityCountRes = await pool.query(
    `
      SELECT COUNT(*) AS total
      FROM activities
      WHERE actor_id = $1
    `,
    [userId]
  );
  const myActivityCount = myActivityCountRes.rows[0] || { total: 0 };

  const totalDebit = Number(Number(financialSummary.total_debit || 0).toFixed(2));
  const totalCredit = Number(Number(financialSummary.total_credit || 0).toFixed(2));
  const netBalance = Number((totalCredit - totalDebit).toFixed(2));

  return {
    myActivity: {
      total: Number(myActivityCount.total || 0),
    },
    customers: {
      total: Number(customerCount.total || 0),
      withDebt: Number(customerBalanceSummary.customers_with_debt || 0),
      withBalance: Number(customerBalanceSummary.customers_with_debt || 0) + Number(customerBalanceSummary.customers_with_credit || 0),
      settled: Number(customerBalanceSummary.settled_customers || 0),
      withCredit: Number(customerBalanceSummary.customers_with_credit || 0),
    },
    orders: {
      total: Number(totalOrders.total || 0),
      recent: Number(recentOrders.total || 0),
      statusBreakdown: orderStatusBreakdown,
    },
    notes: {
      total: Number(totalNotes.total || 0),
    },
    financialMovements: {
      pending: Number(pendingFinancialMovements.total || 0),
      recentPayments: Number(recentPayments.total || 0),
      totalDebit,
      totalCredit,
      balance: netBalance,
    },
    notifications: {
      unread: Number(unreadNotifications.total || 0),
    },
    recentOrders: recentOrdersListRes.rows,
    recentNotes: recentNotesListRes.rows,
    activities: {
      recent: recentActivitiesRes.rows,
      recentOrders: recentOrdersListRes.rows,
      recentNotes: recentNotesListRes.rows,
    },
  };
}

module.exports = {
  getDashboardSummary,
};