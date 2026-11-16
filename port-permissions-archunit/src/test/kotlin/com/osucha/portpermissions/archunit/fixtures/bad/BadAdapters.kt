package com.osucha.portpermissions.archunit.fixtures.bad.adapter.inbound.web

import com.osucha.portpermissions.archunit.fixtures.bad.application.port.inbound.system.Maintenance

class MaintenanceController(
    private val maintenance: Maintenance,
) {
    fun post() = maintenance.notAnnotated()
}
