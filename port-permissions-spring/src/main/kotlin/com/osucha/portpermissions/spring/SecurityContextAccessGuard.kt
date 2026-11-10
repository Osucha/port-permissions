package com.osucha.portpermissions.spring

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.GrantedPermissionsAccessGuard
import com.osucha.portpermissions.Permission
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * [AccessGuard] backed by the Spring Security context. Effective permission keys are expected
 * among the authorities of the current authentication - put them there when the token or session
 * is turned into an `Authentication`, optionally with an [authorityPrefix] such as `PERM_`.
 *
 * An anonymous or missing authentication is reported as not authenticated, not as denied.
 */
public class SecurityContextAccessGuard(
    private val authorityPrefix: String = "",
) : AccessGuard {
    private val delegate = GrantedPermissionsAccessGuard(::currentPermissions)

    override fun isGranted(permission: Permission): Boolean = delegate.isGranted(permission)

    private fun currentPermissions(): Set<String>? {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated || authentication is AnonymousAuthenticationToken) {
            return null
        }
        return authentication.authorities
            .mapNotNull { it.authority }
            .filter { it.startsWith(authorityPrefix) }
            .map { it.removePrefix(authorityPrefix) }
            .toSet()
    }
}
