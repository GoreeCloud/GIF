pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "GoreeCloud-GIF"
include(":app")
include(":interop-fixture")
project(":interop-fixture").projectDir = file("qa/interop-fixture")
