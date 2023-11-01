@file:Suppress("UnstableApiUsage")

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `maven-publish`
    signing
    `java-gradle-plugin`
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.gradlePluginPublish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.taskTree)
}

group = "org.protelis"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

gitSemVer {
    maxVersionLength.set(20)
    buildMetadataSeparator.set("-")
}

multiJvm {
    jvmVersionForCompilation.set(8)
    maximumSupportedJvmVersion.set(latestJavaSupportedByGradle)
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(kotlin("gradle-plugin"))
    implementation(libs.kotlin.compiler)
    implementation(libs.bundles.dokka)

    testImplementation(gradleTestKit())
    testImplementation(libs.bundles.kotlin.testing)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
        showCauses = true
        showStackTraces = true
        events(*org.gradle.api.tasks.testing.logging.TestLogEvent.values())
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<Copy> {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.WARN
}

if (System.getenv("CI") == true.toString()) {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

val websiteUrl = "https://github.com/Protelis/Protelis-KDoc-generator"

publishOnCentral {
    projectDescription.set("A translator from documented Protelis code to compiling Kotlin interfaces")
    projectLongName.set("ProtelisDoc generator")
    projectUrl.set(websiteUrl)
    scmConnection.set("git@github.com:Protelis/Protelis-KDoc-generator.git")
    repository("https://maven.pkg.github.com/Protelis/${rootProject.name}".toLowerCase(), name = "github") {
        user.set("danysk")
        password.set(System.getenv("GITHUB_TOKEN"))
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                developers {
                    developer {
                        name.set("Danilo Pianini")
                        email.set("danilo.pianini@unibo.it")
                        url.set("http://www.danilopianini.org/")
                    }
                    developer {
                        name.set("Roberto Casadei")
                        email.set("roby.casadei@unibo.it")
                    }
                }
            }
        }
    }
}

pluginBundle {
    website = websiteUrl
    vcsUrl = websiteUrl
    tags = listOf("protelis", "javadoc", "documentation", "protelisdoc", "dokka", "kotlin")
}

gradlePlugin {
    plugins {
        create("ProtelisDoc") {
            id = "org.protelis.protelisdoc"
            displayName = "Protelis Documentation Engine"
            description = "A plugin that translates Protelis modules to Kotlin code, then generates the function documentation via Dokka"
            implementationClass = "it.unibo.protelis2kotlin.Protelis2KotlinDocPlugin"
        }
    }
}
