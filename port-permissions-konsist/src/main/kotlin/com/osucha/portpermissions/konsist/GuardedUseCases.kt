package com.osucha.portpermissions.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.osucha.portpermissions.PublicOperation

/**
 * Architecture rule that turns explicit checks into deny by default: every operation of a use
 * case either calls `require(...)` on its access guard or is marked [PublicOperation]. Forgetting
 * both is a silent hole when checks are explicit, so the rule makes it a red build instead.
 *
 * Operations are the overriding functions of classes in [useCasePackage] - the implementations
 * of inbound ports. The `require(...)` call is detected in the function source, so keep it in
 * the operation itself rather than in a helper.
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
                useCase
                    .functions()
                    .filter { it.hasOverrideModifier }
                    .filterNot { function ->
                        function.annotations.any { it.name in PUBLIC_OPERATION_NAMES } ||
                            REQUIRE_CALL.containsMatchIn(function.text)
                    }
                    .map { "${useCase.name}.${it.name}" }
            }.distinct()
            .sorted()

    public fun assertGuarded(
        scope: KoScope,
        useCasePackage: String = DEFAULT_USE_CASE_PACKAGE,
    ) {
        val offenders = unguarded(scope, useCasePackage)
        if (offenders.isNotEmpty()) {
            throw AssertionError(
                "Use case operations without require(...) or @PublicOperation:\n" +
                    offenders.joinToString("\n") { "  - $it" },
            )
        }
    }

    private val REQUIRE_CALL = Regex("""\.require\s*\(""")

    // Matched by name, because type resolution of annotations does not follow wildcard imports.
    private val PUBLIC_OPERATION_NAMES = setOf(PublicOperation::class.simpleName, PublicOperation::class.qualifiedName)
}
