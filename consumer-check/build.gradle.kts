plugins {
    kotlin("jvm") version "2.3.21"
}

repositories {
    mavenLocal()
    mavenCentral()
}

val portPermissions = "0.2.1"

dependencies {
    implementation("com.osucha:port-permissions-core:$portPermissions")
    implementation("com.osucha:port-permissions-spring:$portPermissions")
    testImplementation("com.osucha:port-permissions-test:$portPermissions")
    testImplementation("com.osucha:port-permissions-archunit:$portPermissions")
    testImplementation("com.osucha:port-permissions-konsist:$portPermissions")
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
