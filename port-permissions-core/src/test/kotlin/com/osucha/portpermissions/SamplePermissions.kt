package com.osucha.portpermissions

object BillingPermissions : PermissionSet("billing") {
    val INVOICES_VIEW = permission("invoices.view", "View invoices")
    val INVOICES_ISSUE = permission("invoices.issue", "Issue invoices")
}

object CustomersPermissions : PermissionSet("customers") {
    val VIEW = permission("view", "View customers")
}
