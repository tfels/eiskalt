# Firebase setup

This folder contains the Firebase configuration for the eiskalt app (project `android-eiskalt`).

| File | Purpose |
|---|---|
| `firestore.rules` | Firestore security rules |
| `firestore.indexes.json` | Firestore indexes (collection group index on `members.uid`) |
| `firebase.json` | Firebase CLI config (points to the rules and index files) |
| `.firebaserc` | Links this folder to the Firebase project |
| `package.json` | Local install of the Firebase CLI, the rules test tools and the Admin SDK |
| `test/` | Tests of the security rules, run on the Firestore emulator |
| `scripts/migrate-to-household.mjs` | Migration of the old data model into a household |

## Data model

All shared data belongs to a **household**.
A user can be a member of several households.

```
households/{hid}
  name:      "<household name>"
  createdBy: "<uid>"
  ownerId:   "<uid>"                  // exactly one owner
  members/{uid}
    uid:      "<uid>"                 // needed for the collection group query
    name:     "<display name>"
    inviteId: "<inviteId>"            // only if joined with an invite
  lists/{listId}
    items/{itemId}
  groups/{groupId}

invites/{inviteId}
  householdId, householdName, email (lowercase), invitedBy, invitedByName,
  expiresAt                           // at most 7 days

members/{uid}                         // global registration
  name, email, householdId            // informational only

users/{uid}                           // per-user data, see below
```

The old top-level collections `lists` (with `items`) and `groups` are still allowed for registered users
until all data is migrated into a household (see [Migration](#migration-into-a-household)).

## Access model

The app signs in with Google (Firebase Authentication).

- A user is **registered** if a document `members/<uid>` exists.
  It is created by the user when joining the first household (in the same batch as the membership),
  or manually via the Firebase console. Users can only read their own registration.
- Only registered users can create a household; they become its owner and first member.
- A household, its members, lists, items and groups are readable for the members of the household only;
  lists, items and groups are also writable by them.
- Every member can rename the household and invite others by e-mail.
  Only the owner can hand over the ownership, remove other members and delete the household.
  Members can change their own name and leave the household (the owner must hand over the ownership first).
- An invite can be read and deleted by the invited user and by the members of the household.
  It is accepted with a verified e-mail address and used up (deleted) in the same batch as the new membership.
- The households of a user are found with a collection group query on `members` with `uid == <own uid>`.
  This needs the index in `firestore.indexes.json`.
- `users/{uid}` holds per-user data (settings, folded groups); each user can only read and write their own document.
- Any collection without an explicit rule is denied. A new top-level collection in the app needs a new rule in `firestore.rules`.

## Per-user data (`users/{uid}`)

```
users/{uid}
  settings:         { syncLastViewedList: true }
  lastViewedListId: "<listId>"                        // only if syncLastViewedList is enabled
  collapsedGroups:  { <listId>: [<groupId>, ...] }    // folded groups per list
  updatedAt:        <server timestamp>
```

The document is written by the app itself (`UserSettingsRepository`); all values are also cached locally on the device.

**Known limitation:** the folded state is not cleaned up.
When a group is deleted, its id stays in the `collapsedGroups` arrays; when a list is deleted, its `collapsedGroups.<listId>` entry stays.
The same applies to the local cache on each device.
Unknown ids are simply ignored by the app and each entry is only a few bytes, so this is harmless.
If needed, we can implement a cleanup on load e.g. when viewing a list the collapsed groups list is  checked for existance.

## Registering users manually

Usually a user registers by accepting an invite.
The first user (or a user without an invite) can be registered in the console:

1. The person signs in to the app once with Google.
   This creates their Firebase account and UID. They will not see any data yet.
2. Open the user list in the console:
   https://console.firebase.google.com/project/android-eiskalt/authentication/users
   and copy their **User UID**.
3. In **Firestore Database → Data**, add a document to `members` with the UID as **Document ID** (not Auto-ID) and fill in `name` / `email`.

Only the document ID (the UID) is used by the rules; the fields are just for identifying people.
Deleting the document removes the registration, but not the memberships in households.

## Migration into a household

`scripts/migrate-to-household.mjs` copies the old top-level `lists` (with `items`) and `groups` into a new household
and makes every registered user a member of it. The document IDs stay the same; nothing is deleted.

```
cd firebase
node scripts/migrate-to-household.mjs --project android-eiskalt --owner <uid> [--name <name>] [--household-id <hid>] [--write]
```

- Without `--write` it is a dry run and only shows what would be written.
- With `--household-id` of an existing household only the lists and groups are copied again
  (e.g. right before the app release).
- Credentials: Application Default Credentials (`gcloud auth application-default login`)
  or `GOOGLE_APPLICATION_CREDENTIALS` pointing to a service account key.
  **Never commit a service account key**; keep it outside the repository and delete the key in the console after use.
- `FIRESTORE_EMULATOR_HOST` must not be set when migrating the real project.
  Against the emulator: `FIRESTORE_EMULATOR_HOST=127.0.0.1:8080` and `--project demo-eiskalt`.

## Using the Firebase CLI

The CLI is installed locally in this folder, so all commands are run from here via `npx` / `npm`.

Requirements: a current Node.js LTS.

```
cd firebase
npm install                # install the CLI
npx firebase login         # once per machine, opens the browser
npx firebase use           # shows the active project (should be android-eiskalt)
```

### Deploy the security rules and indexes

```
npm run deploy:firestore   # rules and indexes
npm run deploy:rules       # rules only
```

These are shortcuts for `npx firebase deploy --only firestore` and `npx firebase deploy --only firestore:rules`.

Note: the deploy overwrites the rules in the console. If rules were changed in the console directly, copy those changes into `firestore.rules` first.

### Test the rules

The tests in `test/` run on the Firestore emulator with the demo project `demo-eiskalt`
(no access to the real project):

```
npm run test:rules
```

The emulator needs Java 21 or newer on the `PATH`.
The tests also run in CI (`.github/workflows/firestore-rules.yml`) when something in `firebase/` changes.

In addition, the **Rules Playground** in the console (Firestore → Rules) can simulate single requests.

On Windows PowerShell use `npm.cmd` / `npx.cmd` if script execution is disabled.
