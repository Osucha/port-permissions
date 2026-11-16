package com.osucha.portpermissions

object BillingPermissions : PermissionSet("billing") {
    val INVOICES_VIEW = permission("invoices.view", "View invoices")
    val INVOICES_ISSUE = permission("invoices.issue", "Issue invoices")
}

object CustomersPermissions : PermissionSet("customers") {
    val VIEW = permission("view", "View customers")
}

@JvmInline
value class ProjectId(
    val value: String,
)

@JvmInline
value class TeamId(
    val value: Int,
)

val PROJECT = ScopeType<ProjectId>("project", key = { it.value }, parse = ::ProjectId)
val TEAM = ScopeType<TeamId>("team", key = { it.value.toString() }, parse = { TeamId(it.toInt()) })

object ProjectsPermissions : PermissionSet("projects") {
    val PROJECTS_CREATE = permission("projects.create", "Create projects")
    val RUN_START = scoped("run.start", "Start and retry runs", PROJECT)
    val RUN_VIEW = scoped("run.view", "View runs", PROJECT)
    val TEAM_MANAGE = scoped("team.manage", "Manage a team", TEAM)
}
