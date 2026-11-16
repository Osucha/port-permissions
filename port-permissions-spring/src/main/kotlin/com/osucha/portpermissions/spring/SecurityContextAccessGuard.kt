package com.osucha.portpermissions.spring

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.DefaultAccessGuard
import com.osucha.portpermissions.EffectivePermissions
import com.osucha.portpermissions.EffectivePermissionsSource
import com.osucha.portpermissions.PermissionsPrincipal
import com.osucha.portpermissions.PrincipalKind
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * [EffectivePermissionsSource] over the Spring Security context:
 *
 * - a principal implementing [PermissionsPrincipal] supplies its snapshot, scopes included - the
 *   token strategy;
 * - otherwise the authorities are read as **global** permission keys, optionally with an
 *   [authorityPrefix] such as `PERM_`. Authorities cannot carry scopes.
 *
 * An anonymous or missing authentication gives null - not authenticated.
 */
public class SecurityContextPermissionsSource(
    private val authorityPrefix: String = "",
) : EffectivePermissionsSource {
    override fun current(): EffectivePermissions? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication is AnonymousAuthenticationToken) {
            return null
        }
        (authentication.principal as? PermissionsPrincipal)?.let { return it.effectivePermissions }
        val global =
            authentication.authorities
                .mapNotNull { it.authority }
                .filter { it.startsWith(authorityPrefix) }
                .map { it.removePrefix(authorityPrefix) }
                .toSet()
        return EffectivePermissions(PrincipalKind.USER, global)
    }
}

/** [AccessGuard] over [SecurityContextPermissionsSource], as in 0.1.0. */
public class SecurityContextAccessGuard(
    authorityPrefix: String = "",
) : AccessGuard by DefaultAccessGuard(SecurityContextPermissionsSource(authorityPrefix))
