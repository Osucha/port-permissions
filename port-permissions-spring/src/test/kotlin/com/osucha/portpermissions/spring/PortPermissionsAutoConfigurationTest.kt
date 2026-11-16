package com.osucha.portpermissions.spring

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.DuplicatePermissionException
import com.osucha.portpermissions.EffectivePermissions
import com.osucha.portpermissions.EffectivePermissionsSource
import com.osucha.portpermissions.PermissionCatalog
import com.osucha.portpermissions.PermissionDeniedException
import com.osucha.portpermissions.PermissionSet
import com.osucha.portpermissions.ScopeFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

object PaymentsPermissions : PermissionSet("payments") {
    val REFUND = permission("refund", "Refund payments")
}

@Configuration(proxyBeanMethods = false)
class OrdersModuleConfig {
    @Bean
    fun ordersPermissions(): PermissionSet = OrdersPermissions
}

@Configuration(proxyBeanMethods = false)
class PaymentsModuleConfig {
    @Bean
    fun paymentsPermissions(): PermissionSet = PaymentsPermissions
}

@Configuration(proxyBeanMethods = false)
class ClashingModuleConfig {
    @Bean
    fun clashingPermissions(): PermissionSet =
        object : PermissionSet("orders") {
            val view = permission("view", "View orders, again")
        }
}

class PortPermissionsAutoConfigurationTest {
    private val runner = ApplicationContextRunner().withUserConfiguration(PortPermissionsAutoConfiguration::class.java)

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `collects permission sets of all modules into one catalog, with scopes`() {
        runner.withUserConfiguration(OrdersModuleConfig::class.java, PaymentsModuleConfig::class.java).run { context ->
            context.getBean(PermissionCatalog::class.java).permissions.map { it.key to it.scope } shouldContainExactly
                listOf("orders.cancel" to null, "orders.manage" to "store", "orders.view" to null, "payments.refund" to null)
        }
    }

    @Test
    fun `the default guard checks scoped permissions carried by the principal`() {
        runner.withUserConfiguration(OrdersModuleConfig::class.java).run { context ->
            val guard = context.getBean(AccessGuard::class.java)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    TokenPrincipal(EffectivePermissions.of(OrdersPermissions.MANAGE on 7)),
                    null,
                    emptyList(),
                )

            guard.require(OrdersPermissions.MANAGE, 7)
            guard.scopesWith(OrdersPermissions.MANAGE) shouldBe ScopeFilter.Only(setOf(7))
            shouldThrow<PermissionDeniedException> { guard.require(OrdersPermissions.MANAGE, 8) }.scope shouldBe "8"
        }
    }

    @Test
    fun `the authority prefix comes from configuration`() {
        runner.withPropertyValues("port-permissions.authority-prefix=PERM_").run { context ->
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated("alice", null, listOf(SimpleGrantedAuthority("PERM_orders.view")))

            context.getBean(AccessGuard::class.java).isGranted(OrdersPermissions.VIEW) shouldBe true
        }
    }

    @Test
    fun `an application chooses the strategy by providing its own source`() {
        val fresh = EffectivePermissionsSource { EffectivePermissions.of(PaymentsPermissions.REFUND) }

        runner.withBean(EffectivePermissionsSource::class.java, { fresh }).run { context ->
            context.getBean(AccessGuard::class.java).isGranted(PaymentsPermissions.REFUND) shouldBe true
        }
    }

    @Test
    fun `an application can provide its own guard`() {
        val custom = AccessGuard.denyingAll()
        runner.withBean(AccessGuard::class.java, { custom }).run { context ->
            context.getBean(AccessGuard::class.java) shouldBe custom
        }
    }

    @Test
    fun `duplicate keys across modules stop the context`() {
        runner.withUserConfiguration(OrdersModuleConfig::class.java, ClashingModuleConfig::class.java).run { context ->
            generateSequence(context.startupFailure) { it.cause }
                .any { it is DuplicatePermissionException } shouldBe true
        }
    }

    @Test
    fun `auto-configuration is discovered by Spring Boot`() {
        val candidates = ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader)

        candidates.toList() shouldContain PortPermissionsAutoConfiguration::class.java.name
    }
}
