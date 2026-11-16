package com.osucha.portpermissions

import com.osucha.portpermissions.ProjectsPermissions.PROJECTS_CREATE
import com.osucha.portpermissions.ProjectsPermissions.RUN_START
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class AccessGuardTest {
    private val p1 = ProjectId("p-1")
    private val p2 = ProjectId("p-2")

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
        error.scope shouldBe null
    }

    @Test
    fun `missing principal is not authenticated rather than denied`() {
        val guard = DefaultAccessGuard { null }

        shouldThrow<NotAuthenticatedException> { guard.require(BillingPermissions.INVOICES_VIEW) }
    }

    @Test
    fun `scoped require checks the scope and reports it`() {
        val guard = AccessGuard.allowing(RUN_START on p1)

        shouldNotThrowAny { guard.require(RUN_START, p1) }
        val error = shouldThrow<PermissionDeniedException> { guard.require(RUN_START, p2) }

        error.permission shouldBe RUN_START
        error.scope shouldBe "p-2"
        error.message shouldBe "Missing permission: projects.run.start on project p-2"
    }

    @Test
    fun `scopesWith exposes the filter for list queries`() {
        AccessGuard.allowing(RUN_START on p1).scopesWith(RUN_START) shouldBe ScopeFilter.Only(setOf(p1))
        AccessGuard.allowing(RUN_START.everywhere).scopesWith(RUN_START) shouldBe ScopeFilter.All
    }

    @Test
    fun `allowing grants exactly the listed access, denyingAll nothing`() {
        val guard = AccessGuard.allowing(BillingPermissions.INVOICES_VIEW, RUN_START on p1)

        guard.isGranted(BillingPermissions.INVOICES_VIEW) shouldBe true
        guard.isGranted(BillingPermissions.INVOICES_ISSUE) shouldBe false
        AccessGuard.denyingAll().isGranted(BillingPermissions.INVOICES_VIEW) shouldBe false
    }

    @Test
    fun `the system principal holds no permissions`() {
        val guard = AccessGuard.allowing(PROJECTS_CREATE, RUN_START.everywhere)

        guard.asSystem("nightly cleanup") {
            guard.effective().principal shouldBe PrincipalKind.SYSTEM
            shouldThrow<PermissionDeniedException> { guard.require(PROJECTS_CREATE) }
            shouldThrow<PermissionDeniedException> { guard.require(RUN_START, p1) }
        }
    }

    @Test
    fun `requireSystem passes only inside asSystem`() {
        val guard = AccessGuard.allowing(PROJECTS_CREATE)

        val error = shouldThrow<SystemOnlyOperationException> { guard.requireSystem() }
        error.principal shouldBe PrincipalKind.USER
        guard.asSystem("seeding") { guard.requireSystem() }
        shouldThrow<SystemOnlyOperationException> { guard.requireSystem() }
    }

    @Test
    fun `requireSystem does not need an authenticated user inside asSystem`() {
        val guard = DefaultAccessGuard { null }

        guard.asSystem("queue worker") { shouldNotThrowAny { guard.requireSystem() } }
    }

    @Test
    fun `asSystem restores the outer state, also after an exception and when nested`() {
        val guard = DefaultAccessGuard { EffectivePermissions(PrincipalKind.USER) }

        shouldThrow<IllegalStateException> { guard.asSystem("failing job") { error("boom") } }
        guard.effective().principal shouldBe PrincipalKind.USER

        guard.asSystem("outer") {
            guard.asSystem("inner") { guard.systemReason() shouldBe "inner" }
            guard.systemReason() shouldBe "outer"
        }
        guard.systemReason() shouldBe null
    }

    @Test
    fun `asSystem is bound to the calling thread`() {
        val guard = DefaultAccessGuard { EffectivePermissions(PrincipalKind.USER) }
        val executor = Executors.newSingleThreadExecutor()

        val onOtherThread = guard.asSystem("job") { executor.submit<PrincipalKind> { guard.effective().principal }.get() }

        onOtherThread shouldBe PrincipalKind.USER
        executor.shutdown()
    }

    @Test
    fun `asSystem needs a reason`() {
        shouldThrow<IllegalArgumentException> { AccessGuard.denyingAll().asSystem(" ") {} }
    }
}
