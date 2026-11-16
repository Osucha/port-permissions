package com.osucha.portpermissions

import java.io.Serializable

/**
 * Who is calling. Carried in the snapshot for information and audit; the library treats all kinds
 * the same way, except that [SYSTEM] holds no permissions (see [AccessGuard.asSystem]).
 */
public enum class PrincipalKind {
    /** A person, signed in. */
    USER,

    /** A token acting on behalf of a user, limited to the capabilities it carries. */
    DELEGATED,

    /** A machine or CI token, holding only the permissions it was issued with. */
    MACHINE,

    /** Work started by the application itself: schedulers, queue workers, startup seeding. */
    SYSTEM,
}

/**
 * The scopes of one scoped permission, as stable string keys. Either every scope minus [except]
 * ([all] = true), or exactly [only] ([all] = false).
 */
public data class ScopeSet(
    val all: Boolean,
    val only: Set<String> = emptySet(),
    val except: Set<String> = emptySet(),
) : Serializable {
    init {
        kotlin.require(if (all) only.isEmpty() else except.isEmpty()) {
            "A scope set is either all-except or only, not both"
        }
    }

    public operator fun contains(scopeKey: String): Boolean = if (all) scopeKey !in except else scopeKey in only

    internal val isEmpty: Boolean get() = !all && only.isEmpty()

    internal infix fun intersect(other: ScopeSet): ScopeSet =
        when {
            all && other.all -> allExcept(except + other.except)
            all -> only(other.only - except)
            other.all -> only(only - other.except)
            else -> only(only intersect other.only)
        }

    public companion object {
        public fun only(keys: Set<String>): ScopeSet = ScopeSet(all = false, only = keys)

        public fun allExcept(keys: Set<String> = emptySet()): ScopeSet = ScopeSet(all = true, except = keys)
    }
}

/**
 * Snapshot of what the current principal may do: global permission keys and, per scoped
 * permission key, the scopes it is held on.
 *
 * It is a plain data class of strings, booleans, sets and maps, so it serializes as is - into a
 * session, a token or a `GET /me` response:
 *
 * ```json
 * {
 *   "principal": "USER",
 *   "global": ["projects.projects.create"],
 *   "scoped": {
 *     "projects.run.start": { "all": false, "only": ["p-1", "p-2"], "except": [] },
 *     "projects.run.view":  { "all": true,  "only": [], "except": ["p-9"] }
 *   }
 * }
 * ```
 *
 * A frontend may use it to show or hide controls; it is never a security decision there.
 */
public data class EffectivePermissions(
    val principal: PrincipalKind,
    val global: Set<String> = emptySet(),
    val scoped: Map<String, ScopeSet> = emptyMap(),
) : Serializable {
    public fun holds(permission: Permission): Boolean = permission.key in global

    public fun <S : Any> holds(
        permission: ScopedPermission<S>,
        scope: S,
    ): Boolean = scoped[permission.key]?.contains(permission.scopeType.key(scope)) ?: false

    public fun <S : Any> scopesOf(permission: ScopedPermission<S>): ScopeFilter<S> {
        val set = scoped[permission.key] ?: return ScopeFilter.Only(emptySet())
        val type = permission.scopeType
        return when {
            set.all && set.except.isEmpty() -> ScopeFilter.All
            set.all -> ScopeFilter.AllExcept(set.except.map(type::parse).toSet())
            else -> ScopeFilter.Only(set.only.map(type::parse).toSet())
        }
    }

    /**
     * Permissions of a token acting on behalf of a user: only what both the user holds and the
     * token's [capabilities] allow.
     */
    public fun narrowedTo(capabilities: EffectivePermissions): EffectivePermissions =
        EffectivePermissions(
            principal = PrincipalKind.DELEGATED,
            global = global intersect capabilities.global,
            scoped =
                scoped
                    .mapNotNull { (key, set) -> capabilities.scoped[key]?.let { key to (set intersect it) } }
                    .filterNot { it.second.isEmpty }
                    .toMap(),
        )

    public companion object {
        /** The system principal holds no permissions; system operations call [AccessGuard.requireSystem]. */
        public val SYSTEM: EffectivePermissions = EffectivePermissions(PrincipalKind.SYSTEM)

        /** A snapshot holding exactly [access] - for machine tokens and tests. */
        public fun of(
            vararg access: Access,
            principal: PrincipalKind = PrincipalKind.USER,
        ): EffectivePermissions {
            val global = mutableSetOf<String>()
            val scoped = mutableMapOf<String, ScopeSet>()
            for (item in access) {
                when (item) {
                    is Permission -> global += item.key
                    is AllScopesAccess<*> -> scoped[item.permission.key] = ScopeSet.allExcept()
                    is ScopedAccess<*> -> {
                        val current = scoped[item.permission.key] ?: ScopeSet.only(emptySet())
                        if (!current.all) scoped[item.permission.key] = ScopeSet.only(current.only + item.scopeKey)
                    }
                }
            }
            return EffectivePermissions(principal, global, scoped)
        }
    }
}
