# Changelog

## 0.2.1

### Fixed

- `GrantResolver` threw `IllegalArgumentException` when two grants on the same level and with the same specificity disagreed, e.g. a user in two groups, one granting and one revoking a permission. Login of that user failed. The revocation now wins; `GrantResolver(catalog, SameLevelConflict.GRANT_WINS)` gives the union of group permissions instead.

## 0.2.0

### Added

- **Scoped permissions.** `ScopeType<S>` and `PermissionSet.scoped(name, description, scope)` declare a `ScopedPermission<S>`; `guard.require(permission, scope)` checks it. A missing scope, a scope of another type or a scope on a global permission is a compile error.
- **List filtering.** `guard.scopesWith(permission)` returns `ScopeFilter.All`, `AllExcept(ids)` or `Only(ids)` with typed ids.
- **Scoped grants.** `Grant.scope`; precedence: the most specific level wins, on the same level the scoped grant wins. `PermissionCatalog.ensureValid(grants)` also checks scope consistency.
- **Effective permissions behind a port.** `EffectivePermissions` (serializable snapshot), `EffectivePermissionsSource`, `DefaultAccessGuard`, `SessionCachedPermissions` with `PermissionVersions` / `InMemoryPermissionVersions`, `PermissionsPrincipal` for the token strategy.
- **Principal kinds** in the snapshot (`USER`, `DELEGATED`, `MACHINE`, `SYSTEM`) and `EffectivePermissions.narrowedTo` for delegated tokens; `EffectivePermissions.of(...)` for machine tokens and tests.
- **System operations.** `@SystemOperation(reason)`, `guard.asSystem(reason) { }` (the system principal holds no permissions) and `guard.requireSystem()`.
- **`port-permissions-archunit`**: deny by default on bytecode, following lambdas one level deep, plus rules for system operations and system ports. Port packages are configurable.
- **`port-permissions-test`**: `RecordingAccessGuard`, `assertGuardedBy`, `assertSystemOnly`; `AccessGuard.denyingAll()` in core.
- Compiled for Kotlin 2.3 metadata; CI builds a consumer on Kotlin 2.3.

### Fixed

- `port-permissions-konsist` accepted any `.require(` call, e.g. `validator.require(...)`. It now counts only calls on a constructor parameter or property typed as an access guard, and accepts `@SystemOperation` and `requireSystem()`.

### Migrating from 0.1.0

| 0.1.0 | 0.2.0 |
|---|---|
| `AccessGuard` was a `fun interface` with `isGranted(Permission)` | an interface with `effective()` and `asSystem(...)`; implement it with `DefaultAccessGuard(source)` instead of a lambda: `AccessGuard { p -> ... }` becomes `DefaultAccessGuard { EffectivePermissions(PrincipalKind.USER, global = keys) }` |
| `GrantResolver.resolve(grants): Set<String>` | returns `EffectivePermissions`; the old set is `.global` |
| `PermissionCatalog.permissions`, `modules`, `get(key)` typed with `Permission` | typed with `PermissionDefinition`, the common supertype of `Permission` and `ScopedPermission`; `key`, `module`, `description` are unchanged, `scope` is new |
| `PermissionSet.permissions: List<Permission>` | `List<PermissionDefinition>` |
| `PermissionDeniedException.permission: Permission` | `PermissionDefinition`, plus `scope: String?` |
| `AccessGuard.allowing(vararg Permission)` | `allowing(vararg Access)` - existing calls compile unchanged |
| `GrantedPermissionsAccessGuard`, `SecurityContextAccessGuard` | unchanged constructors; prefer `DefaultAccessGuard` with an `EffectivePermissionsSource` |
| Spring: `AccessGuard` bean was `SecurityContextAccessGuard` | `DefaultAccessGuard` over an `EffectivePermissionsSource` bean (default `SecurityContextPermissionsSource`); the authority prefix is set with `port-permissions.authority-prefix` |
| `@PublicOperation` for jobs without a user | use `@SystemOperation` with `guard.requireSystem()` on a system port; `@PublicOperation` now means "any authenticated caller" |

## 0.1.0

First release: `PermissionSet`, `PermissionCatalog`, `AccessGuard`, `GrantResolver`, `@PublicOperation`, Spring Boot auto-configuration and the Konsist rule.
