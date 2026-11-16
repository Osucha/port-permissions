package com.osucha.portpermissions.test

import com.osucha.portpermissions.Access
import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.AllScopesAccess
import com.osucha.portpermissions.Permission
import com.osucha.portpermissions.PermissionDeniedException
import com.osucha.portpermissions.ScopedAccess
import com.osucha.portpermissions.SystemOnlyOperationException

/**
 * Asserts that [operation] is guarded by exactly [expected] and that the check happens before any
 * side effect. [touched] is a probe the consumer controls - a recording repository, a flag - that
 * returns true once the operation has written anything.
 *
 * The operation runs twice, each time with a fresh guard passed in:
 * 1. a guard denying everything - it must throw [PermissionDeniedException] with nothing touched,
 *    which catches a missing check and a check placed after a write;
 * 2. a guard granting everything except [expected] - it must throw for exactly [expected], again
 *    with nothing touched, which catches a check on the wrong permission or scope.
 *
 * ```
 * assertGuardedBy(RUN_START on projectA, touched = { runs.saved.isNotEmpty() }) { guard ->
 *     StartRunService(guard, runs).start(StartRun(projectA))
 * }
 * ```
 */
public fun assertGuardedBy(
    expected: Access,
    touched: () -> Boolean,
    operation: (AccessGuard) -> Any?,
) {
    val (permission, scope) =
        when (expected) {
            is Permission -> expected to null
            is ScopedAccess<*> -> expected.permission to expected.scopeKey
            is AllScopesAccess<*> -> throw IllegalArgumentException("Expect a permission on one scope, e.g. PERMISSION on id")
        }

    deniedBy(AccessGuard.denyingAll(), operation, "It completed with a guard denying everything - is the check missing?")
    untouched(touched, "before the check (guard denying everything) - the check must come first")

    val withAllButExpected =
        deniedBy(AllExceptGuard(expected), operation, "It completed with everything granted except $expected, so it checks something else.")
    untouched(touched, "with a guard granting everything except $expected - the operation checks something else")
    if (withAllButExpected.permission != permission || withAllButExpected.scope != scope) {
        throw AssertionError("Expected the check $expected, but the operation was denied ${describe(withAllButExpected)}")
    }
}

/**
 * Asserts that a [com.osucha.portpermissions.SystemOperation] refuses to run for a user who holds
 * every permission, without touching anything.
 */
public fun assertSystemOnly(
    touched: () -> Boolean,
    operation: (AccessGuard) -> Any?,
) {
    val result = runCatching { operation(AllExceptGuard(null)) }
    untouched(touched, "outside asSystem - requireSystem() must come first")
    if (result.exceptionOrNull() !is SystemOnlyOperationException) {
        throw AssertionError("Expected SystemOnlyOperationException outside asSystem, got ${result.exceptionOrNull() ?: "no exception"}")
    }
}

private fun deniedBy(
    guard: AccessGuard,
    operation: (AccessGuard) -> Any?,
    completed: String,
): PermissionDeniedException =
    try {
        operation(guard)
        throw AssertionError("Expected PermissionDeniedException. $completed")
    } catch (denied: PermissionDeniedException) {
        denied
    }

private fun untouched(
    touched: () -> Boolean,
    situation: String,
) {
    if (touched()) throw AssertionError("The operation had a side effect $situation")
}

private fun describe(denied: PermissionDeniedException): String = denied.permission.key + (denied.scope?.let { " on $it" } ?: "")
