const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_uid_1";
const ADMIN_EMAIL = "m95169829@gmail.com";
const CUSTOMER_A_UID = "cust_alice";
const CUSTOMER_B_UID = "cust_bob";
const MERCHANT_UID = "merch_yassine";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function tsPast() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

async function seedUsers() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = tsPast();
    await db.collection("users").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      fullName: "TwinCash Admin",
      email: ADMIN_EMAIL,
      whatsapp: "0670533324",
      role: "admin",
      status: "active",
      balance: 0,
      loyaltyCount: 0,
      commissionType: "percentage",
      commissionValue: 0,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(CUSTOMER_A_UID).set({
      uid: CUSTOMER_A_UID,
      fullName: "Alice Customer",
      email: "alice@gmail.com",
      whatsapp: "0611111111",
      role: "customer",
      status: "active",
      balance: 200,
      loyaltyCount: 2,
      commissionType: "percentage",
      commissionValue: 0,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(CUSTOMER_B_UID).set({
      uid: CUSTOMER_B_UID,
      fullName: "Bob Customer",
      email: "bob@gmail.com",
      whatsapp: "0622222222",
      role: "customer",
      status: "active",
      balance: 100,
      loyaltyCount: 0,
      commissionType: "percentage",
      commissionValue: 0,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(MERCHANT_UID).set({
      uid: MERCHANT_UID,
      fullName: "Yassine Merchant",
      email: "merchant@gmail.com",
      whatsapp: "0633333333",
      role: "merchant",
      status: "active",
      balance: 0,
      loyaltyCount: 0,
      commissionType: "percentage",
      commissionValue: 20,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("whatsapp_index").doc("0611111111").set({
      uid: CUSTOMER_A_UID,
      createdAt: now,
    });
  });
}

test("Unauthenticated users: denied across all collections", async () => {
  await seedUsers();
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(CUSTOMER_A_UID).get());
  await assertFails(unauth.collection("services").get());
  await assertFails(unauth.collection("orders").get());
  await assertFails(unauth.collection("settings").doc("global").get());
});

test("Email spoofing test: unverified admin email rejected", async () => {
  const spoofAdmin = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const now = tsPast();

  await assertFails(
    spoofAdmin.collection("settings").doc("global").set({
      agencyPhone: "0670533324",
      whatsappNumber: "212670533324",
      loyaltyThreshold: 4,
      loyaltyDiscountPercent: 25,
      adminEmail: ADMIN_EMAIL,
      agencyBalance: 0,
      updatedAt: now,
    })
  );
  await assertFails(spoofAdmin.collection("users").get());
});

test("Admin registration & customer registration with unique WhatsApp enforcement", async () => {
  const now = tsPast();
  const adminDb = adminContext().firestore();

  // Admin creates own doc + whatsapp_index in batch
  const adminBatch = adminDb.batch();
  adminBatch.set(adminDb.collection("whatsapp_index").doc("0670533324"), {
    uid: ADMIN_UID,
    createdAt: now,
  });
  adminBatch.set(adminDb.collection("users").doc(ADMIN_UID), {
    uid: ADMIN_UID,
    fullName: "TwinCash Owner",
    email: ADMIN_EMAIL,
    whatsapp: "0670533324",
    role: "admin",
    status: "active",
    balance: 0,
    loyaltyCount: 0,
    commissionType: "percentage",
    commissionValue: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(adminBatch.commit());

  // Customer cannot self-assign admin role
  const bobDb = testEnv.authenticatedContext(CUSTOMER_B_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();
  const badBatch = bobDb.batch();
  badBatch.set(bobDb.collection("whatsapp_index").doc("0699999999"), {
    uid: CUSTOMER_B_UID,
    createdAt: now,
  });
  badBatch.set(bobDb.collection("users").doc(CUSTOMER_B_UID), {
    uid: CUSTOMER_B_UID,
    fullName: "Bob Escalator",
    email: "bob@gmail.com",
    whatsapp: "0699999999",
    role: "admin",
    status: "active",
    balance: 0,
    loyaltyCount: 0,
    commissionType: "percentage",
    commissionValue: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertFails(badBatch.commit());

  // Customer cannot register with duplicate WhatsApp (0670533324 already taken)
  const dupPhoneBatch = bobDb.batch();
  dupPhoneBatch.set(bobDb.collection("whatsapp_index").doc("0670533324"), {
    uid: CUSTOMER_B_UID,
    createdAt: now,
  });
  dupPhoneBatch.set(bobDb.collection("users").doc(CUSTOMER_B_UID), {
    uid: CUSTOMER_B_UID,
    fullName: "Bob Dup",
    email: "bob@gmail.com",
    whatsapp: "0670533324",
    role: "customer",
    status: "active",
    balance: 0,
    loyaltyCount: 0,
    commissionType: "percentage",
    commissionValue: 0,
    createdAt: now,
    updatedAt: now,
  });
  await assertFails(dupPhoneBatch.commit());
});

test("User PII & privilege protections: cross-user read, balance inflation, ghost fields fail", async () => {
  await seedUsers();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_A_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();

  // Cross-user read & collection list denied
  await assertFails(aliceDb.collection("users").doc(CUSTOMER_B_UID).get());
  await assertFails(aliceDb.collection("users").get());

  // Safe profile edit succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).update({
      fullName: "Alice Updated",
      updatedAt: tsPast(),
    })
  );

  // Self-inflating balance fails
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).update({
      balance: 9999,
      updatedAt: tsPast(),
    })
  );

  // Shadow/ghost field update fails
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_A_UID).update({
      fullName: "Alice",
      isVerified: true,
      updatedAt: tsPast(),
    })
  );
});

test("Supplier cost isolation: /service_costs readable/writable ONLY by Admin", async () => {
  await seedUsers();
  const adminDb = adminContext().firestore();
  const merchDb = testEnv.authenticatedContext(MERCHANT_UID, {
    email: "merchant@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_A_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();
  const now = tsPast();

  await assertSucceeds(
    adminDb.collection("services").doc("srv_freefire").set({
      category: "FREE_FIRE",
      name: "شحن Free Fire",
      description: "شحن مباشر",
      packages: [{ id: "pkg_1", label: "100 جوهرة", quantityLabel: "100", sellPrice: 12 }],
      available: true,
      displayOrder: 1,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("service_costs").doc("srv_freefire").set({
      costs: { pkg_1: 8.5 },
      updatedAt: now,
    })
  );

  await assertSucceeds(merchDb.collection("services").doc("srv_freefire").get());
  await assertSucceeds(aliceDb.collection("services").doc("srv_freefire").get());
  await assertFails(merchDb.collection("service_costs").doc("srv_freefire").get());
  await assertFails(aliceDb.collection("service_costs").doc("srv_freefire").get());
});

test("Orders lifecycle & query alignment: customer creates pending, merchant processes, unfiltered list fails", async () => {
  await seedUsers();
  const now = tsPast();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_A_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();
  const merchDb = testEnv.authenticatedContext(MERCHANT_UID, {
    email: "merchant@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("orders").doc("ORD-1001").set({
      customerId: CUSTOMER_A_UID,
      customerName: "Alice Customer",
      customerWhatsapp: "0611111111",
      serviceId: "srv_freefire",
      serviceName: "شحن Free Fire",
      serviceCategory: "FREE_FIRE",
      packageId: "pkg_1",
      packageLabel: "100 جوهرة",
      playerAccountInfo: "123456789",
      note: "",
      offerOrder: false,
      originalPrice: 12,
      discountApplied: 0,
      finalPrice: 12,
      merchantCommission: 0,
      status: "pending",
      customerNotified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Scoped customer query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("orders").where("customerId", "==", CUSTOMER_A_UID).get());
  await assertFails(aliceDb.collection("orders").get());

  // Merchant pending query succeeds and completes order
  await assertSucceeds(merchDb.collection("orders").where("status", "==", "pending").get());
  await assertSucceeds(
    merchDb.collection("orders").doc("ORD-1001").update({
      merchantId: MERCHANT_UID,
      merchantName: "Yassine Merchant",
      merchantCommission: 2.4,
      status: "completed",
      customerNotified: false,
      updatedAt: tsPast(),
    })
  );

  // Merchant cannot re-process terminal completed order
  await assertFails(
    merchDb.collection("orders").doc("ORD-1001").update({
      merchantId: MERCHANT_UID,
      merchantName: "Yassine Merchant",
      merchantCommission: 99,
      status: "rejected",
      customerNotified: false,
      updatedAt: tsPast(),
    })
  );
});

test("Discount codes scoped queries & contest single entry", async () => {
  await seedUsers();
  const now = tsPast();
  const adminDb = adminContext().firestore();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_A_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("discountCodes").doc("dc_1").set({
      code: "TWIN10",
      discountType: "percentage",
      discountValue: 10,
      includedCategories: ["ALL"],
      expiresAt: Date.now() + 86400000,
      maxUses: 50,
      usedCount: 0,
      publicCode: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("discountCodes").doc("dc_bob").set({
      code: "BOB20",
      discountType: "fixed",
      discountValue: 20,
      includedCategories: ["FREE_FIRE"],
      expiresAt: Date.now() + 86400000,
      maxUses: 1,
      usedCount: 0,
      publicCode: false,
      targetCustomerId: CUSTOMER_B_UID,
      targetCustomerName: "Bob Customer",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Scoped customer queries succeed; unfiltered fails; reading Bob's private code fails
  await assertSucceeds(aliceDb.collection("discountCodes").where("publicCode", "==", true).get());
  await assertSucceeds(aliceDb.collection("discountCodes").where("targetCustomerId", "==", CUSTOMER_A_UID).get());
  await assertFails(aliceDb.collection("discountCodes").get());
  await assertFails(aliceDb.collection("discountCodes").doc("dc_bob").get());

  // Contest single entry enforcement
  await assertSucceeds(
    adminDb.collection("contests").doc("cnt_1").set({
      title: "سحب أسبوعي",
      description: "جائزة شحن",
      prizeType: "balance",
      prizeLabel: "50 د.م",
      prizeAmount: 50,
      rulesText: "مشاركة واحدة",
      startsAt: Date.now() - 1000,
      endsAt: Date.now() + 86400000,
      status: "active",
      prizeApproved: false,
      winnerNotified: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("contestEntries").doc(`cnt_1_${CUSTOMER_A_UID}`).set({
      contestId: "cnt_1",
      customerId: CUSTOMER_A_UID,
      customerName: "Alice Customer",
      customerWhatsapp: "0611111111",
      createdAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("contestEntries").doc("random_id").set({
      contestId: "cnt_1",
      customerId: CUSTOMER_A_UID,
      customerName: "Alice Customer",
      customerWhatsapp: "0611111111",
      createdAt: now,
    })
  );
});

test("Custom claim admin=true protects service_costs & merchant order netProfit + code usedCount decrement", async () => {
  await seedUsers();
  const now = tsPast();
  const claimAdminDb = testEnv.authenticatedContext("claim_admin_uid", {
    email: "claimadmin@twincash.ma",
    email_verified: true,
    admin: true,
  }).firestore();
  const merchDb = testEnv.authenticatedContext(MERCHANT_UID, {
    email: "merchant@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_A_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    claimAdminDb.collection("service_costs").doc("srv_pubg").set({
      costs: { pkg_60: 7.5 },
      updatedAt: now,
    })
  );
  await assertSucceeds(claimAdminDb.collection("service_costs").doc("srv_pubg").get());
  await assertFails(merchDb.collection("service_costs").doc("srv_pubg").get());
  await assertFails(aliceDb.collection("service_costs").doc("srv_pubg").get());

  // Seed discount code with usedCount = 1
  await assertSucceeds(
    claimAdminDb.collection("discountCodes").doc("dc_used").set({
      code: "DEC10",
      discountType: "percentage",
      discountValue: 10,
      includedCategories: ["ALL"],
      expiresAt: Date.now() + 86400000,
      maxUses: 10,
      usedCount: 1,
      publicCode: true,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer creates order with supplierCost & netProfit=0
  await assertSucceeds(
    aliceDb.collection("orders").doc("ORD-2001").set({
      customerId: CUSTOMER_A_UID,
      customerName: "Alice Customer",
      customerWhatsapp: "0611111111",
      serviceId: "srv_pubg",
      serviceName: "PUBG UC",
      serviceCategory: "PUBG",
      packageId: "pkg_60",
      packageLabel: "60 UC",
      playerAccountInfo: "555111222",
      note: "",
      offerOrder: false,
      discountCodeUsed: "DEC10",
      originalPrice: 12,
      discountApplied: 1.2,
      finalPrice: 10.8,
      supplierCost: 7.5,
      merchantCommission: 0,
      netProfit: 0,
      status: "pending",
      customerNotified: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Merchant rejects order and decrements usedCount by 1
  await assertSucceeds(
    merchDb.collection("orders").doc("ORD-2001").update({
      merchantId: MERCHANT_UID,
      merchantName: "Yassine Merchant",
      status: "rejected",
      rejectionReason: "خطأ بالمعرف",
      customerNotified: false,
      updatedAt: tsPast(),
    })
  );
  await assertSucceeds(
    merchDb.collection("discountCodes").doc("dc_used").update({
      usedCount: 0,
      updatedAt: tsPast(),
    })
  );

  // Merchant cannot decrement usedCount below 0
  await assertFails(
    merchDb.collection("discountCodes").doc("dc_used").update({
      usedCount: -1,
      updatedAt: tsPast(),
    })
  );
});

