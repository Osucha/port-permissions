package com.osucha.portpermissions.test

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.EffectivePermissions
import com.osucha.portpermissions.Permission
import com.osucha.portpermissions.PermissionDefinition
import com.osucha.portpermissions.ScopeFilter
import com.osucha.portpermissions.ScopedAccess
import com.osucha.portpermissions.ScopedPermission

/** A check a use case made, as recorded by [RecordingAccessGuard]. */
public sealed interface RecordedCheck {
    public data class Required(
        val permission: PermissionDefinition,
        val scope: String? = null,
    ) : RecordedCheck

    public data object System : RecordedCheck

    public companion object {
        public fun of(permission: Permission): RecordedCheck = Required(permission)

        public fun of(access: ScopedAccess<*>): RecordedCheck = Required(access.permission, access.scopeKey)
    }
}

/**
 * Records every `require` / `requireSystem` call and passes it on to [delegate], which by default
 * grants everything. Lets a use case test assert exactly what was checked:
 *
 * ```
 * val guard = RecordingAccessGuard()
 * StartRunService(guard, runs).start(StartRun(projectA))
 * guard.checks shouldBe listOf(RecordedCheck.of(RUN_START on projectA))
 * ```
 */
public class RecordingAccessGuard(
    private val delegate: AccessGuard = AllExceptGuard(null),
) : AccessGuard {
    private val recorded = mutableListOf<RecordedCheck>()

    public val checks: List<RecordedCheck>
        get() = recorded.toList()

    override fun effective(): EffectivePermissions = delegate.effective()

    override fun <T> asSystem(
        reason: String,
        block: () -> T,
    ): T = delegate.asSystem(reason, block)

    override fun isGranted(permission: Permission): Boolean = delegate.isGranted(permission)

    override fun <S : Any> isGranted(
        permission: ScopedPermission<S>,
        scope: S,
    ): Boolean = delegate.isGranted(permission, scope)

    override fun require(permission: Permission) {
        recorded += RecordedCheck.Required(permission)
        delegate.require(permission)
    }

    override fun <S : Any> require(
        permission: ScopedPermission<S>,
        scope: S,
    ) {
        recorded += RecordedCheck.Required(permission, permission.scopeType.key(scope))
        delegate.require(permission, scope)
    }

    override fun <S : Any> scopesWith(permission: ScopedPermission<S>): ScopeFilter<S> = delegate.scopesWith(permission)

    override fun requireSystem() {
        recorded += RecordedCheck.System
        delegate.requireSystem()
    }
}
