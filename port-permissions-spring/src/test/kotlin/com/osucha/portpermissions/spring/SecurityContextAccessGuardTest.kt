package com.osucha.portpermissions.spring

import com.osucha.portpermissions.NotAuthenticatedException
import com.osucha.portpermissions.PermissionSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

object OrdersPermissions : PermissionSet("orders") {
    val VIEW = permission("view", "View orders")
    val CANCEL = permission("cancel", "Cancel orders")
}

class SecurityContextAccessGuardTest {
    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `reads permissions from authorities`() {
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
    fun `no authentication is not authenticated`() {
        shouldThrow<NotAuthenticatedException> { SecurityContextAccessGuard().require(OrdersPermissions.VIEW) }
    }

    @Test
    fun `anonymous authentication is not authenticated`() {
        set(AnonymousAuthenticationToken("key", "anonymous", listOf(SimpleGrantedAuthority("orders.view"))))

        shouldThrow<NotAuthenticatedException> { SecurityContextAccessGuard().require(OrdersPermissions.VIEW) }
    }

    private fun authenticate(vararg authorities: String) =
        set(UsernamePasswordAuthenticationToken.authenticated("alice", null, authorities.map(::SimpleGrantedAuthority)))

    private fun set(authentication: Authentication) {
        SecurityContextHolder.getContext().authentication = authentication
    }
}
