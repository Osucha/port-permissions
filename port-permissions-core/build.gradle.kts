// Core has no runtime dependencies besides the Kotlin stdlib - it is meant to live in the
// application layer of a hexagonal module. The compiler is used only by the test proving that
// misuse of scoped permissions does not compile.
dependencies {
    testImplementation(libs.kotlin.compiler.embeddable)
}
