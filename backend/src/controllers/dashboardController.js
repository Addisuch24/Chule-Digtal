 const dashboardModel = require("../models/dashboardModel");

async function getDashboardSummary(req, res) {
  try {
    const summary = await dashboardModel.getDashboardSummary(
      req.user.id
    );

    const validatedSummary = {
      customers: {
        total: Number(summary.customers?.total || 0),
        withBalance: Number(
          summary.customers?.withBalance || 0
        ),
        settled: Number(
          summary.customers?.settled || 0
        ),
        withCredit: Number(
          summary.customers?.withCredit || 0
        ),
      },

      myActivity: {
        total: Number(summary.myActivity?.total || 0),
      },

      pendingVerification: {
        orders: Number(
          summary.pendingVerification?.orders || 0
        ),
        financialMovements: Number(
          summary.pendingVerification?.financialMovements || 0
        ),
        total: Number(
          summary.pendingVerification?.total || 0
        ),
      },

      orders: {
        pending: Number(summary.orders?.pending || 0),
        completed: Number(summary.orders?.completed || 0),
        recent: Number(summary.orders?.recent || 0),
      },

      financialMovements: {
        pending: Number(
          summary.financialMovements?.pending || 0
        ),
        recentPayments: Number(
          summary.financialMovements?.recentPayments || 0
        ),
        totalDebit: Number(
          summary.financialMovements?.totalDebit || 0
        ),
        totalCredit: Number(
          summary.financialMovements?.totalCredit || 0
        ),
        balance: Number(
          summary.financialMovements?.balance || 0
        ),
      },

      notifications: {
        unread: Number(
          summary.notifications?.unread || 0
        ),
      },

      recentActivities: Array.isArray(
        summary.recentActivities || summary.activities?.recent
      )
        ? summary.recentActivities || summary.activities.recent
        : [],

      recentOrders: Array.isArray(
        summary.recentOrders || summary.activities?.recentOrders
      )
        ? summary.recentOrders || summary.activities.recentOrders
        : [],
    };

    return res.status(200).json({
      success: true,
      data: validatedSummary,
    });
  } catch (error) {
    console.error("Get dashboard summary error:", error);

    return res.status(500).json({
      success: false,
      message: "Failed to load dashboard summary",
    });
  }
}

module.exports = {
  getDashboardSummary,
};