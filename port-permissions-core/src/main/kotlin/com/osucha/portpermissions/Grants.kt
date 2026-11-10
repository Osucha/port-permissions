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

/** A stored decision: [permission] (a catalog key) is granted or explicitly revoked at [level]. */
public data class Grant(
    val permission: String,
    val level: GrantLevel,
    val granted: Boolean,
)

/**
 * Turns the grants collected for one user into the set of effective permission keys.
 *
 * For each permission the grant from the most specific level wins, and the permission is
 * effective only if that winning grant says `granted`. No grant at any level means no permission
 * (deny by default). Grants for keys that are no longer in the catalog - leftovers after a
 * permission was removed from code - are ignored.
 */
public class GrantResolver(
    private val catalog: PermissionCatalog,
) {
    public fun resolve(grants: Iterable<Grant>): Set<String> =
        grants
            .groupBy { it.permission }
            .filterKeys { it in catalog }
            .filterValues { winner(it).granted }
            .keys

    private fun winner(grants: List<Grant>): Grant {
        val top = grants.maxOf { it.level }
        val atTop = grants.filter { it.level == top }
        require(atTop.map { it.granted }.distinct().size == 1) {
            "Conflicting grants for ${atTop.first().permission} at level $top"
        }
        return atTop.first()
    }
}
