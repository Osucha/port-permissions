package com.osucha.portpermissions.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.osucha.portpermissions.PublicOperation
import com.osucha.portpermissions.SystemOperation

/**
 * Source-level rule that turns explicit checks into deny by default: every operation of a use case
 * either calls `require(...)` / `requireSystem()` on its access guard or is marked [PublicOperation]
 * or [SystemOperation].
 *
 * Operations are the overriding functions of classes in [useCasePackage]. A check counts only when
 * called on a constructor parameter or property whose type is an access guard (its name ends with
 * `AccessGuard`), directly in the operation's source. Source analysis has no type resolution, so
 * this rule is an approximation; `port-permissions-archunit` checks the bytecode and is the stronger
 * option.
 *
 * ```
 * @Test
 * fun `every use case operation is guarded`() {
 *     GuardedUseCases.assertGuarded(Konsist.scopeFromProduction())
 * }
 * ```
 */
public object GuardedUseCases {
    public const val DEFAULT_USE_CASE_PACKAGE: String = "..application.usecase.."

    /** Operations violating the rule, as `ClassName.function`, sorted. */
    public fun unguarded(
        scope: KoScope,
        useCasePackage: String = DEFAULT_USE_CASE_PACKAGE,
    ): List<String> =
        scope
            .classes()
            .withPackage(useCasePackage)
            .flatMap { useCase ->
                val guardCall = guardCall(guardNames(useCase))
                useCase
                    .functions()
                    .filter { it.hasOverrideModifier }
                    .filterNot { function ->
                        function.annotations.any { it.name in EXEMPTION_NAMES } ||
                            guardCall?.containsMatchIn(function.text) == true
                    }.map { "${useCase.name}.${it.name}" }
            }.distinct()
            .sorted()

    public fun assertGuarded(
        scope: KoScope,
        useCasePackage: String = DEFAULT_USE_CASE_PACKAGE,
    ) {
        val offenders = unguarded(scope, useCasePackage)
        if (offenders.isNotEmpty()) {
            throw AssertionError(
                "Use case operations without a guard check, @PublicOperation or @SystemOperation:\n" +
                    offenders.joinToString("\n") { "  - $it" },
            )
        }
    }

    private fun guardNames(useCase: KoClassDeclaration): Set<String> {
        val parameters = useCase.primaryConstructor?.parameters.orEmpty().filter { isGuardType(it.type.text) }.map { it.name }
        val properties = useCase.properties().filter { property -> property.type?.let { isGuardType(it.text) } == true }.map { it.name }
        return (parameters + properties).toSet()
    }

    private fun isGuardType(type: String): Boolean = type.removeSuffix("?").substringAfterLast('.').endsWith("AccessGuard")

    private fun guardCall(names: Set<String>): Regex? =
        names
            .takeIf { it.isNotEmpty() }
            ?.let { Regex("""\b(${it.joinToString("|") { name -> Regex.escape(name) }})\s*\.\s*require(System)?\s*\(""") }

    // Matched by name, because type resolution of annotations does not follow wildcard imports.
    private val EXEMPTION_NAMES =
        setOf(
            PublicOperation::class.simpleName,
            PublicOperation::class.qualifiedName,
            SystemOperation::class.simpleName,
            SystemOperation::class.qualifiedName,
        )
}
