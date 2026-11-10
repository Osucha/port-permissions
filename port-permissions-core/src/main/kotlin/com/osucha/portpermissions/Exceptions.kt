package com.osucha.portpermissions

/** The principal is authenticated but lacks [permission]. Maps to HTTP 403. */
public class PermissionDeniedException(
    public val permission: Permission,
) : RuntimeException("Missing permission: ${permission.key}")

/** There is no authenticated principal to check permissions against. Maps to HTTP 401. */
public class NotAuthenticatedException : RuntimeException("No authenticated principal")

/** A grant refers to keys that no module declares. Maps to HTTP 400 on the management API. */
public class UnknownPermissionException(
    public val keys: Set<String>,
) : IllegalArgumentException("Unknown permissions: ${keys.sorted().joinToString()}")

/** Two modules declare the same permission key. Raised at startup, while building the catalog. */
public class DuplicatePermissionException(
    public val keys: Set<String>,
) : IllegalStateException("Permissions declared more than once: ${keys.sorted().joinToString()}")
