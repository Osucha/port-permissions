package com.osucha.portpermissions

import com.osucha.portpermissions.ProjectsPermissions.PROJECTS_CREATE
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SessionCachedPermissionsTest {
    private class MapSlot : SessionSlot {
        var value: CachedPermissions? = null

        override fun get() = value

        override fun put(value: CachedPermissions) {
            this.value = value
        }
    }

    private var user: String? = "alice"
    private val stored = mutableMapOf("alice" to setOf(PROJECTS_CREATE.key), "bob" to emptySet())
    private var resolutions = 0
    private val slot = MapSlot()
    private val versions = InMemoryPermissionVersions()
    private val source =
        SessionCachedPermissions({ user }, slot, versions) { id ->
            resolutions++
            EffectivePermissions(PrincipalKind.USER, global = stored.getValue(id))
        }

    @Test
    fun `resolves once and serves the session copy afterwards`() {
        source.current()
        source.current()

        resolutions shouldBe 1
        source.current()?.global shouldBe setOf(PROJECTS_CREATE.key)
    }

    @Test
    fun `invalidating the principal takes effect on the next request`() {
        source.current()
        stored["alice"] = emptySet()
        source.current()?.global shouldBe setOf(PROJECTS_CREATE.key)

        versions.invalidate("alice")

        source.current()?.global shouldBe emptySet()
        resolutions shouldBe 2
    }

    @Test
    fun `invalidating everyone also re-resolves`() {
        source.current()
        versions.invalidateAll()
        source.current()

        resolutions shouldBe 2
    }

    @Test
    fun `invalidating someone else keeps the cache`() {
        source.current()
        versions.invalidate("bob")
        source.current()

        resolutions shouldBe 1
    }

    @Test
    fun `a different principal in the same slot is resolved again`() {
        source.current()
        user = "bob"

        source.current()?.global shouldBe emptySet()
        resolutions shouldBe 2
    }

    @Test
    fun `no principal means not authenticated`() {
        user = null

        source.current() shouldBe null
        resolutions shouldBe 0
    }
}
