package com.osucha.portpermissions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class PermissionSetTest {
    @Test
    fun `key is prefixed with the owning module`() {
        BillingPermissions.INVOICES_VIEW.key shouldBe "billing.invoices.view"
        BillingPermissions.INVOICES_VIEW.module shouldBe "billing"
    }

    @Test
    fun `set lists permissions in declaration order`() {
        BillingPermissions.permissions.map { it.key } shouldContainExactly
            listOf("billing.invoices.view", "billing.invoices.issue")
    }

    @Test
    fun `permissions are equal by key`() {
        val again = object : PermissionSet("billing") {
            val view = permission("invoices.view", "Another description")
        }

        again.view shouldBe BillingPermissions.INVOICES_VIEW
        again.view shouldNotBe BillingPermissions.INVOICES_ISSUE
    }

    @Test
    fun `declaring the same permission twice fails`() {
        shouldThrow<IllegalStateException> {
            object : PermissionSet("billing") {
                val first = permission("invoices.view", "View")
                val second = permission("invoices.view", "View again")
            }
        }
    }

    @Test
    fun `invalid names are rejected`() {
        shouldThrow<IllegalArgumentException> { object : PermissionSet("Billing") {} }
        shouldThrow<IllegalArgumentException> {
            object : PermissionSet("billing") {
                val bad = permission("invoices..view", "View")
            }
        }
    }

    @Test
    fun `description is required`() {
        shouldThrow<IllegalArgumentException> {
            object : PermissionSet("billing") {
                val blank = permission("invoices.view", " ")
            }
        }
    }
}
