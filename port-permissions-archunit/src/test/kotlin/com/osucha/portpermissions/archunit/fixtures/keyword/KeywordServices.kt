package com.osucha.portpermissions.archunit.fixtures.keyword.application.usecase

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.archunit.fixtures.RunPermissions
import com.osucha.portpermissions.archunit.fixtures.keyword.application.port.`in`.StartRun

class StartRunService(
    private val guard: AccessGuard,
) : StartRun {
    override fun start(project: String) {
        guard.require(RunPermissions.START, project)
    }

    override fun forgotten() = Unit
}
