package com.osucha.portpermissions.test

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.PermissionSet
import com.osucha.portpermissions.ScopeType
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

private val PROJECT = ScopeType<String>("project", key = { it }, parse = { it })

private object Runs : PermissionSet("runs") {
    val START = scoped("start", "Start runs", PROJECT)
    val VIEW = scoped("view", "View runs", PROJECT)
    val PURGE = permission("purge", "Purge old runs")
}

/** Each use case below has a different defect - or none. */
private class Store {
    val saved = mutableListOf<String>()
}

private fun correct(
    guard: AccessGuard,
    store: Store,
    project: String,
) {
    guard.require(Runs.START, project)
    store.saved += project
}

private fun checkAfterWrite(
    guard: AccessGuard,
    store: Store,
    project: String,
) {
    store.saved += project
    guard.require(Runs.START, project)
}

private fun wrongPermission(
    guard: AccessGuard,
    store: Store,
    project: String,
) {
    guard.require(Runs.VIEW, project)
    store.saved += project
}

private fun wrongScope(
    guard: AccessGuard,
    store: Store,
    project: String,
) {
    guard.require(Runs.START, "default")
    store.saved += project
}

private fun noCheck(
    @Suppress("UNUSED_PARAMETER") guard: AccessGuard,
    store: Store,
    project: String,
) {
    store.saved += project
}

private fun systemPurge(
    guard: AccessGuard,
    store: Store,
) {
    guard.requireSystem()
    store.saved += "purged"
}

class TestKitTest {
    private val store = Store()

    @Test
    fun `assertGuardedBy passes a use case that checks the right thing first`() {
        shouldNotThrowAny {
            assertGuardedBy(Runs.START on "a", touched = { store.saved.isNotEmpty() }) { correct(it, store, "a") }
        }
    }

    @Test
    fun `assertGuardedBy catches a check placed after a write`() {
        val error =
            shouldThrow<AssertionError> {
                assertGuardedBy(Runs.START on "a", touched = { store.saved.isNotEmpty() }) { checkAfterWrite(it, store, "a") }
            }

        error.message shouldContain "side effect before the check"
    }

    @Test
    fun `assertGuardedBy catches a check on the wrong permission`() {
        val error =
            shouldThrow<AssertionError> {
                assertGuardedBy(Runs.START on "a", touched = { store.saved.isNotEmpty() }) { wrongPermission(it, store, "a") }
            }

        error.message shouldContain "so it checks something else"
    }

    @Test
    fun `assertGuardedBy catches a check on the wrong scope`() {
        shouldThrow<AssertionError> {
            assertGuardedBy(Runs.START on "a", touched = { store.saved.isNotEmpty() }) { wrongScope(it, store, "a") }
        }
    }

    @Test
    fun `assertGuardedBy catches a missing check`() {
        val error =
            shouldThrow<AssertionError> {
                assertGuardedBy(Runs.START on "a", touched = { false }) { noCheck(it, store, "a") }
            }

        error.message shouldContain "is the check missing"
    }

    @Test
    fun `assertGuardedBy reports a check on another scope even when nothing is written`() {
        val error =
            shouldThrow<AssertionError> {
                assertGuardedBy(Runs.START on "a", touched = { false }) { it.require(Runs.START, "b") }
            }

        error.message shouldContain "everything granted except runs.start on a, so it checks something else"
    }

    @Test
    fun `assertSystemOnly passes a system operation and catches a missing requireSystem`() {
        shouldNotThrowAny { assertSystemOnly(touched = { store.saved.isNotEmpty() }) { systemPurge(it, store) } }
        shouldThrow<AssertionError> {
            assertSystemOnly(touched = { store.saved.isNotEmpty() }) { store.saved += "purged" }
        }
    }

    @Test
    fun `recording guard records exactly what was checked`() {
        val guard = RecordingAccessGuard()

        correct(guard, store, "a")
        guard.asSystem("cleanup") { systemPurge(guard, store) }

        guard.checks shouldBe listOf(RecordedCheck.of(Runs.START on "a"), RecordedCheck.System)
    }

    @Test
    fun `recording guard passes checks on to its delegate`() {
        val guard = RecordingAccessGuard(AccessGuard.allowing(Runs.PURGE))

        shouldThrow<com.osucha.portpermissions.PermissionDeniedException> { correct(guard, store, "a") }
        guard.checks shouldBe listOf(RecordedCheck.of(Runs.START on "a"))
        store.saved shouldBe emptyList()
    }
}
