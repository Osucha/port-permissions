package com.osucha.portpermissions.spring

import com.osucha.portpermissions.EffectivePermissions
import com.osucha.portpermissions.NotAuthenticatedException
import com.osucha.portpermissions.PermissionSet
import com.osucha.portpermissions.PermissionsPrincipal
import com.osucha.portpermissions.PrincipalKind
import com.osucha.portpermissions.ScopeType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

val STORE = ScopeType<Int>("store", key = Int::toString, parse = String::toInt)

object OrdersPermissions : PermissionSet("orders") {
    val VIEW = permission("view", "View orders")
    val CANCEL = permission("cancel", "Cancel orders")
    val MANAGE = scoped("manage", "Manage orders of a store", STORE)
}

/** A principal rebuilt from a token that carries the snapshot. */
data class TokenPrincipal(
    override val effectivePermissions: EffectivePermissions,
) : PermissionsPrincipal

class SecurityContextAccessGuardTest {
    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `reads global permissions from authorities`() {
        authenticate("orders.view", "ROLE_USER")
        val guard = SecurityContextAccessGuard()

        guard.isGranted(OrdersPermissions.VIEW) shouldBe true
        guard.isGranted(OrdersPermissions.CANCEL) shouldBe false
    }

    @Test
    fun `honours the authority prefix`() {
        authenticate("PERM_orders.cancel", "orders.view")
        val guard = SecurityContextAccessGuard(authorityPrefix = "PERM_")

        guard.isGranted(OrdersPermissions.CANCEL) shouldBe true
        guard.isGranted(OrdersPermissions.VIEW) shouldBe false
    }

    @Test
    fun `authorities never grant scoped permissions`() {
        authenticate("orders.manage")

        SecurityContextAccessGuard().isGranted(OrdersPermissions.MANAGE, 1) shouldBe false
    }

    @Test
    fun `a principal carrying a snapshot supplies scopes and its kind`() {
        val snapshot = EffectivePermissions.of(OrdersPermissions.MANAGE on 3, principal = PrincipalKind.MACHINE)
        set(UsernamePasswordAuthenticationToken.authenticated(TokenPrincipal(snapshot), null, emptyList()))
        val guard = SecurityContextAccessGuard()

        guard.isGranted(OrdersPermissions.MANAGE, 3) shouldBe true
        guard.isGranted(OrdersPermissions.MANAGE, 4) shouldBe false
        guard.effective().principal shouldBe PrincipalKind.MACHINE
    }

    @Test
    fun `no authentication is not authenticated`() {
        shouldThrow<NotAuthenticatedException> { SecurityContextAccessGuard().require(OrdersPermissions.VIEW) }
    }

    @Test
    fun `anonymous authentication is not authenticated`() {
        set(AnonymousAuthenticationToken("key", "anonymous", listOf(SimpleGrantedAuthority("orders.view"))))

        shouldThrow<NotAuthenticatedException> { SecurityContextAccessGuard().require(OrdersPermissions.VIEW) }
    }

    @Test
    fun `asSystem works without an authentication`() {
        val guard = SecurityContextAccessGuard()

        guard.asSystem("scheduler") { guard.requireSystem() }
    }

    private fun authenticate(vararg authorities: String) =
        set(UsernamePasswordAuthenticationToken.authenticated("alice", null, authorities.map(::SimpleGrantedAuthority)))

    private fun set(authentication: Authentication) {
        SecurityContextHolder.getContext().authentication = authentication
    }
}
