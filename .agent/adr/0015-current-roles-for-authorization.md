# 0015: Current roles for authorization

Date: 2026-09-11

Status: Accepted. Updates the authorization behavior in 0014.

## Decision

Use the Spring Security session to identify the caller, then read the current database role
at every application permission check. Route rules, the creation service, and Ent creation
rules share this lookup through `CurrentUser`. They do not authorize from the role or
authorities captured at login.

The lookup selects only `role` from `users`, using the authenticated database ID as a bound
parameter. It uses the existing Spring JDBC integration because EntKt currently loads whole
entities and cannot project only the role. This is a narrow authorization lookup: it does
not read credentials, change UserPolicy, or reuse authentication's privacy bypass.

Missing accounts have no permissions. Database errors propagate instead of falling back to
a session's old role. There is no role cache. An in-progress operation is not cancelled by
a role change; subsequent permission checks use the committed database value.

## Consequences

Granting or revoking admin access takes effect on the next permission check, without another
login. Permission checks add a small indexed database lookup. Authorization depends on
database availability. Tests promote and demote users while retaining the same authenticated
session and exercise direct routes, SPA decisions, GraphQL mutations, and Ent writes.
