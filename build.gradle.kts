plugins {
    id("io.wusa.semver-git-plugin") version Versions.semverGitPlugin
    id("com.github.ben-manes.versions") version Versions.versionsPlugin
}

val semVersion = semver.info.version
allprojects {
    version = semVersion
    group = "io.orangebuffalo.simpleaccounting"
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
