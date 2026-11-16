package com.osucha.portpermissions.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PortPermissionsRulesTest {
    private val fixtures = "com.osucha.portpermissions.archunit.fixtures"
    private val good = ClassFileImporter().importPackages("$fixtures.good")
    private val bad = ClassFileImporter().importPackages("$fixtures.bad")
    private val keyword = ClassFileImporter().importPackages("$fixtures.keyword")
    private val rules = PortPermissionsRules()

    @Test
    fun `well-guarded code passes every rule`() {
        rules.all().flatMap { violations(it, good) }.shouldBeEmpty()
    }

    @Test
    fun `operations without a guard call are reported - including require on a non-guard and nested lambdas`() {
        violations(rules.operationsAreGuarded(), bad) shouldContainExactlyInAnyOrder
            listOf("OperationsService.missingCheck", "OperationsService.falsePositive", "OperationsService.nestedLambda")
    }

    @Test
    fun `system operations must call requireSystem`() {
        violations(rules.systemOperationsRequireSystem(), bad) shouldContainExactlyInAnyOrder
            listOf("MaintenanceService.withoutRequireSystem")
    }

    @Test
    fun `system operations and system ports go together`() {
        violations(rules.systemOperationsLiveInSystemPorts(), bad) shouldContainExactlyInAnyOrder
            listOf("OperationsService.systemOnUserPort", "MaintenanceService.notAnnotated")
    }

    @Test
    fun `user-facing adapters cannot depend on system ports`() {
        val details = rules.systemPortsAreNotReachableFromUserFacingAdapters().evaluate(bad).failureReport.details

        details.isNotEmpty() shouldBe true
        details.all { "MaintenanceController" in it && "Maintenance" in it } shouldBe true
    }

    @Test
    fun `inbound port package is configurable, including port dot in`() {
        val inKeyword = PortPermissionsRules(inboundPorts = listOf("..application.port.in.."))

        violations(inKeyword.operationsAreGuarded(), keyword) shouldContainExactlyInAnyOrder listOf("StartRunService.forgotten")
        violations(rules.operationsAreGuarded(), keyword).shouldBeEmpty()
    }

    /** Violations as `Class.method`, for the method-level rules. */
    private fun violations(
        rule: ArchRule,
        classes: JavaClasses,
    ): List<String> =
        rule
            .evaluate(classes)
            .failureReport.details
            .mapNotNull { METHOD.find(it)?.let { match -> "${match.groupValues[1]}.${match.groupValues[2]}" } }

    private companion object {
        val METHOD = Regex("""\.(\w+)\.([\w$-]+)\(""")
    }
}
