package wildcard.application.usecase

import com.osucha.portpermissions.*

interface Ping {
    fun ping(): String
}

class PingService : Ping {
    @PublicOperation("Health check")
    override fun ping(): String = "pong"
}

interface Version {
    fun version(): String
}

class VersionService : Version {
    @com.osucha.portpermissions.PublicOperation("Shown on the login page")
    override fun version(): String = "1.0"
}
