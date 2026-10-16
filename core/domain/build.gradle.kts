plugins {
    alias(libs.plugins.tilfaz.jvm.library)
}

// Domain: repository contracts and business rules. Depends on nothing but the model and coroutines.
dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}

dependencies {
    testImplementation(projects.core.testing)
}
