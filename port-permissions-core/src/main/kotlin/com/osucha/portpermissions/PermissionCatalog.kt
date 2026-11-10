package com.osucha.portpermissions

/**
 * The central, read-only registry of every permission in the application, assembled from the
 * [PermissionSet]s of all modules. Code is the source of truth: the catalog is built in memory at
 * startup and never persisted - only grants live in the database.
 *
 * The management side (an admin screen, a grant editor) reads the catalog and validates
 * incoming grants against it, without importing anything from the modules that own the
 * permissions.
 */
public class PermissionCatalog(
    sets: Iterable<PermissionSet>,
) {
    private val byKey: Map<String, Permission>

    init {
        val all = sets.distinct().flatMap { it.permissions }
        val duplicates = all.groupBy { it.key }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw DuplicatePermissionException(duplicates)
        byKey = all.sortedWith(compareBy({ it.module }, { it.key })).associateBy { it.key }
    }

    /** All permissions, ordered by module and key. */
    public val permissions: List<Permission>
        get() = byKey.values.toList()

    /** Permissions grouped by owning module, for rendering a management screen. */
    public val modules: Map<String, List<Permission>>
        get() = permissions.groupBy { it.module }

    public operator fun get(key: String): Permission? = byKey[key]

    public operator fun contains(key: String): Boolean = key in byKey

    /**
     * Fails fast when a grant is about to be stored for a key that no module declares. There is
     * no foreign key between grants and the catalog, so this check is what keeps them consistent
     * on write.
     */
    public fun ensureKnown(keys: Collection<String>) {
        val unknown = keys.filterNot { it in byKey }.toSet()
        if (unknown.isNotEmpty()) throw UnknownPermissionException(unknown)
    }
}
