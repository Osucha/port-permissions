package com.osucha.portpermissions

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PermissionCatalogTest {
    private val catalog = PermissionCatalog(listOf(CustomersPermissions, BillingPermissions))

    @Test
    fun `collects permissions of all modules ordered by module and key`() {
        catalog.permissions.map { it.key } shouldContainExactly
            listOf("billing.invoices.issue", "billing.invoices.view", "customers.view")
    }

    @Test
    fun `groups permissions by module`() {
        catalog.modules.keys.toList() shouldContainExactly listOf("billing", "customers")
        catalog.modules.getValue("billing") shouldContainExactly
            listOf(BillingPermissions.INVOICES_ISSUE, BillingPermissions.INVOICES_VIEW)
    }

    @Test
    fun `finds a permission by key`() {
        catalog["customers.view"] shouldBe CustomersPermissions.VIEW
        catalog["customers.edit"] shouldBe null
        ("billing.invoices.view" in catalog) shouldBe true
    }

    @Test
    fun `the same set registered twice is counted once`() {
        PermissionCatalog(listOf(BillingPermissions, BillingPermissions)).permissions.size shouldBe 2
    }

    @Test
    fun `two sets declaring the same key fail at startup`() {
        val clash = object : PermissionSet("billing") {
            val view = permission("invoices.view", "View")
        }

        val error = shouldThrow<DuplicatePermissionException> { PermissionCatalog(listOf(BillingPermissions, clash)) }

        error.keys shouldBe setOf("billing.invoices.view")
    }

    @Test
    fun `ensureKnown accepts declared keys`() {
        shouldNotThrowAny { catalog.ensureKnown(listOf("billing.invoices.view", "customers.view")) }
    }

    @Test
    fun `ensureKnown reports every unknown key`() {
        val error = shouldThrow<UnknownPermissionException> {
            catalog.ensureKnown(listOf("billing.invoices.view", "billing.invoices.void", "hr.salaries.view"))
        }

        error.keys shouldBe setOf("billing.invoices.void", "hr.salaries.view")
    }
}
