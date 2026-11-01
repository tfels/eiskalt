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

/**
 * Create the household hid with uid as creator and owner, and the membership of uid, in one batch.
 * household and member: changed / additional fields for the documents
 * withMember: false to leave out the membership
 */
function createHousehold(db, hid, uid, { household = {}, member = {}, withMember = true } = {}) {
  const batch = db.batch();
  batch.set(db.doc(`households/${hid}`), { name: 'Home', createdBy: uid, ownerId: uid, ...household });
  if (withMember) {
    batch.set(db.doc(`households/${hid}/members/${uid}`), { uid, name: uid, ...member });
  }
  return batch.commit();
}

describe('create a household', () => {
  test('registered user: creates a household with itself as owner and member', async () => {
    await assertSucceeds(createHousehold(signedInAs(OTHER_HOUSEHOLD_MEMBER), 'h3', OTHER_HOUSEHOLD_MEMBER));
  });

  test('not signed in or not registered: cannot create a household', async () => {
    await assertFails(createHousehold(unauthenticated(), 'h3', NOT_REGISTERED));
    await assertFails(createHousehold(signedInAs(NOT_REGISTERED), 'h3', NOT_REGISTERED));
  });

  test('only together with the own membership', async () => {
    await assertFails(createHousehold(signedInAs(OWNER), 'h3', OWNER, { withMember: false }));
  });

  test('creator and owner must be the user itself', async () => {
    const db = signedInAs(OWNER);
    await assertFails(createHousehold(db, 'h3', OWNER, { household: { createdBy: MEMBER } }));
    await assertFails(createHousehold(db, 'h3', OWNER, { household: { ownerId: MEMBER } }));
  });

  test('name must not be empty, no additional fields', async () => {
    const db = signedInAs(OWNER);
    await assertFails(createHousehold(db, 'h3', OWNER, { household: { name: '' } }));
    await assertFails(createHousehold(db, 'h3', OWNER, { household: { extra: 1 } }));
    await assertFails(createHousehold(db, 'h3', OWNER, { member: { name: '' } }));
    await assertFails(createHousehold(db, 'h3', OWNER, { member: { extra: 1 } }));
  });

  test('cannot add someone else as member', async () => {
    const db = signedInAs(OWNER);
    const batch = db.batch();
    batch.set(db.doc('households/h3'), { name: 'Home', createdBy: OWNER, ownerId: OWNER });
    batch.set(db.doc(`households/h3/members/${OWNER}`), { uid: OWNER, name: OWNER });
    batch.set(db.doc(`households/h3/members/${MEMBER}`), { uid: MEMBER, name: MEMBER });
    await assertFails(batch.commit());
  });

  test('cannot add itself to an existing household (without invite)', async () => {
    const db = signedInAs(OTHER_HOUSEHOLD_MEMBER);
    await assertFails(db.doc(`households/h1/members/${OTHER_HOUSEHOLD_MEMBER}`)
      .set({ uid: OTHER_HOUSEHOLD_MEMBER, name: OTHER_HOUSEHOLD_MEMBER }));
  });

  test('cannot take over an existing household', async () => {
    await assertFails(createHousehold(signedInAs(OTHER_HOUSEHOLD_MEMBER), 'h1', OTHER_HOUSEHOLD_MEMBER));
  });
});
