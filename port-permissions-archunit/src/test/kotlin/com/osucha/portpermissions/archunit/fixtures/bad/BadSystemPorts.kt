package com.osucha.portpermissions.archunit.fixtures.bad.application.port.inbound.system

interface Maintenance {
    fun withoutRequireSystem()

    fun notAnnotated()
}
