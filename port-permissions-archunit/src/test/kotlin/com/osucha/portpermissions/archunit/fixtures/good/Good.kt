package com.osucha.portpermissions.archunit.fixtures.good.application.port.inbound

interface StartRun {
    fun start(project: String)
}

interface ListRuns {
    fun list(): List<String>
}

interface ShowProfile {
    fun show(): String
}

/** A generic port: the implementation's parameter is erased to Object here. */
interface UseCase<C> {
    fun execute(command: C)
}
