package com.osucha.portpermissions

/**
 * Marks a use case operation that intentionally requires no permission, e.g. logging in, reading
 * one's own profile, or a scheduled job that runs without a user. The [reason] is mandatory, so
 * every exemption is explained where it is made.
 *
 * Together with the architecture rule from `port-permissions-konsist` this gives deny by default:
 * an operation that neither calls [AccessGuard.require] nor carries this annotation fails the
 * build.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
public annotation class PublicOperation(
    val reason: String,
)
