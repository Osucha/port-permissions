package com.osucha.portpermissions

/**
 * Level at which a permission is granted or revoked, declared from the lowest to the highest
 * precedence. SYSTEM is the default for everyone, GROUP overrides it for members of a group, USER
 * overrides both for one person.
 */
public enum class GrantLevel {
    SYSTEM,
    GROUP,
    USER,
}

/**
 * A stored decision: [permission] (a catalog key) is granted or explicitly revoked at [level].
 * For a scoped permission, [scope] is the scope key, or null for every scope.
 */
public data class Grant(
    val permission: String,
    val level: GrantLevel,
    val granted: Boolean,
    val scope: String? = null,
)

/**
 * Turns the grants collected for one principal into [EffectivePermissions].
 *
 * Precedence, for a permission on a given scope: the candidates are the grants without a scope and
 * the grants for that scope. The most specific level wins (USER > GROUP > SYSTEM); on the same level
 * a scoped grant beats a grant without a scope. A USER-level revocation without a scope therefore
 * switches the permission off everywhere. The permission is held only if the winning grant says
 * `granted` - no grant at all means no permission (deny by default).
 *
 * Opposite decisions on the same level and with the same specificity - typically two groups of the
 * user, one granting and one revoking - are settled by [sameLevelConflict]: by default the
 * revocation wins ([SameLevelConflict.DENY_WINS]); [SameLevelConflict.GRANT_WINS] gives the union of
 * group permissions instead. Such a conflict arises only from combining memberships, so it cannot be
 * rejected on write.
 *
 * Grants that no longer match the catalog - an unknown key, or a scope on a global permission - are
 * ignored, so a permission removed from code does not break login. Validate on write with
 * [PermissionCatalog.ensureValid].
 */
public class GrantResolver(
    private val catalog: PermissionCatalog,
    private val sameLevelConflict: SameLevelConflict = SameLevelConflict.DENY_WINS,
) {
    public fun resolve(
        grants: Iterable<Grant>,
        principal: PrincipalKind = PrincipalKind.USER,
    ): EffectivePermissions {
        val global = mutableSetOf<String>()
        val scoped = mutableMapOf<String, ScopeSet>()
        for ((key, forPermission) in grants.groupBy { it.permission }) {
            when (catalog[key]) {
                is Permission -> if (winner(forPermission.filter { it.scope == null })?.granted == true) global += key
                is ScopedPermission<*> -> scopesOf(forPermission)?.let { scoped[key] = it }
                null -> Unit
            }
        }
        return EffectivePermissions(principal, global, scoped)
    }

    private fun scopesOf(grants: List<Grant>): ScopeSet? {
        val everywhere = winner(grants.filter { it.scope == null })
        val perScope =
            grants
                .filter { it.scope != null }
                .groupBy { it.scope!! }
                .mapValues { (_, forScope) -> winner(forScope)!! }
                .mapValues { (_, scoped) -> if (everywhere != null && everywhere.level > scoped.level) everywhere.granted else scoped.granted }
        val set =
            if (everywhere?.granted == true) {
                ScopeSet.allExcept(perScope.filterValues { !it }.keys)
            } else {
                ScopeSet.only(perScope.filterValues { it }.keys)
            }
        return set.takeUnless { it.isEmpty }
    }

    private fun winner(grants: List<Grant>): Grant? {
        if (grants.isEmpty()) return null
        val top = grants.maxOf { it.level }
        val atTop = grants.filter { it.level == top }
        val preferred = sameLevelConflict == SameLevelConflict.GRANT_WINS
        return atTop.firstOrNull { it.granted == preferred } ?: atTop.first()
    }
}

/** How [GrantResolver] settles opposite decisions on the same level and with the same specificity. */
public enum class SameLevelConflict {
    /** The revocation wins - the safe default. */
    DENY_WINS,

    /** The grant wins - permissions of all groups add up. */
    GRANT_WINS,
}
