package com.osucha.portpermissions.spring

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.DuplicatePermissionException
import com.osucha.portpermissions.PermissionCatalog
import com.osucha.portpermissions.PermissionSet
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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

    @Test
    fun `collects permission sets of all modules into one catalog`() {
        runner.withUserConfiguration(OrdersModuleConfig::class.java, PaymentsModuleConfig::class.java).run { context ->
            context.getBean(PermissionCatalog::class.java).permissions.map { it.key } shouldContainExactly
                listOf("orders.cancel", "orders.view", "payments.refund")
            context.getBean(AccessGuard::class.java).shouldBeInstanceOf<SecurityContextAccessGuard>()
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
    fun `an application can provide its own guard`() {
        val custom = AccessGuard { true }
        runner.withBean(AccessGuard::class.java, { custom }).run { context ->
            context.getBean(AccessGuard::class.java) shouldBe custom
        }
    }

    @Test
    fun `auto-configuration is discovered by Spring Boot`() {
        val candidates = ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader)

        candidates.toList() shouldContain PortPermissionsAutoConfiguration::class.java.name
    }
}
