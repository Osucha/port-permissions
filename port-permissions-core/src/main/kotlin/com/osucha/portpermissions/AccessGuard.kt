package com.osucha.portpermissions

/**
 * The authorization port used by the application layer. Every use case (an implementation of an
 * inbound port) states the permission it needs by calling [require] as its first step:
 *
 * ```
 * override fun issue(command: IssueInvoice): InvoiceId {
 *     guard.require(BillingPermissions.INVOICES_ISSUE)
 *     ...
 * }
 * ```
 *
 * Because the check sits in the use case and not in a REST controller, it holds for every
 * adapter that drives the port - HTTP, a message listener, a CLI - and the core does not depend
 * on any security framework. Operations that deliberately need no permission are marked with
 * [PublicOperation].
 */
public fun interface AccessGuard {
    public fun isGranted(permission: Permission): Boolean

    /** Throws [PermissionDeniedException] when the current principal lacks [permission]. */
    public fun require(permission: Permission) {
        if (!isGranted(permission)) throw PermissionDeniedException(permission)
    }

    public companion object {
        /** A guard with a fixed set of permissions - handy in use case unit tests. */
        public fun allowing(vararg permissions: Permission): AccessGuard {
            val keys = permissions.map { it.key }.toSet()
            return AccessGuard { it.key in keys }
        }
    }
}

/**
 * [AccessGuard] that checks permissions against the effective keys of the current principal,
 * typically resolved once at login by [GrantResolver] and carried in the session or token - so a
 * check is an in-memory lookup, without a database round trip.
 *
 * [currentPermissions] returns null when there is no authenticated principal.
 */
public class GrantedPermissionsAccessGuard(
    private val currentPermissions: () -> Set<String>?,
) : AccessGuard {
    override fun isGranted(permission: Permission): Boolean {
        val granted = currentPermissions() ?: throw NotAuthenticatedException()
        return permission.key in granted
    }
}
