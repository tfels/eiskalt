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

// e-mail addresses in the auth token (bob is invited to h1)
const INVITED_EMAIL = 'bob@example.com';
const OTHER_EMAIL = 'carol@example.com';

const DAY = 24 * 60 * 60 * 1000;

function daysFromNow(days) {
  return new Date(Date.now() + days * DAY);
}

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
    // open invites to h1 for bob (not registered yet) and carol (already registered), and an expired one
    await db.doc('invites/inv1').set({ householdId: 'h1', email: INVITED_EMAIL, invitedBy: OWNER, expiresAt: daysFromNow(3) });
    await db.doc('invites/inv2').set({ householdId: 'h1', email: OTHER_EMAIL, invitedBy: OWNER, expiresAt: daysFromNow(3) });
    await db.doc('invites/expired').set({ householdId: 'h1', email: INVITED_EMAIL, invitedBy: OWNER, expiresAt: daysFromNow(-1) });
  });
});

function unauthenticated() {
  return testEnv.unauthenticatedContext().firestore();
}

function signedInAs(uid, email, emailVerified = true) {
  const token = email ? { email, email_verified: emailVerified } : {};
  return testEnv.authenticatedContext(uid, token).firestore();
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

/**
 * A new invite to h1 from uid, with changed / additional fields
 */
function invite(uid, fields = {}) {
  return { householdId: 'h1', email: 'erin@example.com', invitedBy: uid, expiresAt: daysFromNow(7 - 1 / 24), ...fields };
}

describe('create an invite', () => {
  for (const uid of [OWNER, MEMBER]) {
    test(`${uid}: every member can invite for at most 7 days`, async () => {
      await assertSucceeds(signedInAs(uid).collection('invites').add(invite(uid)));
    });
  }

  test('not a member of the household: cannot invite', async () => {
    await assertFails(signedInAs(OTHER_HOUSEHOLD_MEMBER).collection('invites').add(invite(OTHER_HOUSEHOLD_MEMBER)));
    await assertFails(signedInAs(NOT_REGISTERED).collection('invites').add(invite(NOT_REGISTERED)));
    await assertFails(unauthenticated().collection('invites').add(invite(OWNER)));
  });

  test('invitedBy must be the user itself', async () => {
    await assertFails(signedInAs(MEMBER).collection('invites').add(invite(MEMBER, { invitedBy: OWNER })));
  });

  test('e-mail must be valid and in lowercase', async () => {
    const db = signedInAs(OWNER);
    await assertFails(db.collection('invites').add(invite(OWNER, { email: 'Erin@example.com' })));
    await assertFails(db.collection('invites').add(invite(OWNER, { email: 'erin' })));
    await assertFails(db.collection('invites').add(invite(OWNER, { email: '' })));
  });

  test('must expire in the future, but within 7 days', async () => {
    const db = signedInAs(OWNER);
    await assertFails(db.collection('invites').add(invite(OWNER, { expiresAt: daysFromNow(-1) })));
    await assertFails(db.collection('invites').add(invite(OWNER, { expiresAt: daysFromNow(8) })));
    await assertFails(db.collection('invites').add(invite(OWNER, { expiresAt: 'tomorrow' })));
  });

  test('no additional fields', async () => {
    await assertFails(signedInAs(OWNER).collection('invites').add(invite(OWNER, { extra: 1 })));
  });

  test('cannot be changed afterwards (e.g. extended)', async () => {
    await assertFails(signedInAs(OWNER).doc('invites/inv1').update({ expiresAt: daysFromNow(6) }));
  });
});

describe('read and delete an invite', () => {
  test('invited user: can read, find and decline (delete) its invite', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    await assertSucceeds(db.doc('invites/inv1').get());
    await assertSucceeds(db.collection('invites').where('email', '==', INVITED_EMAIL).get());
    await assertSucceeds(db.doc('invites/inv1').delete());
  });

  test('invited user: the e-mail in the token may differ in case', async () => {
    await assertSucceeds(signedInAs(NOT_REGISTERED, 'Bob@Example.com').doc('invites/inv1').get());
  });

  for (const uid of [OWNER, MEMBER]) {
    test(`${uid}: members of the household can read and revoke (delete) the invites`, async () => {
      const db = signedInAs(uid);
      await assertSucceeds(db.doc('invites/inv1').get());
      await assertSucceeds(db.collection('invites').where('householdId', '==', 'h1').get());
      await assertSucceeds(db.doc('invites/inv1').delete());
    });
  }

  test('others: cannot read, find or delete the invite', async () => {
    const db = signedInAs(OTHER_HOUSEHOLD_MEMBER, OTHER_EMAIL);
    await assertFails(db.doc('invites/inv1').get());
    await assertFails(db.collection('invites').get());
    await assertFails(db.collection('invites').where('email', '==', INVITED_EMAIL).get());
    await assertFails(db.doc('invites/inv1').delete());
    await assertFails(unauthenticated().doc('invites/inv1').get());
  });
});

/**
 * Join the household hid with an invite in one batch: create the membership, use up (delete) the invite
 * and, for the first household, register (create members/<uid>).
 * member and registration: changed / additional fields for the documents
 * register / deleteInvite: false to leave out that part of the batch
 */
function joinHousehold(db, uid, email, { hid = 'h1', inviteId = 'inv1', member = {}, registration = {},
                                       register = true, deleteInvite = true } = {}) {
  const batch = db.batch();
  batch.set(db.doc(`households/${hid}/members/${uid}`), { uid, name: uid, inviteId, ...member });
  if (deleteInvite) {
    batch.delete(db.doc(`invites/${inviteId}`));
  }
  if (register) {
    batch.set(db.doc(`members/${uid}`), { name: uid, email, householdId: hid, ...registration });
  }
  return batch.commit();
}

describe('join a household with an invite', () => {
  test('not registered user: joins and registers, then has access to the household', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    await assertSucceeds(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL));
    await assertSucceeds(db.doc('households/h1/lists/l1').get());
  });

  test('already registered user: joins without registering again', async () => {
    const db = signedInAs(OTHER_HOUSEHOLD_MEMBER, OTHER_EMAIL);
    await assertSucceeds(joinHousehold(db, OTHER_HOUSEHOLD_MEMBER, OTHER_EMAIL, { inviteId: 'inv2', register: false }));
  });

  test('the invite must be used up (deleted) in the same batch', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { deleteInvite: false }));
  });

  test('the invite must be for this household', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { hid: 'h2' }));
  });

  test('the invite must be for the own, verified e-mail address', async () => {
    // carol uses the invite for bob
    await assertFails(joinHousehold(signedInAs(OTHER_HOUSEHOLD_MEMBER, OTHER_EMAIL), OTHER_HOUSEHOLD_MEMBER, OTHER_EMAIL,
      { register: false }));
    // bob, but the e-mail address is not verified
    await assertFails(joinHousehold(signedInAs(NOT_REGISTERED, INVITED_EMAIL, false), NOT_REGISTERED, INVITED_EMAIL));
  });

  test('the invite must exist and not be expired', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { inviteId: 'unknown' }));
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { inviteId: 'expired' }));
  });

  test('cannot add someone else with the invite', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    const batch = db.batch();
    batch.set(db.doc('households/h1/members/erin'), { uid: 'erin', name: 'erin', inviteId: 'inv1' });
    batch.delete(db.doc('invites/inv1'));
    await assertFails(batch.commit());
  });

  test('membership: name must not be empty, no additional fields', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { member: { name: '' } }));
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { member: { extra: 1 } }));
  });

  test('registration: only together with joining, with the own e-mail, no additional fields', async () => {
    const db = signedInAs(NOT_REGISTERED, INVITED_EMAIL);
    // only the registration, without membership
    await assertFails(db.doc(`members/${NOT_REGISTERED}`).set({ name: 'bob', email: INVITED_EMAIL, householdId: 'h1' }));
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { registration: { email: OTHER_EMAIL } }));
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { registration: { householdId: 'h2' } }));
    await assertFails(joinHousehold(db, NOT_REGISTERED, INVITED_EMAIL, { registration: { extra: 1 } }));
  });
});
