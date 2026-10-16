plugins {
    alias(libs.plugins.tilfaz.jvm.library)
}

// Shared fakes, fixtures and test rules. Pure JVM so every module's unit tests can use it.
dependencies {
    api(projects.core.model)
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.test)
    api(libs.junit)
}
