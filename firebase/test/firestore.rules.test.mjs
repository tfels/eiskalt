// Tests for the current rules (data at the top level, access via members/<uid>).
// Run with "npm run test:rules" (starts the Firestore emulator, see package.json).
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';

// alice is a member, bob is signed in but not a member
const MEMBER = 'alice';
const NON_MEMBER = 'bob';

// the shared data, every member may read and write it
const DATA_PATHS = ['lists/l1', 'lists/l1/items/i1', 'groups/g1'];

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
    await db.doc(`members/${MEMBER}`).set({ name: 'Alice' });
    await db.doc('lists/l1').set({ name: 'Freezer' });
    await db.doc('lists/l1/items/i1').set({ name: 'Ice cream' });
    await db.doc('groups/g1').set({ name: 'Sweets' });
    await db.doc(`users/${MEMBER}`).set({ settings: {} });
    await db.doc(`users/${NON_MEMBER}`).set({ settings: {} });
  });
});

function unauthenticated() {
  return testEnv.unauthenticatedContext().firestore();
}

function signedInAs(uid) {
  return testEnv.authenticatedContext(uid).firestore();
}

describe('not signed in', () => {
  for (const path of [...DATA_PATHS, `users/${MEMBER}`]) {
    test(`no read/write on ${path}`, async () => {
      const db = unauthenticated();
      await assertFails(db.doc(path).get());
      await assertFails(db.doc(path).set({ name: 'x' }));
    });
  }
});

describe('signed in, but not a member', () => {
  for (const path of DATA_PATHS) {
    test(`no read/write on ${path}`, async () => {
      const db = signedInAs(NON_MEMBER);
      await assertFails(db.doc(path).get());
      await assertFails(db.doc(path).set({ name: 'x' }));
    });
  }

  test('no access to the own users document either', async () => {
    const db = signedInAs(NON_MEMBER);
    await assertFails(db.doc(`users/${NON_MEMBER}`).get());
    await assertFails(db.doc(`users/${NON_MEMBER}`).set({ settings: {} }));
  });

  test('cannot register itself (create members/<own uid>)', async () => {
    await assertFails(signedInAs(NON_MEMBER).doc(`members/${NON_MEMBER}`).set({ name: 'Bob' }));
  });
});

describe('member', () => {
  for (const path of DATA_PATHS) {
    test(`read, create, update and delete on ${path}`, async () => {
      const db = signedInAs(MEMBER);
      await assertSucceeds(db.doc(path).get());
      await assertSucceeds(db.doc(path).update({ name: 'changed' }));
      await assertSucceeds(db.doc(`${path}-new`).set({ name: 'new' }));
      await assertSucceeds(db.doc(path).delete());
    });
  }

  test('can query the lists and the items of a list', async () => {
    const db = signedInAs(MEMBER);
    await assertSucceeds(db.collection('lists').get());
    await assertSucceeds(db.collection('lists/l1/items').get());
    await assertSucceeds(db.collection('groups').get());
  });

  test('read/write on the own users document', async () => {
    const db = signedInAs(MEMBER);
    await assertSucceeds(db.doc(`users/${MEMBER}`).get());
    await assertSucceeds(db.doc(`users/${MEMBER}`).set({ settings: { title: 'x' } }));
  });

  test('no read/write on the users document of someone else', async () => {
    const db = signedInAs(MEMBER);
    await assertFails(db.doc(`users/${NON_MEMBER}`).get());
    await assertFails(db.doc(`users/${NON_MEMBER}`).set({ settings: {} }));
  });

  test('no access to members, not even to the own document', async () => {
    const db = signedInAs(MEMBER);
    await assertFails(db.doc(`members/${MEMBER}`).get());
    await assertFails(db.doc(`members/${MEMBER}`).update({ name: 'x' }));
    await assertFails(db.doc(`members/${NON_MEMBER}`).set({ name: 'Bob' }));
    await assertFails(db.collection('members').get());
  });

  test('no access to unknown collections', async () => {
    const db = signedInAs(MEMBER);
    await assertFails(db.doc('foo/x').get());
    await assertFails(db.doc('foo/x').set({ name: 'x' }));
  });
});
