package com.osucha.portpermissions.spring

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.DefaultAccessGuard
import com.osucha.portpermissions.EffectivePermissionsSource
import com.osucha.portpermissions.GrantResolver
import com.osucha.portpermissions.PermissionCatalog
import com.osucha.portpermissions.PermissionSet
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment

/**
 * Collects every [PermissionSet] bean into one [PermissionCatalog]. A module takes part by
 * exposing its set in its own configuration, without the management module knowing about it:
 *
 * ```
 * @Bean
 * fun billingPermissions(): PermissionSet = BillingPermissions
 * ```
 *
 * Duplicate keys across modules stop the application context from starting.
 *
 * The [AccessGuard] reads effective permissions from an [EffectivePermissionsSource] bean - define
 * one to choose the strategy (per request, session cache). Without it, [SecurityContextPermissionsSource]
 * is used, with the authority prefix from `port-permissions.authority-prefix`.
 */
@AutoConfiguration
public class PortPermissionsAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public fun permissionCatalog(sets: ObjectProvider<PermissionSet>): PermissionCatalog =
        PermissionCatalog(sets.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    public fun grantResolver(catalog: PermissionCatalog): GrantResolver = GrantResolver(catalog)

    @Bean
    @ConditionalOnMissingBean
    public fun effectivePermissionsSource(environment: Environment): EffectivePermissionsSource =
        SecurityContextPermissionsSource(environment.getProperty("port-permissions.authority-prefix", ""))

    @Bean
    @ConditionalOnMissingBean
    public fun accessGuard(source: EffectivePermissionsSource): AccessGuard = DefaultAccessGuard(source)
}
