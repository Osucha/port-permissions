package com.osucha.portpermissions

import com.osucha.portpermissions.GrantLevel.GROUP
import com.osucha.portpermissions.GrantLevel.SYSTEM
import com.osucha.portpermissions.GrantLevel.USER
import com.osucha.portpermissions.ProjectsPermissions.PROJECTS_CREATE
import com.osucha.portpermissions.ProjectsPermissions.RUN_START
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GrantResolverTest {
    private val resolver =
        GrantResolver(PermissionCatalog(listOf(BillingPermissions, CustomersPermissions, ProjectsPermissions)))
    private val view = BillingPermissions.INVOICES_VIEW.key
    private val issue = BillingPermissions.INVOICES_ISSUE.key
    private val start = RUN_START.key
    private val unionOfGroups =
        GrantResolver(PermissionCatalog(listOf(BillingPermissions, CustomersPermissions, ProjectsPermissions)), SameLevelConflict.GRANT_WINS)

    private fun global(vararg grants: Grant) = resolver.resolve(grants.toList()).global

    private fun scopes(vararg grants: Grant) = resolver.resolve(grants.toList()).scoped[start]

    @Nested
    inner class GlobalPermissions {
        @Test
        fun `no grants means no permissions`() {
            resolver.resolve(emptyList()).global.shouldBeEmpty()
        }

        @Test
        fun `system default applies when nothing overrides it`() {
            global(Grant(view, SYSTEM, true)) shouldBe setOf(view)
        }

        @Test
        fun `group overrides system`() {
            global(Grant(view, SYSTEM, true), Grant(view, GROUP, false)).shouldBeEmpty()
            global(Grant(issue, SYSTEM, false), Grant(issue, GROUP, true)) shouldBe setOf(issue)
        }

        @Test
        fun `user overrides group and system regardless of order`() {
            global(Grant(view, USER, true), Grant(view, SYSTEM, false), Grant(view, GROUP, false)) shouldBe setOf(view)
        }

        @Test
        fun `each permission is resolved independently`() {
            global(Grant(view, SYSTEM, true), Grant(issue, GROUP, true), Grant(issue, USER, false)) shouldBe setOf(view)
        }

        @Test
        fun `grants that no longer match the catalog are ignored`() {
            val effective =
                resolver.resolve(
                    listOf(Grant("billing.invoices.void", USER, true), Grant(PROJECTS_CREATE.key, USER, true, scope = "p-1")),
                )

            effective.global.shouldBeEmpty()
            effective.scoped.shouldBeEmpty()
        }

        @Test
        fun `identical duplicates are tolerated`() {
            global(Grant(view, GROUP, true), Grant(view, GROUP, true)) shouldBe setOf(view)
        }

        @Test
        fun `two groups with opposite decisions - the revocation wins by default`() {
            global(Grant(view, GROUP, true), Grant(view, GROUP, false)).shouldBeEmpty()
            global(Grant(view, GROUP, false), Grant(view, GROUP, true)).shouldBeEmpty()
        }

        @Test
        fun `two groups with opposite decisions - the grant wins when configured`() {
            unionOfGroups.resolve(listOf(Grant(view, GROUP, false), Grant(view, GROUP, true))).global shouldBe setOf(view)
        }

        @Test
        fun `a same-level conflict does not override a higher level`() {
            global(Grant(view, GROUP, true), Grant(view, GROUP, false), Grant(view, USER, true)) shouldBe setOf(view)
            unionOfGroups.resolve(listOf(Grant(view, GROUP, true), Grant(view, GROUP, false), Grant(view, USER, false))).global
                .shouldBeEmpty()
        }

        @Test
        fun `principal kind is carried into the snapshot`() {
            resolver.resolve(emptyList(), PrincipalKind.MACHINE).principal shouldBe PrincipalKind.MACHINE
        }
    }

    @Nested
    inner class ScopedPermissions {
        @Test
        fun `grants on scopes give exactly those scopes`() {
            scopes(Grant(start, GROUP, true, "p-1"), Grant(start, USER, true, "p-2")) shouldBe ScopeSet.only(setOf("p-1", "p-2"))
        }

        @Test
        fun `a grant without scope gives every scope`() {
            scopes(Grant(start, GROUP, true)) shouldBe ScopeSet.allExcept()
        }

        @Test
        fun `a scoped revocation on a higher level excludes that scope`() {
            scopes(Grant(start, GROUP, true), Grant(start, USER, false, "p-9")) shouldBe ScopeSet.allExcept(setOf("p-9"))
        }

        @Test
        fun `on the same level the scoped grant beats the one without scope`() {
            scopes(Grant(start, GROUP, true), Grant(start, GROUP, false, "p-9")) shouldBe ScopeSet.allExcept(setOf("p-9"))
            scopes(Grant(start, GROUP, false), Grant(start, GROUP, true, "p-1")) shouldBe ScopeSet.only(setOf("p-1"))
        }

        @Test
        fun `a higher level without scope beats a scoped grant - user revocation is a switch`() {
            scopes(Grant(start, GROUP, true, "p-1"), Grant(start, USER, false)) shouldBe null
        }

        @Test
        fun `a higher level grant without scope overrides a lower scoped revocation`() {
            scopes(Grant(start, SYSTEM, false, "p-1"), Grant(start, GROUP, true)) shouldBe ScopeSet.allExcept()
        }

        @Test
        fun `per scope the most specific level wins`() {
            scopes(Grant(start, GROUP, true, "p-1"), Grant(start, USER, false, "p-1"), Grant(start, SYSTEM, true, "p-2")) shouldBe
                ScopeSet.only(setOf("p-2"))
        }

        @Test
        fun `two groups with opposite decisions on one scope - the revocation wins by default`() {
            scopes(Grant(start, GROUP, true, "p-1"), Grant(start, GROUP, false, "p-1")) shouldBe null
            scopes(Grant(start, GROUP, true), Grant(start, GROUP, true, "p-1"), Grant(start, GROUP, false, "p-1")) shouldBe
                ScopeSet.allExcept(setOf("p-1"))
        }

        @Test
        fun `two groups with opposite decisions on every scope - the revocation wins by default`() {
            scopes(Grant(start, GROUP, true), Grant(start, GROUP, false)) shouldBe null
        }

        @Test
        fun `two groups with opposite decisions on one scope - the grant wins when configured`() {
            unionOfGroups.resolve(listOf(Grant(start, GROUP, false, "p-1"), Grant(start, GROUP, true, "p-1"))).scoped[start] shouldBe
                ScopeSet.only(setOf("p-1"))
            unionOfGroups.resolve(listOf(Grant(start, GROUP, false), Grant(start, GROUP, true))).scoped[start] shouldBe
                ScopeSet.allExcept()
        }

        @Test
        fun `scoped grants do not leak into global permissions`() {
            resolver.resolve(listOf(Grant(start, USER, true, "p-1"))).global.shouldBeEmpty()
        }
    }
}
