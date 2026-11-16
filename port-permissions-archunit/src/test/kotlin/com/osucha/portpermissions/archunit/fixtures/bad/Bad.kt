package com.osucha.portpermissions.archunit.fixtures.bad.application.port.inbound

interface Operations {
    fun missingCheck()

    fun falsePositive(project: String)

    fun nestedLambda()

    fun systemOnUserPort()
}
