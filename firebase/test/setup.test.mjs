// Smoke test: checks that the emulator runs and loads our rules.
// Run with "npm run test:rules" (starts the Firestore emulator, see package.json).
import { readFileSync } from 'node:fs';
import { after, before, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';

let testEnv;

before(async () => {
  // "demo-" project: the emulator never touches the real project
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-eiskalt',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().doc('members/alice').set({ name: 'Alice' });
  });
});

after(async () => {
  await testEnv.cleanup();
});

test('not signed in: no access', async () => {
  await assertFails(testEnv.unauthenticatedContext().firestore().doc('lists/l1').get());
});

test('member: access to lists', async () => {
  await assertSucceeds(testEnv.authenticatedContext('alice').firestore().doc('lists/l1').get());
});
