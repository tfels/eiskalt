// Migration of the old data model into a household (see firestore.rules).
//
// Copies the lists (with their items) and the groups from the top level into households/<hid>/...
// and makes every registered user (members/<uid>) a member of the new household.
// The document IDs stay the same, so groupId in the items and the list IDs in users/<uid> stay valid.
// Nothing is deleted: the current app keeps working on the old data.
//
// Usage (from the firebase/ folder):
//   node scripts/migrate-to-household.mjs --project android-eiskalt --owner <uid> [--name <name>]
//                                         [--household-id <hid>] [--write]
// Without --write it only shows what would be written (dry run).
//
// Credentials: Application Default Credentials, e.g. once "gcloud auth application-default login".
// Against the emulator: set FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 and use --project demo-eiskalt.
import { parseArgs } from 'node:util';
import { pathToFileURL } from 'node:url';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

/**
 * Copy the old data into the household householdId.
 * The household and its memberships are only created if the household does not exist yet,
 * a second run (e.g. right before the app release) only copies the data again.
 * @returns the number of documents per kind that are (or would be) written
 */
export async function migrateToHousehold(db, { householdId, householdName, ownerId, write = false, log = console.log }) {
  const members = await db.collection('members').get();
  if (!members.docs.some((member) => member.id === ownerId)) {
    throw new Error(`The owner ${ownerId} is not registered (no document members/${ownerId})`);
  }

  // collect all writes first, so the dry run shows exactly what would be written
  const writes = [];
  const counts = { households: 0, members: 0, lists: 0, items: 0, groups: 0 };

  const household = db.doc(`households/${householdId}`);
  if (!(await household.get()).exists) {
    writes.push([household, { name: householdName, createdBy: ownerId, ownerId }]);
    counts.households++;
    for (const member of members.docs) {
      writes.push([household.collection('members').doc(member.id), { uid: member.id, name: member.get('name') || member.id }]);
      counts.members++;
    }
  } else {
    log(`Household ${householdId} exists already: only the lists and groups are copied again`);
  }

  for (const list of (await db.collection('lists').get()).docs) {
    const newList = household.collection('lists').doc(list.id);
    writes.push([newList, list.data()]);
    counts.lists++;
    for (const item of (await list.ref.collection('items').get()).docs) {
      writes.push([newList.collection('items').doc(item.id), item.data()]);
      counts.items++;
    }
  }

  for (const group of (await db.collection('groups').get()).docs) {
    writes.push([household.collection('groups').doc(group.id), group.data()]);
    counts.groups++;
  }

  for (const [ref] of writes) {
    log(`${write ? 'write' : 'would write'} ${ref.path}`);
  }
  if (write) {
    const bulkWriter = db.bulkWriter();
    for (const [ref, data] of writes) {
      bulkWriter.set(ref, data);
    }
    await bulkWriter.close();
  }
  return counts;
}

async function main() {
  const { values } = parseArgs({
    options: {
      project: { type: 'string' },
      owner: { type: 'string' },
      name: { type: 'string', default: 'Home' },
      'household-id': { type: 'string' },
      write: { type: 'boolean', default: false },
    },
  });
  if (!values.project || !values.owner) {
    console.error('Usage: node scripts/migrate-to-household.mjs --project <project> --owner <uid> '
      + '[--name <name>] [--household-id <hid>] [--write]');
    process.exit(1);
  }

  initializeApp({ projectId: values.project });
  const db = getFirestore();
  const householdId = values['household-id'] ?? db.collection('households').doc().id;
  const target = process.env.FIRESTORE_EMULATOR_HOST ? `emulator ${process.env.FIRESTORE_EMULATOR_HOST}` : 'PRODUCTION';
  console.log(`Project ${values.project} (${target}), household ${householdId}, ${values.write ? 'WRITE' : 'dry run'}`);

  const counts = await migrateToHousehold(db, {
    householdId, householdName: values.name, ownerId: values.owner, write: values.write,
  });
  console.log(`${values.write ? 'Written' : 'Would write'}:`, counts);
  if (!values['household-id']) {
    console.log(`To copy into the same household again: --household-id ${householdId}`);
  }
}

// only when started as a script, not when imported by the tests
if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  main().catch((e) => {
    console.error(e.message);
    process.exit(1);
  });
}
