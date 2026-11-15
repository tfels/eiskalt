
![CI](https://github.com/tfels/eiskalt/actions/workflows/ci.yml/badge.svg?branch=main)

# eiskalt
An android app for a shared inventory list of your freezer

## Households
The lists, items and groups belong to a household and are shared by all its members.
A user can be a member of several households; the app always works with one of them, the active household:

- the household last used on this device, if the user is still a member of it,
- otherwise the first one by name.

The active household is only stored on the device, so each device can use another one.
If the user is not a member of any household, or the households can't be loaded (e.g. offline on the first start),
the app shows a message instead of the lists.

## Firebase
The data model, security rules, registration and the Firebase CLI are described in [firebase/README.md](firebase/README.md).

## License
Copyright 2025-2026 Tim Felser

Licensed under the [Apache License 2.0](LICENSE).
