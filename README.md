
![CI](https://github.com/tfels/eiskalt/actions/workflows/ci.yml/badge.svg?branch=main)

# eiskalt
An android app for a shared inventory list of your freezer

## Households
The lists, items and groups belong to a household and are shared by all its members.
A user can be a member of several households; the app always works with one of them, the active household:

- the household last used on this device, if the user is still a member of it,
- otherwise the only one, if the user is a member of just one household,
- otherwise the user chooses one.

The active household is only stored on the device, so each device can use another one.
Its name is shown as the title of the list overview.
With several households, **Switch household** on the household screen changes the active household.
If the user is not a member of any household, or the households can't be loaded (e.g. offline on the first start),
the app shows a message instead of the lists.

### Managing a household
The menu entry **Household** opens the active household:

- every member can rename the household and see its members,
- members can leave the household; the owner has to make another member the owner first,
- the owner can make another member the owner and remove members (tap a member),
- the owner can delete the household as its last member; its lists have to be deleted before,
  its groups and open invites are deleted together with it.

### Creating a household
Registered users (members of at least one household, or registered via the Firebase console)
can create a new household on the household screen with **Create a new household**,
or from the message shown when they are not a member of any household.
They become its owner and first member, and it becomes the active household.
To share some lists with other people only, create a new household and invite them.

### Invitations
Every member can invite others on the household screen with the e-mail address of their Google account.
An invite is valid for 6 days; it can be shared (e.g. by mail or messenger) and revoked while it is open.

The invited user signs in to the app with this Google account and gets the invite shown after signing in,
or later via **Invitations** in the menu. They can accept it, decline it or decide later.
Accepting makes them a member of the household (and registers them, if it is their first household).
Open invites are shown before the choice of the household.

## Firebase
The data model, security rules, registration and the Firebase CLI are described in [firebase/README.md](firebase/README.md).

## License
Copyright 2025-2026 Tim Felser

Licensed under the [Apache License 2.0](LICENSE).
