package com.osucha.portpermissions

import com.osucha.portpermissions.GrantLevel.GROUP
import com.osucha.portpermissions.GrantLevel.SYSTEM
import com.osucha.portpermissions.GrantLevel.USER
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GrantResolverTest {
    private val resolver = GrantResolver(PermissionCatalog(listOf(BillingPermissions, CustomersPermissions)))
    private val view = BillingPermissions.INVOICES_VIEW.key
    private val issue = BillingPermissions.INVOICES_ISSUE.key

    @Test
    fun `no grants means no permissions`() {
        resolver.resolve(emptyList()).shouldBeEmpty()
    }

    @Test
    fun `system default applies when nothing overrides it`() {
        resolver.resolve(listOf(Grant(view, SYSTEM, true))) shouldBe setOf(view)
    }

    @Test
    fun `group overrides system`() {
        resolver.resolve(listOf(Grant(view, SYSTEM, true), Grant(view, GROUP, false))).shouldBeEmpty()
        resolver.resolve(listOf(Grant(issue, SYSTEM, false), Grant(issue, GROUP, true))) shouldBe setOf(issue)
    }

    @Test
    fun `user overrides group and system regardless of order`() {
        val grants = listOf(Grant(view, USER, true), Grant(view, SYSTEM, false), Grant(view, GROUP, false))

        resolver.resolve(grants) shouldBe setOf(view)
    }

    @Test
    fun `each permission is resolved independently`() {
        val grants = listOf(
            Grant(view, SYSTEM, true),
            Grant(issue, GROUP, true),
            Grant(issue, USER, false),
        )

        resolver.resolve(grants) shouldBe setOf(view)
    }

    @Test
    fun `grants for keys missing from the catalog are ignored`() {
        resolver.resolve(listOf(Grant("billing.invoices.void", USER, true))).shouldBeEmpty()
    }

    @Test
    fun `identical duplicates are tolerated`() {
        resolver.resolve(listOf(Grant(view, GROUP, true), Grant(view, GROUP, true))) shouldBe setOf(view)
    }

    @Test
    fun `conflicting grants on the same level are rejected`() {
        shouldThrow<IllegalArgumentException> {
            resolver.resolve(listOf(Grant(view, GROUP, true), Grant(view, GROUP, false)))
        }
    }
}
