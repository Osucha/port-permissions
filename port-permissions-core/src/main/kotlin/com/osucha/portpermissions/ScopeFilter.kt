package com.osucha.portpermissions

/**
 * The scopes on which the current principal holds a scoped permission, for filtering lists:
 *
 * ```
 * when (val filter = guard.scopesWith(RUN_VIEW)) {
 *     ScopeFilter.All -> runs.findAll()
 *     is ScopeFilter.AllExcept -> runs.findByProjectNotIn(filter.excluded)
 *     is ScopeFilter.Only -> runs.findByProjectIn(filter.ids)   // empty set: nothing visible
 * }
 * ```
 */
public sealed interface ScopeFilter<out S : Any> {
    /** Every scope, e.g. an administrator of all projects. */
    public data object All : ScopeFilter<Nothing>

    /** Every scope except the listed ones - a global grant with per-scope revocations. */
    public data class AllExcept<S : Any>(
        val excluded: Set<S>,
    ) : ScopeFilter<S>

    /** Only the listed scopes. Empty when the permission is not held at all. */
    public data class Only<S : Any>(
        val ids: Set<S>,
    ) : ScopeFilter<S>
}

public fun <S : Any> ScopeFilter<S>.allows(id: S): Boolean =
    when (this) {
        ScopeFilter.All -> true
        is ScopeFilter.AllExcept -> id !in excluded
        is ScopeFilter.Only -> id in ids
    }
