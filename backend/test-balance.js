const { getCustomerBalance } = require("./src/models/financialMovementModel");

async function test() {
  try {
    const balance = await getCustomerBalance(1);

    console.log("Customer Balance:");
    console.log(balance);
  } catch (error) {
    console.error("Balance test failed:", error.message);
  }

  process.exit();
}

test();