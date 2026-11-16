package com.osucha.portpermissions.archunit.fixtures.good.adapter.inbound.scheduler

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound.system.PurgeRuns

class PurgeJob(
    private val guard: AccessGuard,
    private val purgeRuns: PurgeRuns,
) {
    fun nightly() = guard.asSystem("nightly retention") { purgeRuns.purge() }
}
