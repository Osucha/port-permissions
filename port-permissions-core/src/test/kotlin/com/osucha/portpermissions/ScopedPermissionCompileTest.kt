package com.osucha.portpermissions

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

/** Misuse of scoped permissions is a compile error, not a runtime one. */
class ScopedPermissionCompileTest {
    @Test
    fun `correct usage compiles`() {
        compile(
            """
            guard.require(ProjectsPermissions.PROJECTS_CREATE)
            guard.require(ProjectsPermissions.RUN_START, ProjectId("p-1"))
            guard.require(ProjectsPermissions.TEAM_MANAGE, TeamId(7))
            """,
        ) shouldBe ExitCode.OK
    }

    @Test
    fun `a scoped permission cannot be required without a scope`() {
        compile("guard.require(ProjectsPermissions.RUN_START)") shouldBe ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `a scoped permission cannot be required with a scope of another type`() {
        compile("guard.require(ProjectsPermissions.RUN_START, TeamId(7))") shouldBe ExitCode.COMPILATION_ERROR
        compile("guard.require(ProjectsPermissions.RUN_START, \"p-1\")") shouldBe ExitCode.COMPILATION_ERROR
    }

    @Test
    fun `a global permission cannot be required with a scope`() {
        compile("guard.require(ProjectsPermissions.PROJECTS_CREATE, ProjectId(\"p-1\"))") shouldBe ExitCode.COMPILATION_ERROR
    }

    private fun compile(body: String): ExitCode {
        val dir = Files.createTempDirectory("scoped-usage")
        val source = dir.resolve("Usage.kt")
        Files.writeString(
            source,
            """
            import com.osucha.portpermissions.*

            fun usage(guard: AccessGuard) {
                $body
            }
            """.trimIndent(),
        )
        val output = ByteArrayOutputStream()
        val code =
            K2JVMCompiler().exec(
                PrintStream(output),
                "-no-stdlib",
                "-no-reflect",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                dir.resolve("out").toString(),
                source.toString(),
            )
        withClue(output.toString()) { (code == ExitCode.OK || code == ExitCode.COMPILATION_ERROR) shouldBe true }
        dir.toFile().deleteRecursively()
        return code
    }
}
