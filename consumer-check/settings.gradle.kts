// A separate build on the oldest supported Kotlin, consuming the artifacts from mavenLocal.
// CI: ./gradlew publishToMavenLocal, then ./gradlew -p consumer-check build
rootProject.name = "consumer-check"
