package com.osucha.portpermissions

import com.osucha.portpermissions.GrantLevel.GROUP
import com.osucha.portpermissions.GrantLevel.SYSTEM
import com.osucha.portpermissions.GrantLevel.USER
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** The whole flow: modules expose sets, admin stores grants, login resolves them, use case checks. */
class EndToEndTest {
    interface IssueInvoice {
        fun issue(number: String): String
    }

    class IssueInvoiceService(
        private val guard: AccessGuard,
    ) : IssueInvoice {
        override fun issue(number: String): String {
            guard.require(BillingPermissions.INVOICES_ISSUE)
            return "issued $number"
        }
    }

    private val catalog = PermissionCatalog(listOf(BillingPermissions, CustomersPermissions))
    private val issue = BillingPermissions.INVOICES_ISSUE.key

    @Test
    fun `group grant lets the user issue, a user revocation takes it away`() {
        val stored = listOf(Grant(issue, SYSTEM, false), Grant(issue, GROUP, true))
        catalog.ensureKnown(stored.map { it.permission })

        IssueInvoiceService(guardFor(stored)).issue("FV/1") shouldBe "issued FV/1"

        val revoked = stored + Grant(issue, USER, false)
        shouldThrow<PermissionDeniedException> { IssueInvoiceService(guardFor(revoked)).issue("FV/2") }
    }

    private fun guardFor(grants: List<Grant>): AccessGuard {
        val session = GrantResolver(catalog).resolve(grants)
        return DefaultAccessGuard { session }
    }
}
