package com.osucha.portpermissions

/**
 * Something a test fixture or a machine token can be granted: a global [Permission], a scoped
 * permission on one scope ([ScopedAccess]) or on every scope ([AllScopesAccess]).
 */
public sealed interface Access

/**
 * Common shape of every permission in the catalog. The key has the form `module.name`, e.g.
 * `billing.invoices.view`, and is what gets stored in grants and carried in snapshots.
 *
 * Instances are created only by [PermissionSet], so every permission a use case can require is, by
 * construction, also part of the catalog.
 */
public sealed class PermissionDefinition(
    public val module: String,
    public val name: String,
    public val description: String,
) {
    public val key: String = "$module.$name"

    /** Name of the scope type (e.g. `project`) for a scoped permission, null for a global one. */
    public abstract val scope: String?

    override fun equals(other: Any?): Boolean = other is PermissionDefinition && other.key == key

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key
}

/** A global permission: held or not, regardless of any resource. */
public class Permission internal constructor(
    module: String,
    name: String,
    description: String,
) : PermissionDefinition(module, name, description),
    Access {
    override val scope: String? get() = null
}

/**
 * A permission held per resource of type [S], e.g. "start runs" on a given project. It can only be
 * required together with a scope of the right type: `guard.require(RUN_START, projectId)`.
 *
 * The type parameter is invariant on purpose - it pins [S] so that a scope of another type does not
 * compile.
 */
public class ScopedPermission<S : Any> internal constructor(
    module: String,
    name: String,
    description: String,
    public val scopeType: ScopeType<S>,
) : PermissionDefinition(module, name, description) {
    override val scope: String get() = scopeType.name

    /** This permission on one scope. */
    public infix fun on(scope: S): ScopedAccess<S> = ScopedAccess(this, scope)

    /** This permission on every scope, e.g. for an administrator of all projects. */
    public val everywhere: AllScopesAccess<S> get() = AllScopesAccess(this)
}

public data class ScopedAccess<S : Any>(
    val permission: ScopedPermission<S>,
    val scope: S,
) : Access {
    val scopeKey: String get() = permission.scopeType.key(scope)

    override fun toString(): String = "$permission on $scopeKey"
}

public data class AllScopesAccess<S : Any>(
    val permission: ScopedPermission<S>,
) : Access {
    override fun toString(): String = "$permission everywhere"
}
