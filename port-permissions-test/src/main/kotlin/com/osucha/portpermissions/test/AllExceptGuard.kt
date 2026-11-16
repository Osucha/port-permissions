package com.osucha.portpermissions.test

import com.osucha.portpermissions.Access
import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.AllScopesAccess
import com.osucha.portpermissions.DefaultAccessGuard
import com.osucha.portpermissions.EffectivePermissions
import com.osucha.portpermissions.Permission
import com.osucha.portpermissions.PrincipalKind
import com.osucha.portpermissions.ScopeFilter
import com.osucha.portpermissions.ScopedAccess
import com.osucha.portpermissions.ScopedPermission

/**
 * A user who holds every permission on every scope except [denied] (nothing denied when null). A
 * use case that checks anything other than [denied] passes the check and runs on - which is what
 * exposes a check on the wrong permission or scope.
 */
internal class AllExceptGuard(
    private val denied: Access?,
) : AccessGuard {
    private val system = DefaultAccessGuard { EffectivePermissions(PrincipalKind.USER) }

    private val isSystem: Boolean get() = system.systemReason() != null

    override fun effective(): EffectivePermissions = system.effective()

    override fun <T> asSystem(
        reason: String,
        block: () -> T,
    ): T = system.asSystem(reason, block)

    override fun isGranted(permission: Permission): Boolean = !isSystem && permission != denied

    override fun <S : Any> isGranted(
        permission: ScopedPermission<S>,
        scope: S,
    ): Boolean =
        !isSystem &&
            when (denied) {
                is AllScopesAccess<*> -> denied.permission != permission
                is ScopedAccess<*> -> denied.permission != permission || denied.scopeKey != permission.scopeType.key(scope)
                else -> true
            }

    @Suppress("UNCHECKED_CAST")
    override fun <S : Any> scopesWith(permission: ScopedPermission<S>): ScopeFilter<S> =
        when {
            isSystem -> ScopeFilter.Only(emptySet())
            denied is AllScopesAccess<*> && denied.permission == permission -> ScopeFilter.Only(emptySet())
            denied is ScopedAccess<*> && denied.permission == permission -> ScopeFilter.AllExcept(setOf(denied.scope as S))
            else -> ScopeFilter.All
        }
}
