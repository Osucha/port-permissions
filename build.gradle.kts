import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

subprojects {
    group = "com.osucha"
    version = "0.1.0"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
        explicitApi()
    }

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name = project.name
                    url = "https://github.com/Osucha/port-permissions"
                    description = "Permissions exposed by modules, checked on inbound ports and collected centrally for management."
                    scm {
                        url = "https://github.com/Osucha/port-permissions"
                    }
                    licenses {
                        license {
                            name = "Apache License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0"
                        }
                    }
                }
            }
        }
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junit.bom))
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testImplementation"(rootProject.libs.kotest.assertions.core)
        "testRuntimeOnly"(rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
