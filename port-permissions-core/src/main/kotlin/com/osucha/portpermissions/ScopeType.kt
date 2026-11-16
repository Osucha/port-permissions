package com.osucha.portpermissions

/**
 * A kind of resource that permissions can be scoped to, e.g. a project. Declared once and shared by
 * every permission of that scope:
 *
 * ```
 * val PROJECT = ScopeType<ProjectId>("project", key = { it.value.toString() }, parse = { ProjectId(UUID.fromString(it)) })
 * ```
 *
 * [key] gives the stable string form stored in grants and snapshots, [parse] turns it back into the
 * consumer's id type, so list queries get typed ids. The two must round-trip.
 */
public class ScopeType<S : Any>(
    public val name: String,
    private val key: (S) -> String,
    private val parse: (String) -> S,
) {
    init {
        kotlin.require(NAME.matches(name)) { "Invalid scope name '$name', expected e.g. 'project'" }
    }

    public fun key(id: S): String = key.invoke(id)

    public fun parse(key: String): S = parse.invoke(key)

    override fun toString(): String = name

    private companion object {
        val NAME = Regex("[a-z][a-z0-9_]*")
    }
}
