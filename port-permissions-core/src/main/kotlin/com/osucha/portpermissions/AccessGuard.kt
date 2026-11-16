package com.osucha.portpermissions

/**
 * The authorization port used by the application layer. Every use case (an implementation of an
 * inbound port) states the permission it needs as its first step:
 *
 * ```
 * override fun start(command: StartRun): RunId {
 *     guard.require(ProjectsPermissions.RUN_START, command.projectId)
 *     ...
 * }
 * ```
 *
 * Because the check sits in the use case and not in a REST controller, it holds for every adapter
 * that drives the port - HTTP, a message listener, a CLI - and the core does not depend on any
 * security framework. A scoped permission cannot be required without a scope of its type; that is
 * a compile error.
 *
 * Use [DefaultAccessGuard] unless you have a reason not to.
 */
public interface AccessGuard {
    /** Effective permissions of the current principal; [NotAuthenticatedException] when there is none. */
    public fun effective(): EffectivePermissions

    /**
     * Runs [block] as the system principal, for internal adapters (schedulers, queue workers,
     * startup seeding). The system principal holds no permissions: inside [block] only operations
     * that call [requireSystem] succeed, so this is not a way to escalate privileges.
     */
    public fun <T> asSystem(
        reason: String,
        block: () -> T,
    ): T

    public fun isGranted(permission: Permission): Boolean = effective().holds(permission)

    public fun <S : Any> isGranted(
        permission: ScopedPermission<S>,
        scope: S,
    ): Boolean = effective().holds(permission, scope)

    /** Throws [PermissionDeniedException] when the current principal lacks [permission]. */
    public fun require(permission: Permission) {
        if (!isGranted(permission)) throw PermissionDeniedException(permission)
    }

    /** Throws [PermissionDeniedException] when the current principal lacks [permission] on [scope]. */
    public fun <S : Any> require(
        permission: ScopedPermission<S>,
        scope: S,
    ) {
        if (!isGranted(permission, scope)) throw PermissionDeniedException(permission, permission.scopeType.key(scope))
    }

    /** The scopes on which the current principal holds [permission], for filtering list queries. */
    public fun <S : Any> scopesWith(permission: ScopedPermission<S>): ScopeFilter<S> = effective().scopesOf(permission)

    /**
     * The check of a [SystemOperation]: throws [SystemOnlyOperationException] unless running inside
     * [asSystem]. Makes a system operation unusable from a user-facing path even if wired by mistake.
     */
    public fun requireSystem() {
        val principal = effective().principal
        if (principal != PrincipalKind.SYSTEM) throw SystemOnlyOperationException(principal)
    }

    public companion object {
        /** A guard holding exactly [access] - handy in use case unit tests. */
        public fun allowing(vararg access: Access): AccessGuard = DefaultAccessGuard { EffectivePermissions.of(*access) }

        /** A guard of an authenticated principal holding nothing. */
        public fun denyingAll(): AccessGuard = DefaultAccessGuard { EffectivePermissions(PrincipalKind.USER) }
    }
}

/**
 * [AccessGuard] over an [EffectivePermissionsSource] - which decides how fresh the permissions are
 * (per request, cached in the session, carried in a token).
 *
 * [asSystem] is bound to the current thread, like Spring's `SecurityContext`. Work handed over to
 * another thread or a coroutine dispatcher inside the block does not run as the system.
 */
public class DefaultAccessGuard(
    private val source: EffectivePermissionsSource,
) : AccessGuard {
    private val system = ThreadLocal<String>()

    override fun effective(): EffectivePermissions =
        if (system.get() != null) {
            EffectivePermissions.SYSTEM
        } else {
            source.current() ?: throw NotAuthenticatedException()
        }

    override fun <T> asSystem(
        reason: String,
        block: () -> T,
    ): T {
        kotlin.require(reason.isNotBlank()) { "asSystem needs a reason" }
        val outer = system.get()
        system.set(reason)
        try {
            return block()
        } finally {
            if (outer == null) system.remove() else system.set(outer)
        }
    }

    /** Why the current thread runs as the system, or null when it does not. */
    public fun systemReason(): String? = system.get()
}

/**
 * Guard over a set of global permission keys, as in 0.1.0. Kept for compatibility; prefer
 * [DefaultAccessGuard] with an [EffectivePermissionsSource].
 */
public class GrantedPermissionsAccessGuard(
    currentPermissions: () -> Set<String>?,
) : AccessGuard by DefaultAccessGuard({ currentPermissions()?.let { EffectivePermissions(PrincipalKind.USER, global = it) } })
