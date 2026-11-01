// Tests for the household rules (data under households/<hid>, access via households/<hid>/members/<uid>).
// Run with "npm run test:rules" (starts the Firestore emulator, see package.json).
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';

// alice owns household h1, dave is another member of h1,
// carol owns household h2, bob is signed in but not registered
const OWNER = 'alice';
const MEMBER = 'dave';
const OTHER_HOUSEHOLD_MEMBER = 'carol';
const NOT_REGISTERED = 'bob';

// the shared data of a household, every member may read and write it
function dataPaths(hid) {
  return [`households/${hid}/lists/l1`, `households/${hid}/lists/l1/items/i1`, `households/${hid}/groups/g1`];
}

let testEnv;

before(async () => {
  // "demo-" project: the emulator never touches the real project
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-eiskalt',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => {
  await testEnv.cleanup();
});

// every test starts with the same data
beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    // global registration
    for (const uid of [OWNER, MEMBER, OTHER_HOUSEHOLD_MEMBER]) {
      await db.doc(`members/${uid}`).set({ name: uid });
    }
    // two households with their members and data
    const households = { h1: [OWNER, MEMBER], h2: [OTHER_HOUSEHOLD_MEMBER] };
    for (const [hid, uids] of Object.entries(households)) {
      await db.doc(`households/${hid}`).set({ name: hid, createdBy: uids[0], ownerId: uids[0] });
      for (const uid of uids) {
        await db.doc(`households/${hid}/members/${uid}`).set({ uid, name: uid });
      }
      await db.doc(`households/${hid}/lists/l1`).set({ name: 'Freezer' });
      await db.doc(`households/${hid}/lists/l1/items/i1`).set({ name: 'Ice cream' });
      await db.doc(`households/${hid}/groups/g1`).set({ name: 'Sweets' });
    }
  });
});

function unauthenticated() {
  return testEnv.unauthenticatedContext().firestore();
}

function signedInAs(uid) {
  return testEnv.authenticatedContext(uid).firestore();
}

describe('member of the household', () => {
  for (const uid of [OWNER, MEMBER]) {
    test(`${uid}: can read the household and its members`, async () => {
      const db = signedInAs(uid);
      await assertSucceeds(db.doc('households/h1').get());
      await assertSucceeds(db.collection('households/h1/members').get());
    });
  }

  for (const path of dataPaths('h1')) {
    test(`read, create, update and delete on ${path}`, async () => {
      const db = signedInAs(MEMBER);
      await assertSucceeds(db.doc(path).get());
      await assertSucceeds(db.doc(path).update({ name: 'changed' }));
      await assertSucceeds(db.doc(`${path}-new`).set({ name: 'new' }));
      await assertSucceeds(db.doc(path).delete());
    });
  }

  test('can query the lists, items and groups', async () => {
    const db = signedInAs(MEMBER);
    await assertSucceeds(db.collection('households/h1/lists').get());
    await assertSucceeds(db.collection('households/h1/lists/l1/items').get());
    await assertSucceeds(db.collection('households/h1/groups').get());
  });
});

describe('member of another household', () => {
  test('cannot read the household or its members', async () => {
    const db = signedInAs(OTHER_HOUSEHOLD_MEMBER);
    await assertFails(db.doc('households/h1').get());
    await assertFails(db.collection('households/h1/members').get());
  });

  for (const path of dataPaths('h1')) {
    test(`no read/write on ${path}`, async () => {
      const db = signedInAs(OTHER_HOUSEHOLD_MEMBER);
      await assertFails(db.doc(path).get());
      await assertFails(db.doc(path).set({ name: 'x' }));
    });
  }

  test('cannot query the lists of the household', async () => {
    await assertFails(signedInAs(OTHER_HOUSEHOLD_MEMBER).collection('households/h1/lists').get());
  });
});

describe('not a member of any household', () => {
  for (const [who, db] of [['not signed in', () => unauthenticated()], ['not registered', () => signedInAs(NOT_REGISTERED)]]) {
    test(`${who}: no access to the household and its data`, async () => {
      await assertFails(db().doc('households/h1').get());
      for (const path of dataPaths('h1')) {
        await assertFails(db().doc(path).get());
        await assertFails(db().doc(path).set({ name: 'x' }));
      }
    });
  }
});
