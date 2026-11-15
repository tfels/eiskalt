// Tests for the migration script (scripts/migrate-to-household.mjs) on the emulator.
// Run with "npm run test:rules" (starts the Firestore emulator, see package.json).
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, test } from 'node:test';
import { assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { deleteApp, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { migrateToHousehold } from '../scripts/migrate-to-household.mjs';

const OWNER = 'alice';
const MEMBER = 'dave';
const options = { householdId: 'home', householdName: 'Home', ownerId: OWNER, log: () => {} };

let testEnv;
let adminApp;
let db;

before(async () => {
  // "demo-" project: the emulator never touches the real project
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-eiskalt',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
  // the Admin SDK uses the emulator, as emulators:exec sets FIRESTORE_EMULATOR_HOST
  adminApp = initializeApp({ projectId: 'demo-eiskalt' }, 'migration-test');
  db = getFirestore(adminApp);
});

after(async () => {
  await deleteApp(adminApp);
  await testEnv.cleanup();
});

// every test starts with the same data in the old data model
beforeEach(async () => {
  await testEnv.clearFirestore();
  await db.doc(`members/${OWNER}`).set({ name: 'Alice', email: 'alice@example.com' });
  await db.doc(`members/${MEMBER}`).set({ email: 'dave@example.com' });   // without name
  await db.doc('lists/l1').set({ name: 'Freezer', icon: 'freezer' });
  await db.doc('lists/l1/items/i1').set({ name: 'Ice cream', quantity: 2, groupId: 'g1' });
  await db.doc('groups/g1').set({ name: 'Sweets' });
  await db.doc(`users/${OWNER}`).set({ lastViewedListId: 'l1' });
});

async function data(path) {
  return (await db.doc(path).get()).data();
}

test('dry run: writes nothing', async () => {
  const counts = await migrateToHousehold(db, options);
  assert.deepEqual(counts, { households: 1, members: 2, lists: 1, items: 1, groups: 1 });
  assert.equal((await db.doc('households/home').get()).exists, false);
});

test('copies the data with the same IDs, the old data stays', async () => {
  await migrateToHousehold(db, { ...options, write: true });

  assert.deepEqual(await data('households/home'), { name: 'Home', createdBy: OWNER, ownerId: OWNER });
  assert.deepEqual(await data(`households/home/members/${OWNER}`), { uid: OWNER, name: 'Alice' });
  assert.deepEqual(await data(`households/home/members/${MEMBER}`), { uid: MEMBER, name: MEMBER });
  assert.deepEqual(await data('households/home/lists/l1'), { name: 'Freezer', icon: 'freezer' });
  assert.deepEqual(await data('households/home/lists/l1/items/i1'), { name: 'Ice cream', quantity: 2, groupId: 'g1' });
  assert.deepEqual(await data('households/home/groups/g1'), { name: 'Sweets' });

  assert.deepEqual(await data('lists/l1'), { name: 'Freezer', icon: 'freezer' });
  assert.deepEqual(await data(`users/${OWNER}`), { lastViewedListId: 'l1' });
});

test('afterwards the members have access to the household through the rules', async () => {
  await migrateToHousehold(db, { ...options, write: true });
  const memberDb = testEnv.authenticatedContext(MEMBER).firestore();
  await assertSucceeds(memberDb.doc('households/home/lists/l1/items/i1').get());
});

test('second run: copies the data again, keeps the household and its members', async () => {
  await migrateToHousehold(db, { ...options, write: true });
  await db.doc('households/home').update({ name: 'Renamed' });
  await db.doc(`households/home/members/${MEMBER}`).delete();   // left the household
  await db.doc('lists/l2').set({ name: 'Cellar' });

  const counts = await migrateToHousehold(db, { ...options, write: true });

  assert.deepEqual(counts, { households: 0, members: 0, lists: 2, items: 1, groups: 1 });
  assert.equal((await data('households/home')).name, 'Renamed');
  assert.equal((await db.doc(`households/home/members/${MEMBER}`).get()).exists, false);
  assert.deepEqual(await data('households/home/lists/l2'), { name: 'Cellar' });
});

test('the owner must be registered', async () => {
  await assert.rejects(migrateToHousehold(db, { ...options, ownerId: 'unknown', write: true }), /not registered/);
  assert.equal((await db.doc('households/home').get()).exists, false);
});
