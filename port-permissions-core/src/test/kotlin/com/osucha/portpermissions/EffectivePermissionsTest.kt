package com.osucha.portpermissions

import com.osucha.portpermissions.ProjectsPermissions.PROJECTS_CREATE
import com.osucha.portpermissions.ProjectsPermissions.RUN_START
import com.osucha.portpermissions.ProjectsPermissions.RUN_VIEW
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EffectivePermissionsTest {
    private val p1 = ProjectId("p-1")
    private val p2 = ProjectId("p-2")
    private val p9 = ProjectId("p-9")

    @Test
    fun `scope filter is All for every scope`() {
        EffectivePermissions.of(RUN_VIEW.everywhere).scopesOf(RUN_VIEW) shouldBe ScopeFilter.All
    }

    @Test
    fun `scope filter is AllExcept with typed ids`() {
        val effective = EffectivePermissions(PrincipalKind.USER, scoped = mapOf(RUN_VIEW.key to ScopeSet.allExcept(setOf("p-9"))))

        effective.scopesOf(RUN_VIEW) shouldBe ScopeFilter.AllExcept(setOf(p9))
        effective.holds(RUN_VIEW, p1) shouldBe true
        effective.holds(RUN_VIEW, p9) shouldBe false
    }

    @Test
    fun `scope filter is Only with typed ids, empty when not held`() {
        val effective = EffectivePermissions.of(RUN_START on p1, RUN_START on p2)

        effective.scopesOf(RUN_START) shouldBe ScopeFilter.Only(setOf(p1, p2))
        effective.scopesOf(RUN_VIEW) shouldBe ScopeFilter.Only(emptySet())
    }

    @Test
    fun `filters answer membership`() {
        ScopeFilter.All.allows(p1) shouldBe true
        ScopeFilter.AllExcept(setOf(p9)).allows(p9) shouldBe false
        ScopeFilter.Only(setOf(p1)).allows(p2) shouldBe false
    }

    @Test
    fun `everywhere wins over single scopes in of`() {
        EffectivePermissions.of(RUN_START on p1, RUN_START.everywhere, RUN_START on p2).scoped[RUN_START.key] shouldBe ScopeSet.allExcept()
    }

    @Test
    fun `a delegated token holds only what both the user and its capabilities allow`() {
        val user =
            EffectivePermissions(
                PrincipalKind.USER,
                global = setOf(PROJECTS_CREATE.key),
                scoped = mapOf(RUN_START.key to ScopeSet.allExcept(setOf("p-9")), RUN_VIEW.key to ScopeSet.only(setOf("p-1"))),
            )
        val token = EffectivePermissions.of(RUN_START on p1, RUN_START on p9, RUN_VIEW on p2)

        val delegated = user.narrowedTo(token)

        delegated.principal shouldBe PrincipalKind.DELEGATED
        delegated.global shouldBe emptySet()
        delegated.scoped shouldBe mapOf(RUN_START.key to ScopeSet.only(setOf("p-1")))
    }

    @Test
    fun `intersection of two all-except sets excludes both`() {
        (ScopeSet.allExcept(setOf("a")) intersect ScopeSet.allExcept(setOf("b"))) shouldBe ScopeSet.allExcept(setOf("a", "b"))
    }

    @Test
    fun `a scope set is either all-except or only`() {
        shouldThrow<IllegalArgumentException> { ScopeSet(all = true, only = setOf("a")) }
    }
}
