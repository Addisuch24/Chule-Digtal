const pool = require("../config/db");

async function getDashboardSummary(userId, db = pool) {
  const customerSummaryRes = await db.query(`
    SELECT
      COUNT(*) AS total,
      COALESCE(
        SUM(
          CASE
            WHEN balance < 0 THEN 1
            ELSE 0
          END
        ),
        0
      ) AS customers_with_debt,

      COALESCE(
        SUM(
          CASE
            WHEN balance = 0 THEN 1
            ELSE 0
          END
        ),
        0
      ) AS settled_customers,

      COALESCE(
        SUM(
          CASE
            WHEN balance > 0 THEN 1
            ELSE 0
          END
        ),
        0
      ) AS customers_with_credit
    FROM (
      SELECT
        c.id,
        ROUND(
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
          ),
          2
        ) AS balance
      FROM customers c
      JOIN financial_movements fm
        ON fm.customer_id = c.id
       AND fm.verification_status <> 'REJECTED'
      GROUP BY c.id
    ) AS customer_balances
  `);
  const customerSummary = customerSummaryRes.rows[0] || {};

  const totalOrdersRes = await db.query(`
    SELECT COUNT(*) AS total
    FROM orders
  `);
  const totalOrders = totalOrdersRes.rows[0] || { total: 0 };

  const totalNotesRes = await db.query(`
    SELECT COUNT(*) AS total
    FROM business_notes
  `);
  const totalNotes = totalNotesRes.rows[0] || { total: 0 };

  const pendingFinancialMovementsRes = await db.query(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE verification_status = 'PENDING'
  `);
  const pendingFinancialMovements = pendingFinancialMovementsRes.rows[0] || { total: 0 };

  const financialSummaryRes = await db.query(`
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


  const orderStatusRes = await db.query(`
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

  const recentOrdersRes = await db.query(`
    SELECT COUNT(*) AS total
    FROM orders
    WHERE created_at >= NOW() - INTERVAL '7 days'
  `);
  const recentOrders = recentOrdersRes.rows[0] || { total: 0 };

  const recentPaymentsRes = await db.query(`
    SELECT COUNT(*) AS total
    FROM financial_movements
    WHERE movement_type = 'CREDIT'
      AND created_at >= NOW() - INTERVAL '7 days'
  `);
  const recentPayments = recentPaymentsRes.rows[0] || { total: 0 };

  const unreadNotificationsRes = await db.query(
    `
      SELECT COUNT(*) AS total
      FROM notifications
      WHERE recipient_id = $1
        AND is_read = FALSE
    `,
    [userId]
  );
  const unreadNotifications = unreadNotificationsRes.rows[0] || { total: 0 };

  const recentActivitiesRes = await db.query(`
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

  const recentOrdersListRes = await db.query(`
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

  const recentNotesListRes = await db.query(`
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

  const myActivityCountRes = await db.query(
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
      total: Number(customerSummary.total || 0),
      withDebt: Number(customerSummary.customers_with_debt || 0),
      withBalance: Number(customerSummary.customers_with_debt || 0) + Number(customerSummary.customers_with_credit || 0),
      settled: Number(customerSummary.settled_customers || 0),
      withCredit: Number(customerSummary.customers_with_credit || 0),
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