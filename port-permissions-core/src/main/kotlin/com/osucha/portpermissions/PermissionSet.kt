package com.osucha.portpermissions

/**
 * The permissions a module exposes. Each module declares what it protects, next to its use
 * cases, and hands the set over to the central [PermissionCatalog]:
 *
 * ```
 * object ProjectsPermissions : PermissionSet("projects") {
 *     val PROJECTS_CREATE = permission("projects.create", "Create projects")      // Permission
 *     val RUN_START = scoped("run.start", "Start and retry runs", PROJECT)        // ScopedPermission<ProjectId>
 * }
 * ```
 *
 * Declare permissions as eagerly initialized properties (an `object` is the natural fit) - a
 * permission becomes part of the set at the moment its property is initialized.
 */
public abstract class PermissionSet(
    public val module: String,
) {
    private val declared = linkedMapOf<String, PermissionDefinition>()

    init {
        kotlin.require(SEGMENTS.matches(module)) { "Invalid module name '$module', expected e.g. 'billing'" }
    }

    /** All permissions declared by this set, in declaration order. */
    public val permissions: List<PermissionDefinition>
        get() = declared.values.toList()

    protected fun permission(
        name: String,
        description: String,
    ): Permission = register(Permission(module, validName(name), validDescription(name, description)))

    protected fun <S : Any> scoped(
        name: String,
        description: String,
        scope: ScopeType<S>,
    ): ScopedPermission<S> = register(ScopedPermission(module, validName(name), validDescription(name, description), scope))

    private fun <P : PermissionDefinition> register(permission: P): P {
        check(declared.putIfAbsent(permission.key, permission) == null) {
            "Permission ${permission.key} is declared twice"
        }
        return permission
    }

    private fun validName(name: String): String {
        kotlin.require(SEGMENTS.matches(name)) { "Invalid permission name '$name', expected e.g. 'invoices.view'" }
        return name
    }

    private fun validDescription(
        name: String,
        description: String,
    ): String {
        kotlin.require(description.isNotBlank()) { "Permission $module.$name needs a description" }
        return description
    }

    private companion object {
        val SEGMENTS = Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")
    }
}
