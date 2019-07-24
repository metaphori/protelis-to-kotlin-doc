import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version Versions.org_jetbrains_kotlin
    `maven-publish`
    signing
    id("de.fayard.buildSrcVersions") version Versions.de_fayard_buildsrcversions_gradle_plugin
    id("com.gradle.build-scan") version Versions.com_gradle_build_scan_gradle_plugin
    id("com.gradle.plugin-publish") version Versions.com_gradle_plugin_publish_gradle_plugin
    id("org.danilopianini.git-sensitive-semantic-versioning") version
        Versions.org_danilopianini_git_sensitive_semantic_versioning_gradle_plugin
    id("org.danilopianini.publish-on-central") version Versions.org_danilopianini_publish_on_central_gradle_plugin
    id("org.jetbrains.dokka") version Versions.org_jetbrains_dokka
    id("org.jlleitschuh.gradle.ktlint") version Versions.org_jlleitschuh_gradle_ktlint_gradle_plugin
    `java-gradle-plugin`
}

group = "org.protelis"

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://plugins.gradle.org/m2/")
}

gitSemVer {
    maxVersionLength.set(20)
    version = computeGitSemVer() // THIS IS MANDATORY, AND MUST BE LAST IN BLOCK
}

dependencies {
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)
    implementation(gradleApi())
    implementation(Libs.dokka_gradle_plugin)
    implementation(Libs.kotlin_gradle_plugin)
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:8.2.0")

    testImplementation(Libs.kotlin_test)
    testImplementation(Libs.kotlin_test_junit)
    testImplementation(gradleTestKit())
    testImplementation(Libs.kotlintest_runner_junit5)
}

application {
    mainClassName = "it.unibo.protelis2kotlin.AppKt"
}

tasks {
    javadocJar {
        dependsOn(dokka)
        from(dokka.get().outputDirectory)
    }
}

ktlint {
    ignoreFailures.set(false)
}

val websiteUrl = "https://github.com/Protelis/Protelis-KDoc-generator"

publishOnCentral {
    projectDescription.set("A translator from documented Protelis code to compiling Kotlin interfaces")
    projectLongName.set("Protelis KDoc generator")
    projectUrl.set(websiteUrl)
    scmConnection.set("git@github.com:Protelis/Protelis-KDoc-generator.git")
}

pluginBundle {
    website = websiteUrl
    vcsUrl = websiteUrl
    tags = listOf("protelis", "javadoc", "documentation", "protelisdoc", "dokka", "kotlin")
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

tasks {
    "test"(Test::class) {
        useJUnitPlatform()
        testLogging.showStandardStreams = true
        testLogging {
            showCauses = true
            showStackTraces = true
            showStandardStreams = true
            events(*TestLogEvent.values())
        }
    }
    register("createClasspathManifest") {
        val outputDir = file("$buildDir/$name")
        inputs.files(sourceSets.main.get().runtimeClasspath)
        outputs.dir(outputDir)
        doLast {
            outputDir.mkdirs()
            file("$outputDir/plugin-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.joinToString("\n"))
        }
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.6"
    }
}

// Add the classpath file to the test runtime classpath
dependencies {
    testRuntimeOnly(files(tasks["createClasspathManifest"]))
}

gradlePlugin {
    plugins {
        create("Protelis2KotlinDoc") {
            id = "org.protelis.protelisdoc"
            displayName = "Protelis Documentation Engine"
            description = "A plugin that translates Protelis modules to Kotlin code, then generates the function documentation via Dokka"
            implementationClass = "it.unibo.protelis2kotlin.Protelis2KotlinDocPlugin"
        }
    }
}

buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
}
