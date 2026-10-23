const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");

const PROJECT_ID = "sadhana-card-test";

describe("Sadhana Card Firestore Security Rules", () => {
  let testEnv;

  before(async () => {
    const rulesPath = path.resolve(__dirname, "../../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: rules,
        host: "127.0.0.1",
        port: 8080,
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

  // 1. Unauthenticated Access
  describe("Unauthenticated Access", () => {
    it("denies unauthenticated read to user profile", async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(unauthDb.collection("users").doc("devotee_1").get());
    });

    it("denies unauthenticated read to weekly summaries", async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(
        unauthDb
          .collection("users")
          .doc("devotee_1")
          .collection("weekly_summaries")
          .doc("2026-10-18")
          .get()
      );
    });

    it("denies unauthenticated read to counselors collection", async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(
        unauthDb.collection("counselors").doc("counselor_1").get()
      );
    });
  });

  // 2. Devotee Self-Access & Multi-Devotee Isolation
  describe("Devotee Account Isolation", () => {
    it("allows devotee to read and create their own profile", async () => {
      const aliceDb = testEnv
        .authenticatedContext("alice_uid", { email: "alice@test.com" })
        .firestore();
      await assertSucceeds(
        aliceDb.collection("users").doc("alice_uid").set({
          uid: "alice_uid",
          displayName: "Alice",
          email: "alice@test.com",
          role: "devotee",
          updatedAt: Date.now(),
        })
      );
      await assertSucceeds(
        aliceDb.collection("users").doc("alice_uid").get()
      );
    });

    it("denies devotee from reading another devotee profile", async () => {
      // Seed Bob's profile using admin context
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("users").doc("bob_uid").set({
          uid: "bob_uid",
          displayName: "Bob",
          email: "bob@test.com",
          role: "devotee",
          updatedAt: Date.now(),
        });
      });

      const aliceDb = testEnv
        .authenticatedContext("alice_uid")
        .firestore();
      await assertFails(aliceDb.collection("users").doc("bob_uid").get());
    });

    it("denies devotee from modifying another devotee data", async () => {
      const aliceDb = testEnv
        .authenticatedContext("alice_uid")
        .firestore();
      await assertFails(
        aliceDb.collection("users").doc("bob_uid").set({
          uid: "bob_uid",
          displayName: "Hacked",
          role: "devotee",
        })
      );
    });
  });

  // 3. Self-Promotion & Counselor Role Tampering
  describe("Privilege Escalation & Counselor Protection", () => {
    it("denies devotee from setting role to counselor on creation", async () => {
      const eveDb = testEnv
        .authenticatedContext("eve_uid")
        .firestore();
      await assertFails(
        eveDb.collection("users").doc("eve_uid").set({
          uid: "eve_uid",
          displayName: "Eve",
          role: "counselor",
          updatedAt: Date.now(),
        })
      );
    });

    it("denies devotee from mutating role from devotee to counselor on update", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("users").doc("eve_uid").set({
          uid: "eve_uid",
          displayName: "Eve",
          role: "devotee",
          updatedAt: Date.now(),
        });
      });

      const eveDb = testEnv
        .authenticatedContext("eve_uid")
        .firestore();
      await assertFails(
        eveDb.collection("users").doc("eve_uid").update({
          role: "counselor",
        })
      );
    });

    it("denies any client from writing to /counselors collection", async () => {
      const eveDb = testEnv
        .authenticatedContext("eve_uid")
        .firestore();
      await assertFails(
        eveDb.collection("counselors").doc("eve_uid").set({
          approved: true,
        })
      );
    });

    it("unapproved counselor cannot read devotee directory", async () => {
      // Eve is not in /counselors with approved: true
      const eveDb = testEnv
        .authenticatedContext("eve_uid")
        .firestore();
      await assertFails(
        eveDb.collection("users").doc("alice_uid").get()
      );
    });

    it("approved counselor can inspect devotee profiles", async () => {
      // Seed approved counselor doc
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("counselors").doc("counselor_guru").set({
          approved: true,
        });
        await context.firestore().collection("users").doc("alice_uid").set({
          uid: "alice_uid",
          displayName: "Alice",
          role: "devotee",
          updatedAt: Date.now(),
        });
      });

      const guruDb = testEnv
        .authenticatedContext("counselor_guru")
        .firestore();
      await assertSucceeds(
        guruDb.collection("users").doc("alice_uid").get()
      );
    });
  });

  // 4. Weekly Summary Score Bounds & Validation
  describe("Weekly Summary Validation", () => {
    it("allows devotee to upload valid weekly summary (0-1400)", async () => {
      const aliceDb = testEnv
        .authenticatedContext("alice_uid")
        .firestore();
      await assertSucceeds(
        aliceDb
          .collection("users")
          .doc("alice_uid")
          .collection("weekly_summaries")
          .doc("2026-10-18")
          .set({
            startOfWeek: "2026-10-18",
            devoteeName: "Alice",
            weekTotalScore: 1250,
            percentage: 89,
            totalPathanMinutes: 120,
            totalSravanMinutes: 180,
            totalSevaMinutes: 60,
            updatedAt: Date.now(),
          })
      );
    });

    it("denies weekly summary exceeding 1400 max score", async () => {
      const aliceDb = testEnv
        .authenticatedContext("alice_uid")
        .firestore();
      await assertFails(
        aliceDb
          .collection("users")
          .doc("alice_uid")
          .collection("weekly_summaries")
          .doc("2026-10-18")
          .set({
            startOfWeek: "2026-10-18",
            weekTotalScore: 1401, // > 1400 is invalid
            percentage: 100,
            updatedAt: Date.now(),
          })
      );
    });

    it("denies weekly summary with negative score", async () => {
      const aliceDb = testEnv
        .authenticatedContext("alice_uid")
        .firestore();
      await assertFails(
        aliceDb
          .collection("users")
          .doc("alice_uid")
          .collection("weekly_summaries")
          .doc("2026-10-18")
          .set({
            startOfWeek: "2026-10-18",
            weekTotalScore: -5, // Negative invalid
            percentage: 0,
            updatedAt: Date.now(),
          })
      );
    });

    it("denies devotee from writing another devotee weekly summary", async () => {
      const aliceDb = testEnv
        .authenticatedContext("alice_uid")
        .firestore();
      await assertFails(
        aliceDb
          .collection("users")
          .doc("bob_uid")
          .collection("weekly_summaries")
          .doc("2026-10-18")
          .set({
            startOfWeek: "2026-10-18",
            weekTotalScore: 1000,
            percentage: 71,
          })
      );
    });
  });
});
