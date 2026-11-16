package com.osucha.portpermissions.archunit.fixtures.good.adapter.inbound.web

import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.StartRun

class RunsController(
    private val startRun: StartRun,
) {
    fun post(project: String) = startRun.start(project)
}
