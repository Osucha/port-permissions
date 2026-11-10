package com.osucha.portpermissions

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AccessGuardTest {
    @Test
    fun `require passes when the permission is granted`() {
        val guard = GrantedPermissionsAccessGuard { setOf("billing.invoices.view") }

        shouldNotThrowAny { guard.require(BillingPermissions.INVOICES_VIEW) }
    }

    @Test
    fun `require reports the missing permission`() {
        val guard = GrantedPermissionsAccessGuard { setOf("billing.invoices.view") }

        val error = shouldThrow<PermissionDeniedException> { guard.require(BillingPermissions.INVOICES_ISSUE) }

        error.permission shouldBe BillingPermissions.INVOICES_ISSUE
    }

    @Test
    fun `missing principal is not authenticated rather than denied`() {
        val guard = GrantedPermissionsAccessGuard { null }

        shouldThrow<NotAuthenticatedException> { guard.require(BillingPermissions.INVOICES_VIEW) }
    }

    @Test
    fun `allowing grants exactly the listed permissions`() {
        val guard = AccessGuard.allowing(BillingPermissions.INVOICES_VIEW)

        guard.isGranted(BillingPermissions.INVOICES_VIEW) shouldBe true
        guard.isGranted(BillingPermissions.INVOICES_ISSUE) shouldBe false
    }
}
