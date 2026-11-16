package com.example.runs

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.test.assertGuardedBy
import com.osucha.portpermissions.test.assertSystemOnly
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class RunsTest {
    private val runs = Runs()
    private val a = ProjectId(UUID.randomUUID())
    private val b = ProjectId(UUID.randomUUID())

    @Test
    fun `start is guarded by run start on the project`() {
        assertGuardedBy(RunPermissions.START on a, touched = { runs.started.isNotEmpty() }) { guard ->
            StartRunService(guard, runs).start(a)
        }
    }

    @Test
    fun `list shows only projects with view`() {
        runs.started += listOf(a, b)

        assertEquals(listOf(a), ListRunsService(AccessGuard.allowing(RunPermissions.VIEW on a), runs).list())
    }

    @Test
    fun `purge is a system operation`() {
        runs.started += a
        assertSystemOnly(touched = { runs.started.isEmpty() }) { guard -> PurgeRunsService(guard, runs).purge() }
    }
}
