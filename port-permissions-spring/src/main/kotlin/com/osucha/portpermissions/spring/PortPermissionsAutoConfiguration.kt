package com.osucha.portpermissions.spring

import com.osucha.portpermissions.AccessGuard
import com.osucha.portpermissions.GrantResolver
import com.osucha.portpermissions.PermissionCatalog
import com.osucha.portpermissions.PermissionSet
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

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
    public fun accessGuard(): AccessGuard = SecurityContextAccessGuard()
}
