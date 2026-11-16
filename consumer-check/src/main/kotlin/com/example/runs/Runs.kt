package com.example.runs

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.PermissionSet
import com.osucha.portpermissions.ScopeFilter
import com.osucha.portpermissions.ScopeType
import com.osucha.portpermissions.SystemOperation
import java.util.UUID

@JvmInline
value class ProjectId(
    val value: UUID,
)

val PROJECT = ScopeType<ProjectId>("project", key = { it.value.toString() }, parse = { ProjectId(UUID.fromString(it)) })

object RunPermissions : PermissionSet("runs") {
    val START = scoped("start", "Start runs", PROJECT)
    val VIEW = scoped("view", "View runs", PROJECT)
}

interface StartRun {
    fun start(project: ProjectId)
}

interface ListRuns {
    fun list(): List<ProjectId>
}

interface PurgeRuns {
    fun purge()
}

class Runs {
    val started = mutableListOf<ProjectId>()
}

class StartRunService(
    private val guard: AccessGuard,
    private val runs: Runs,
) : StartRun {
    override fun start(project: ProjectId) {
        guard.require(RunPermissions.START, project)
        runs.started += project
    }
}

class ListRunsService(
    private val guard: AccessGuard,
    private val runs: Runs,
) : ListRuns {
    override fun list(): List<ProjectId> =
        when (val filter = guard.scopesWith(RunPermissions.VIEW)) {
            ScopeFilter.All -> runs.started
            is ScopeFilter.AllExcept -> runs.started.filterNot { it in filter.excluded }
            is ScopeFilter.Only -> runs.started.filter { it in filter.ids }
        }
}

class PurgeRunsService(
    private val guard: AccessGuard,
    private val runs: Runs,
) : PurgeRuns {
    @SystemOperation("Nightly retention")
    override fun purge() {
        guard.requireSystem()
        runs.started.clear()
    }
}
