dependencies {
    api(project(":port-permissions-core"))
    api(libs.spring.security.core)
    implementation(libs.spring.boot.autoconfigure)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.assertj.core)
}
