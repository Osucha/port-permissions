package com.osucha.portpermissions.konsist

import com.lemonappdev.konsist.api.Konsist
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import java.io.File

class GuardedUseCasesTest {
    private val fixtures = Konsist.scopeFromExternalDirectory(
        File(javaClass.getResource("/fixtures")!!.toURI()).absolutePath,
    )

    @Test
    fun `reports operations that neither require a permission nor are public`() {
        GuardedUseCases.unguarded(fixtures) shouldContainExactly listOf("CancelOrderService.cancelAll")
    }

    @Test
    fun `assertGuarded names the offenders`() {
        val error = shouldThrow<AssertionError> { GuardedUseCases.assertGuarded(fixtures) }

        error.message shouldContain "CancelOrderService.cancelAll"
    }

    @Test
    fun `only classes in the use case package are checked`() {
        GuardedUseCases.unguarded(fixtures, useCasePackage = "..adapter..").shouldBeEmpty()
        shouldNotThrowAny { GuardedUseCases.assertGuarded(fixtures, useCasePackage = "..adapter..") }
    }
}
