package com.osucha.portpermissions.archunit.fixtures

import com.osucha.portpermissions.PermissionSet
import com.osucha.portpermissions.ScopeType

val PROJECT = ScopeType<String>("project", key = { it }, parse = { it })

object RunPermissions : PermissionSet("runs") {
    val START = scoped("start", "Start runs", PROJECT)
    val VIEW = permission("view", "View runs")
}

/** Like Spring's TransactionTemplate: a non-inline call taking a SAM callback. */
fun interface Callback<T> {
    fun run(): T
}

class Tx {
    fun <T> execute(callback: Callback<T>): T = callback.run()
}

/** Has a method called `require`, but is not an access guard. */
class Validator {
    fun require(condition: Boolean) {
        check(condition)
    }
}

/** A plain Kotlin higher-order function, not inline. */
fun <T> inTransaction(block: () -> T): T = block()
