# 0014: Admin problem authoring

Date: 2026-09-11

Status: Accepted.

## Decision

Store USER/ADMIN roles on users, defaulting to USER for existing accounts and public
registration. Registration privacy rejects ADMIN creation, and ordinary credential
reads and account updates remain denied.

Spring Security establishes the user ID at login. Route rules, creation mutations, and Ent
creation rules read the current role for that ID when checking permissions; Ent rules also
require its ID to match the viewer context. Credential lookup's privacy bypass stays confined
to UserDao. Role changes apply on the next permission check without another login; see
[0015: Current roles for authorization](0015-current-roles-for-authorization.md).

The `/problem/create` route is defined before `/problem/{slug}` and requires an admin
through spa-routing. Client navigation leaves the creation page when its authorization
request fails. The GraphQL mutation independently requires an admin.

Creation publishes the shared statement and public examples alongside one or more language
configurations in one EntKt transaction. Each configuration specifies an enabled language,
starter code, and solution filename. The form reads its language choices from the catalog;
Kotlin is an initial seeded entry, not a requirement for a problem. Judge configuration and
code execution remain separate work. The form clearly labels its action “Create and publish.”

## Local administrator setup

No account is promoted automatically. After registering an account, an operator can grant
access directly in the development database:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-account@example.com';
```

Visit `/problem/create` afterward. An existing session picks up the new role on its next request.
