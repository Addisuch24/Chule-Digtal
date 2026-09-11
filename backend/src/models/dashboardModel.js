 const pool = require("../config/db");

async function getDashboardSummary(userId) {
  const [[customerCount]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM customers
  `);

  const [[pendingOrders]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM orders
    WHERE order_status = 'PENDING'
  `);

  const [[completedOrders]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM orders
    WHERE order_status = 'COMPLETED'
  `);

  const [[pendingFinancialMovements]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE verification_status = 'PENDING'
  `);
  const [[financialSummary]] = await pool.execute(`
  SELECT
    COALESCE(
      SUM(
        CASE
          WHEN movement_type = 'DEBIT'
            AND verification_status = 'VERIFIED'
          THEN amount
          ELSE 0
        END
      ),
      0
    ) AS total_debit,

    COALESCE(
      SUM(
        CASE
          WHEN movement_type = 'CREDIT'
            AND verification_status = 'VERIFIED'
          THEN amount
          ELSE 0
        END
      ),
      0
    ) AS total_credit
  FROM financial_movements
`);
const [[customerBalanceSummary]] = await pool.execute(`
  SELECT
    SUM(
      CASE
        WHEN balance > 0 THEN 1
        ELSE 0
      END
    ) AS customers_with_balance,

    SUM(
      CASE
        WHEN balance = 0 THEN 1
        ELSE 0
      END
    ) AS settled_customers,

    SUM(
      CASE
        WHEN balance < 0 THEN 1
        ELSE 0
      END
    ) AS customers_with_credit
  FROM (
    SELECT
      c.id,
      COALESCE(
        SUM(
          CASE
            WHEN fm.movement_type = 'DEBIT'
              AND fm.verification_status = 'VERIFIED'
            THEN fm.amount

            WHEN fm.movement_type = 'CREDIT'
              AND fm.verification_status = 'VERIFIED'
            THEN -fm.amount

            ELSE 0
          END
        ),
        0
      ) AS balance
    FROM customers c
    LEFT JOIN financial_movements fm
      ON fm.customer_id = c.id
    GROUP BY c.id
  ) AS customer_balances
`);

  const [[recentOrders]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM orders
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  `);

  const [[recentPayments]] = await pool.execute(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE movement_type = 'CREDIT'
      AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  `);

  const [[unreadNotifications]] = await pool.execute(
    `
      SELECT COUNT(*) AS total
      FROM notifications
      WHERE recipient_id = ?
        AND is_read = FALSE
    `,
    [userId]
  );
const [recentActivities] = await pool.execute(`
  SELECT
    a.id,
    a.actor_id,
    fm.full_name AS actor_name,
    a.activity_type,
    a.entity_type,
    a.entity_id,
    a.description,
    a.created_at
  FROM activities a
  JOIN family_members fm
    ON fm.id = a.actor_id
  ORDER BY a.created_at DESC, a.id DESC
  LIMIT 10
`);
const [recentOrdersList] = await pool.execute(`
  SELECT
    o.id,
    o.customer_id,
    c.full_name AS customer_name,
    o.order_status,
    o.created_at
  FROM orders o
  JOIN customers c
    ON c.id = o.customer_id
  ORDER BY o.created_at DESC, o.id DESC
  LIMIT 10
`);
const [[myActivityCount]] = await pool.execute(`
  SELECT COUNT(*) AS total
  FROM activities
  WHERE actor_id = ?
`, [userId]);
  return {
    pendingVerification: {
  orders: Number(pendingOrders.total),
  financialMovements: Number(
    pendingFinancialMovements.total
  ),
  total:
    Number(pendingOrders.total) +
    Number(pendingFinancialMovements.total),
},
    myActivity: {
  total: Number(myActivityCount.total),
},
    customers: {
  total: Number(customerCount.total),
  withBalance: Number(
    customerBalanceSummary.customers_with_balance || 0
  ),
  settled: Number(
    customerBalanceSummary.settled_customers || 0
  ),
  withCredit: Number(
    customerBalanceSummary.customers_with_credit || 0
  ),
},
    orders: {
      pending: Number(pendingOrders.total),
      completed: Number(completedOrders.total),
      recent: Number(recentOrders.total),
    },
    financialMovements: {
      pending: Number(pendingFinancialMovements.total),
      recentPayments: Number(recentPayments.total),
      summary: financialSummary[0],
    },
    notifications: {
      unread: Number(unreadNotifications.total),
    },
    activities: {
      recent: recentActivities,
      recentOrders: recentOrdersList,
    },
    financialMovements: {
  pending: Number(pendingFinancialMovements.total),
  recentPayments: Number(recentPayments.total),
  totalDebit: Number(financialSummary.total_debit),
  totalCredit: Number(financialSummary.total_credit),
  balance:
    Number(financialSummary.total_debit) -
    Number(financialSummary.total_credit),
},
  };
  
}

module.exports = {
  getDashboardSummary,
};