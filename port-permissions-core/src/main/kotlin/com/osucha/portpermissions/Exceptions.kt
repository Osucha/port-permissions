package com.osucha.portpermissions

/**
 * The principal is authenticated but lacks [permission] - on [scope] (a scope key) for a scoped
 * permission. Maps to HTTP 403.
 */
public class PermissionDeniedException(
    public val permission: PermissionDefinition,
    public val scope: String? = null,
) : RuntimeException("Missing permission: ${permission.key}" + (scope?.let { " on ${permission.scope} $it" } ?: ""))

/** There is no authenticated principal to check permissions against. Maps to HTTP 401. */
public class NotAuthenticatedException : RuntimeException("No authenticated principal")

/**
 * A system operation was called outside [AccessGuard.asSystem] - a wiring mistake, not a missing
 * permission. Maps to HTTP 403 if it ever reaches a user-facing adapter.
 */
public class SystemOnlyOperationException(
    public val principal: PrincipalKind,
) : RuntimeException("System operation called by a $principal principal")

/** A grant refers to keys that no module declares. Maps to HTTP 400 on the management API. */
public class UnknownPermissionException(
    public val keys: Set<String>,
) : IllegalArgumentException("Unknown permissions: ${keys.sorted().joinToString()}")

/** Grants with a scope on a global permission, or a scope key of the wrong form. Maps to HTTP 400. */
public class InvalidGrantException(
    public val grants: List<Grant>,
) : IllegalArgumentException("Invalid grants: ${grants.joinToString()}")

/** Two modules declare the same permission key. Raised at startup, while building the catalog. */
public class DuplicatePermissionException(
    public val keys: Set<String>,
) : IllegalStateException("Permissions declared more than once: ${keys.sorted().joinToString()}")
