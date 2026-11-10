package com.osucha.portpermissions

/**
 * A single permission owned by a module. The key has the form `module.name`, e.g.
 * `billing.invoices.view`, and is what gets stored in grants and carried in tokens.
 *
 * Instances are created only by [PermissionSet.permission], so every permission that a use case
 * can require is, by construction, also part of the catalog.
 */
public class Permission internal constructor(
    public val module: String,
    public val name: String,
    public val description: String,
) {
    public val key: String = "$module.$name"

    override fun equals(other: Any?): Boolean = other is Permission && other.key == key

    override fun hashCode(): Int = key.hashCode()

    override fun toString(): String = key
}
