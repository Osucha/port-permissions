package com.osucha.portpermissions.archunit

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.PublicOperation
import com.osucha.portpermissions.SystemOperation
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaCodeUnit
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.domain.PackageMatchers
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * Architecture rules on bytecode that make explicit checks deny by default.
 *
 * An **operation** is a public method of a class in [services] that implements a method of an
 * interface from [inboundPorts]. A **check** is a direct call to `require(...)` (any overload) or
 * `requireSystem()` on an [AccessGuard], made in the operation itself or in a lambda the operation
 * creates (one level deep - e.g. `transactionTemplate.execute { guard.require(...) }`). Checks in
 * helper methods or in lambdas nested in lambdas are not seen.
 *
 * ```
 * private val classes = ClassFileImporter().importPackages("com.example")
 * private val rules = PortPermissionsRules(inboundPorts = listOf("..port.`in`.."))
 *
 * @Test fun `operations are guarded`() = rules.operationsAreGuarded().check(classes)
 * ```
 *
 * Package patterns use ArchUnit syntax. A Kotlin package `` port.`in` `` is `port.in` in bytecode.
 */
public class PortPermissionsRules(
    private val inboundPorts: List<String> = listOf("..application.port.inbound.."),
    private val services: List<String> = listOf("..application.."),
    private val systemPorts: List<String> = listOf("..application.port.inbound.system.."),
    private val userFacingAdapters: List<String> = listOf("..adapter.inbound.web..", "..adapter.inbound.rest.."),
    private val exemptions: List<Class<out Annotation>> = listOf(PublicOperation::class.java, SystemOperation::class.java),
) {
    /** Every operation calls a check or carries one of the [exemptions]. */
    public fun operationsAreGuarded(): ArchRule =
        methods()
            .that(isOperationOf(inboundPorts))
            .should(
                condition("call AccessGuard.require(...) or be annotated with ${exemptions.joinToString { "@" + it.simpleName }}") { method ->
                    exemptions.any(method::isAnnotatedWith) || calledGuardMethods(method).any { it == REQUIRE || it == REQUIRE_SYSTEM }
                },
            ).allowEmptyShould(true)

    /** A [SystemOperation] calls `requireSystem()`, so the annotation is backed by a runtime check. */
    public fun systemOperationsRequireSystem(): ArchRule =
        methods()
            .that()
            .areAnnotatedWith(SystemOperation::class.java)
            .should(condition("call AccessGuard.requireSystem()") { REQUIRE_SYSTEM in calledGuardMethods(it) })
            .allowEmptyShould(true)

    /**
     * [SystemOperation]s implement [systemPorts], and operations of [systemPorts] are
     * [SystemOperation]s - so a system entry point is a separate port, not an annotation on a user one.
     */
    public fun systemOperationsLiveInSystemPorts(): ArchRule =
        methods()
            .that(
                isOperationOf(inboundPorts).or(
                    DescribedPredicate.describe("are annotated with @SystemOperation") { it.isAnnotatedWith(SystemOperation::class.java) },
                ),
            ).should(
                condition("be annotated with @SystemOperation exactly when implementing a system port ${systemPorts.joinToString()}") {
                    it.isAnnotatedWith(SystemOperation::class.java) == implementsPortMethod(it, systemPorts)
                },
            ).allowEmptyShould(true)

    /** User-facing adapters cannot even see system ports. */
    public fun systemPortsAreNotReachableFromUserFacingAdapters(): ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage(*userFacingAdapters.toTypedArray())
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*systemPorts.toTypedArray())
            .allowEmptyShould(true)

    /** All rules above. */
    public fun all(): List<ArchRule> =
        listOf(
            operationsAreGuarded(),
            systemOperationsRequireSystem(),
            systemOperationsLiveInSystemPorts(),
            systemPortsAreNotReachableFromUserFacingAdapters(),
        )

    private fun isOperationOf(ports: List<String>): DescribedPredicate<JavaMethod> =
        DescribedPredicate.describe("are operations of inbound ports ${ports.joinToString()}") { method ->
            method.modifiers.contains(JavaModifier.PUBLIC) &&
                JavaModifier.STATIC !in method.modifiers &&
                JavaModifier.SYNTHETIC !in method.modifiers &&
                JavaModifier.BRIDGE !in method.modifiers &&
                !method.owner.isInterface &&
                PackageMatchers.of(services).test(method.owner.packageName) &&
                implementsPortMethod(method, ports)
        }

    private fun implementsPortMethod(
        method: JavaMethod,
        ports: List<String>,
    ): Boolean =
        method.owner.allRawInterfaces
            .filter { PackageMatchers.of(ports).test(it.packageName) }
            .any { port -> port.methods.any { it.isImplementedBy(method) } }

    // Matched by name and by erased parameters assignable to the port's, which also covers generic
    // ports such as UseCase<C> whose erased parameter is Object.
    private fun JavaMethod.isImplementedBy(method: JavaMethod): Boolean =
        name == method.name &&
            rawParameterTypes.size == method.rawParameterTypes.size &&
            rawParameterTypes.zip(method.rawParameterTypes).all { (port, impl) -> impl.isAssignableTo(port.name) }

    private fun calledGuardMethods(method: JavaMethod): Set<String> =
        (sequenceOf<JavaCodeUnit>(method) + lambdasOf(method))
            .flatMap { it.methodCallsFromSelf.asSequence() }
            .filter { it.targetOwner.isAccessGuard() }
            .map { it.name }
            .toSet()

    // A non-inline lambda is compiled into a method of the same class, referenced from the operation
    // through invokedynamic: `op$lambda$0` in Kotlin 2 (not marked synthetic), `lambda$op$0` in Java.
    private fun lambdasOf(method: JavaMethod): Sequence<JavaCodeUnit> =
        method.methodReferencesFromSelf
            .asSequence()
            .filter { it.targetOwner == method.owner }
            .mapNotNull { it.target.resolveMember().orElse(null) }
            .filter {
                JavaModifier.SYNTHETIC in it.modifiers ||
                    it.name.startsWith("${method.name}\$lambda\$") ||
                    it.name.startsWith("lambda\$${method.name}\$")
            }

    private fun JavaClass.isAccessGuard(): Boolean = isAssignableTo(AccessGuard::class.java)

    private fun condition(
        description: String,
        satisfied: (JavaMethod) -> Boolean,
    ): ArchCondition<JavaMethod> =
        object : ArchCondition<JavaMethod>(description) {
            override fun check(
                method: JavaMethod,
                events: ConditionEvents,
            ) {
                if (!satisfied(method)) events.add(SimpleConditionEvent.violated(method, "${method.fullName} does not $description"))
            }
        }

    private companion object {
        const val REQUIRE = "require"
        const val REQUIRE_SYSTEM = "requireSystem"
    }
}
