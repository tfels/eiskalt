# Firebase setup

This folder contains the Firebase configuration for the eiskalt app (project `android-eiskalt`).

| File | Purpose |
|---|---|
| `firestore.rules` | Firestore security rules |
| `firebase.json` | Firebase CLI config (points to the rules file) |
| `.firebaserc` | Links this folder to the Firebase project |
| `package.json` | Local install of the Firebase CLI (`firebase-tools`) |

## Access model

The app signs in with Google (Firebase Authentication).
Access to Firestore is granted only to **members**:
a user is a member if a document `members/<uid>` exists.

- `lists`, `lists/{id}/items` and `groups` are readable and writable by members.
- `members` itself is not accessible from the app at all; it is maintained via the Firebase console.
- Any collection without an explicit rule is denied. A new top-level collection in the app needs a new rule in `firestore.rules`.

## Managing members

Each member is one document in the top-level collection `members`:

```
members/
  <uid>/
    name:  "<display name>"   (informational only)
    email: "<email>"          (informational only)
```

Only the document ID (the UID) is used by the rules; the fields are just for identifying people.

### Add a member

1. The person signs in to the app once with Google.
   This creates their Firebase account and UID. They will not see any data yet.
2. Open the user list in the console:
   https://console.firebase.google.com/project/android-eiskalt/authentication/users
   and copy their **User UID**.
3. In **Firestore Database → Data**, add a document to `members` with the UID as **Document ID** (not Auto-ID) and fill in `name` / `email`.
4. The person restarts the app. Access works immediately.

### Remove a member

Delete their document in `members`. Access is revoked immediately.

## Using the Firebase CLI

The CLI is installed locally in this folder, so all commands are run from here via `npx` / `npm`.

Requirements: a current Node.js LTS.

```
cd firebase
npm install                # install the CLI
npx firebase login         # once per machine, opens the browser
npx firebase use           # shows the active project (should be android-eiskalt)
```

### Deploy the security rules

```
npm run deploy:rules
```

This is a shortcut for `npx firebase deploy --only firestore:rules`.

Note: the deploy overwrites the rules in the console. If rules were changed in the console directly, copy those changes into `firestore.rules` first.

### Test rules before deploying (optional)

The **Rules Playground** in the console (Firestore → Rules) can simulate requests,
e.g. a read on `/lists/test` with a member UID (allowed) and a random UID (denied).
