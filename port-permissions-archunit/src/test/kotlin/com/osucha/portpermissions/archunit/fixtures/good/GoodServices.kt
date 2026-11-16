package com.osucha.portpermissions.archunit.fixtures.good.application.usecase

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.PublicOperation
import com.osucha.portpermissions.SystemOperation
import com.osucha.portpermissions.archunit.fixtures.Callback
import com.osucha.portpermissions.archunit.fixtures.RunPermissions
import com.osucha.portpermissions.archunit.fixtures.Tx
import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.ListRuns
import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.ShowProfile
import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.StartRun
import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.UseCase
import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.system.PurgeRuns
import com.osucha.portpermissions.archunit.fixtures.inTransaction

class StartRunService(
    private val guard: AccessGuard,
) : StartRun {
    override fun start(project: String) {
        guard.require(RunPermissions.START, project)
    }

    /** Public, but not part of the port - not an operation. */
    fun helper(): Int = 42
}

class ListRunsService(
    private val guard: AccessGuard,
    private val tx: Tx,
) : ListRuns {
    override fun list(): List<String> =
        tx.execute(
            Callback {
                guard.require(RunPermissions.VIEW)
                emptyList()
            },
        )
}

class ShowProfileService : ShowProfile {
    @PublicOperation("Every signed-in user sees their own profile")
    override fun show(): String = "me"
}

class ArchiveRunService(
    private val guard: AccessGuard,
) : UseCase<String> {
    override fun execute(command: String) {
        inTransaction { guard.require(RunPermissions.START, command) }
    }
}

class PurgeRunsService(
    private val guard: AccessGuard,
) : PurgeRuns {
    @SystemOperation("Nightly retention job")
    override fun purge() {
        guard.requireSystem()
    }
}
