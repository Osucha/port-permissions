package com.osucha.portpermissions

import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * The port that answers "what may the current principal do?". The strategy behind it decides how
 * fresh the answer is:
 *
 * - **per request** - resolve grants on every call; always fresh, one query per request:
 *   `EffectivePermissionsSource { currentUserId()?.let { resolver.resolve(grants.of(it)) } }`
 * - **cached in the server-side session** - [SessionCachedPermissions]; fresh after an invalidation,
 *   cheap otherwise. The recommended default for session and BFF applications.
 * - **carried in a token** - resolved at login, read back from the principal ([PermissionsPrincipal]);
 *   for stateless services, with revocation delayed until the token expires.
 *
 * Returns null when there is no authenticated principal.
 */
public fun interface EffectivePermissionsSource {
    public fun current(): EffectivePermissions?
}

/** A principal that carries its effective permissions, e.g. one rebuilt from a token. */
public interface PermissionsPrincipal {
    public val effectivePermissions: EffectivePermissions
}

/**
 * Version of a principal's permissions. Bump it whenever grants, roles or memberships that feed the
 * principal's permissions change; a cached snapshot with an older version is resolved again.
 *
 * With several application instances the version must live in shared storage - for example a
 * column on the user row incremented in the same transaction as the grant change.
 * [InMemoryPermissionVersions] is enough for a single instance and for tests.
 */
public fun interface PermissionVersions {
    public fun current(principalId: String): Long
}

/** [PermissionVersions] in memory: valid for one application instance only. */
public class InMemoryPermissionVersions : PermissionVersions {
    private val everyone = AtomicLong()
    private val perPrincipal = ConcurrentHashMap<String, Long>()

    override fun current(principalId: String): Long = everyone.get() + (perPrincipal[principalId] ?: 0L)

    /** Call when the grants, roles or memberships of one principal change. */
    public fun invalidate(principalId: String) {
        perPrincipal.merge(principalId, 1L, Long::plus)
    }

    /** Call when something affecting many principals changes, e.g. a group's grants. */
    public fun invalidateAll() {
        everyone.incrementAndGet()
    }
}

public data class CachedPermissions(
    val principalId: String,
    val version: Long,
    val permissions: EffectivePermissions,
) : Serializable

/** Where [SessionCachedPermissions] keeps its entry - typically an attribute of the HTTP session. */
public interface SessionSlot {
    public fun get(): CachedPermissions?

    public fun put(value: CachedPermissions)
}

/**
 * Effective permissions cached in the server-side session and resolved again when the principal's
 * [PermissionVersions] moves on. A revocation takes effect on the next request after the version is
 * bumped, with one cheap version lookup per request instead of resolving grants.
 */
public class SessionCachedPermissions(
    private val principalId: () -> String?,
    private val slot: SessionSlot,
    private val versions: PermissionVersions,
    private val resolve: (String) -> EffectivePermissions,
) : EffectivePermissionsSource {
    override fun current(): EffectivePermissions? {
        val id = principalId() ?: return null
        // Read the version before resolving: a change made meanwhile bumps it again and the next
        // request resolves once more, instead of caching a stale snapshot under the new version.
        val version = versions.current(id)
        slot.get()?.takeIf { it.principalId == id && it.version == version }?.let { return it.permissions }
        return resolve(id).also { slot.put(CachedPermissions(id, version, it)) }
    }
}
