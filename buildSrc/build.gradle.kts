object Versions {
    const val jooq = "3.13.4"
}

repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

dependencies {
    implementation("org.jooq:jooq-meta-extensions:${Versions.jooq}") {
        // causes conflicts with spring-boot plugin
        exclude(group = "org.springframework")
    }
    implementation("org.jooq:jooq-codegen:${Versions.jooq}")
}
