pluginManagement {
    repositories {
        val isCi = System.getenv("CI") == "true"
        if (!isCi) {
            maven { url = java.net.URI("https://maven.aliyun.com/repository/google") }
            maven { url = java.net.URI("https://maven.aliyun.com/repository/public") }
            maven { url = java.net.URI("https://maven.aliyun.com/repository/gradle-plugin") }
        }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val isCi = System.getenv("CI") == "true"
        if (!isCi) {
            maven { url = java.net.URI("https://maven.aliyun.com/repository/google") }
            maven { url = java.net.URI("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "gkd-rule-studio"
include(":app")
