package com.osucha.portpermissions

/**
 * Marks a use case operation that any authenticated caller may run, e.g. reading one's own profile.
 * The [reason] is mandatory, so every exemption is explained where it is made.
 *
 * Together with the rules from `port-permissions-archunit` or `port-permissions-konsist` this gives
 * deny by default: an operation that neither calls [AccessGuard.require] nor carries an exemption
 * annotation fails the build.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class PublicOperation(
    val reason: String,
)

/**
 * Marks a use case operation started only by the application itself - schedulers, queue workers,
 * callbacks of internal components, startup seeding. There is no user, and it must never be
 * reachable from a user-facing adapter.
 *
 * The operation calls [AccessGuard.requireSystem] as its first step, and its adapter runs it inside
 * [AccessGuard.asSystem]. Domain rules stay in the domain; the system use case is only the
 * application-level entry point.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class SystemOperation(
    val reason: String,
)
