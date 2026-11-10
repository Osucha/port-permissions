package com.osucha.portpermissions

/**
 * The permissions a module exposes. Each module declares what it protects, next to its use
 * cases, and hands the set over to the central [PermissionCatalog]:
 *
 * ```
 * object BillingPermissions : PermissionSet("billing") {
 *     val INVOICES_VIEW = permission("invoices.view", "View invoices")
 *     val INVOICES_ISSUE = permission("invoices.issue", "Issue invoices")
 * }
 * ```
 *
 * Declare permissions as eagerly initialized properties (an `object` is the natural fit) - a
 * permission becomes part of the set at the moment its property is initialized.
 */
public abstract class PermissionSet(
    public val module: String,
) {
    private val declared = linkedMapOf<String, Permission>()

    init {
        require(SEGMENTS.matches(module)) { "Invalid module name '$module', expected e.g. 'billing'" }
    }

    /** All permissions declared by this set, in declaration order. */
    public val permissions: List<Permission>
        get() = declared.values.toList()

    protected fun permission(
        name: String,
        description: String,
    ): Permission {
        require(SEGMENTS.matches(name)) { "Invalid permission name '$name', expected e.g. 'invoices.view'" }
        require(description.isNotBlank()) { "Permission $module.$name needs a description" }
        val permission = Permission(module, name, description)
        check(declared.putIfAbsent(permission.key, permission) == null) {
            "Permission ${permission.key} is declared twice"
        }
        return permission
    }

    private companion object {
        val SEGMENTS = Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")
    }
}
