const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read, list, create, or delete favorites", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const favRef = unauthDb.collection("users").doc(ALICE_UID).collection("favorites");

  await assertFails(favRef.get());
  await assertFails(favRef.doc("bitcoin").get());
  await assertFails(
    favRef.doc("bitcoin").set({
      userId: ALICE_UID,
      coinId: "bitcoin",
      symbol: "BTC",
      name: "Bitcoin",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(favRef.doc("bitcoin").delete());
});

test("Authenticated owner: can create, update, list, get, and delete own favorite coin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const favCollection = aliceDb.collection("users").doc(ALICE_UID).collection("favorites");
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    favCollection.doc("bitcoin").set({
      userId: ALICE_UID,
      coinId: "bitcoin",
      symbol: "BTC",
      name: "Bitcoin",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(favCollection.get());
  await assertSucceeds(favCollection.doc("bitcoin").get());

  await assertSucceeds(
    favCollection.doc("bitcoin").update({
      symbol: "BTC",
      name: "Bitcoin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(favCollection.doc("bitcoin").delete());
});

test("Authenticated owner: rejects shadow fields, @DocumentId 'id', future timestamps, and immutable field mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const favCollection = aliceDb.collection("users").doc(ALICE_UID).collection("favorites");
  const created = new Date(Date.now() - 10000);

  // Reject extra 'id' field when @DocumentId is used
  await assertFails(
    favCollection.doc("ethereum").set({
      id: "ethereum",
      userId: ALICE_UID,
      coinId: "ethereum",
      symbol: "ETH",
      name: "Ethereum",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject shadow field
  await assertFails(
    favCollection.doc("ethereum").set({
      userId: ALICE_UID,
      coinId: "ethereum",
      symbol: "ETH",
      name: "Ethereum",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    favCollection.doc("ethereum").set({
      userId: ALICE_UID,
      coinId: "ethereum",
      symbol: "ETH",
      name: "Ethereum",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Create valid doc then reject immutable createdAt / coinId update
  await assertSucceeds(
    favCollection.doc("ethereum").set({
      userId: ALICE_UID,
      coinId: "ethereum",
      symbol: "ETH",
      name: "Ethereum",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    favCollection.doc("ethereum").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    favCollection.doc("ethereum").update({
      coinId: "bitcoin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: user cannot read, list, create, update, or delete another user's favorites", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("favorites")
      .doc("solana")
      .set({
        userId: BOB_UID,
        coinId: "solana",
        symbol: "SOL",
        name: "Solana",
        createdAt: new Date(Date.now() - 10000),
        updatedAt: new Date(Date.now() - 10000),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobFavs = aliceDb.collection("users").doc(BOB_UID).collection("favorites");

  await assertFails(bobFavs.get());
  await assertFails(bobFavs.doc("solana").get());
  await assertFails(
    bobFavs.doc("cardano").set({
      userId: BOB_UID,
      coinId: "cardano",
      symbol: "ADA",
      name: "Cardano",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobFavs.doc("solana").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobFavs.doc("solana").delete());
});
