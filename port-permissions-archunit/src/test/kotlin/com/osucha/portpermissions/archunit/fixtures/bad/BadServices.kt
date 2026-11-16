package com.osucha.portpermissions.archunit.fixtures.bad.application.usecase

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.SystemOperation
import com.osucha.portpermissions.archunit.fixtures.RunPermissions
import com.osucha.portpermissions.archunit.fixtures.Validator
import com.osucha.portpermissions.archunit.fixtures.bad.application.port.inbound.Operations
import com.osucha.portpermissions.archunit.fixtures.bad.application.port.inbound.system.Maintenance
import com.osucha.portpermissions.archunit.fixtures.inTransaction

class OperationsService(
    private val guard: AccessGuard,
    private val validator: Validator,
) : Operations {
    override fun missingCheck() = Unit

    /** `.require(` on something that is not a guard - the Konsist regex accepted this. */
    override fun falsePositive(project: String) {
        validator.require(project.isNotBlank())
    }

    /** Two lambda levels deep - documented as not seen. */
    override fun nestedLambda() {
        inTransaction { inTransaction { guard.require(RunPermissions.VIEW) } }
    }

    @SystemOperation("Marked as system, but on a user port")
    override fun systemOnUserPort() {
        guard.requireSystem()
    }
}

class MaintenanceService(
    private val guard: AccessGuard,
) : Maintenance {
    @SystemOperation("Forgets the runtime check")
    override fun withoutRequireSystem() = Unit

    override fun notAnnotated() {
        guard.requireSystem()
    }
}
