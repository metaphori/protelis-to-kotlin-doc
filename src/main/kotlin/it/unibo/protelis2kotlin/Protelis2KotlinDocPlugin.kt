package it.unibo.protelis2kotlin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File
import java.io.File.separator as SEP

/**
 * Extension for the Protelis2KotlinDoc plugin.
 * @param baseDir The base directory from which looking for Protelis files
 * @param destDir The directory that will contain the generated docs
 * @param kotlinVersion
 * @param protelisVersion
 */
open class Protelis2KotlinDocPluginExtension @JvmOverloads constructor(
    private val project: Project,
    val baseDir: Property<String> = project.propertyWithDefault("."),
    val destDir: Property<String> = project.propertyWithDefault(project.buildDir.path + "${SEP}protelis-docs$SEP"),
    val kotlinDestDir: Property<String> = project.propertyWithDefault(project.buildDir.path + "${SEP}kotlin-for-protelis$SEP"),
    val kotlinVersion: Property<String> = project.propertyWithDefault("+"),
    val protelisVersion: Property<String> = project.propertyWithDefault("+"),
    val outputFormat: Property<String> = project.propertyWithDefault("javadoc"),
    val automaticDependencies: Property<Boolean> = project.propertyWithDefault(false),
    val debug: Property<Boolean> = project.propertyWithDefault(false)
)

fun applyPluginIfNotAlreadyApplied(project: Project, pluginId: String) {
    if (!project.pluginManager.hasPlugin(pluginId)) {
        project.pluginManager.apply(pluginId)
    }
}

fun addRepoIfNotAlreadyPresent(repoHandler: RepositoryHandler, repo: ArtifactRepository) {
    if (!repoHandler.contains(repo)) {
        repoHandler.add(repo)
    }
}

/**
 * Protelis2KotlinDoc Gradle Plugin: reuses the Protelis2Kotlin and Dokka plugins to generate Kotlin docs from Protelis code.
 */
class Protelis2KotlinDocPlugin : Plugin<Project> {
    private val generateProtelisDocTaskName = "generateProtelisDoc"
    private val generateKotlinFromProtelisTaskName = "generateKotlinFromProtelis"
    private val compileKotlinTaskName = "compileKotlin"

    private val dokkaTaskName = "dokka"
    private val dokkaPluginId = "org.jetbrains.dokka"
    private val kotlinPluginId = "org.jetbrains.kotlin.jvm"
    private val kotlinLintPluginId = "org.jlleitschuh.gradle.ktlint"
    private val protelis2KotlinDocPlugin = "Protelis2KotlinDoc"

    private val protelisGroup = "org.protelis"
    private val protelisInterpreterDepName = "protelis-interpreter"
    private val kotlinGroup = "org.jetbrains.kotlin"
    private val kotlinStdlibDepName = "kotlin-stdlib"

    private val implConfiguration = "implementation"
    private val compileConfig = "compileClasspath"

    override fun apply(project: Project) {
        val extension = project.extensions.create(protelis2KotlinDocPlugin, Protelis2KotlinDocPluginExtension::class.java, project)
        if (JavaVersion.current() > JavaVersion.VERSION_1_8) extension.outputFormat.set("html")

        addRepoIfNotAlreadyPresent(project.buildscript.repositories, project.buildscript.repositories.gradlePluginPortal())
        addRepoIfNotAlreadyPresent(project.repositories, project.repositories.jcenter())
        addRepoIfNotAlreadyPresent(project.repositories, project.repositories.mavenCentral())

        /*
        Log.log("Buildscript configs: " + project.buildscript.configurations.map { it.name })
        val buildscriptConf = project.buildscript.configurations.create("download")
        // project.buildscript.configurations.findByName("classpath")!! // Cannot change dependencies of configuration ':classpath' after it has been resolved
        project.buildscript.dependencies.add(buildscriptConf.name, "org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:8.2.0") // org.jlleitschuh.gradle:ktlint.gradle:8.2.0
        val resconf = buildscriptConf.resolve()
        Log.log("Resolved plugins: " + resconf.map { it.absolutePath })
        */

        applyPluginIfNotAlreadyApplied(project, kotlinPluginId)
        applyPluginIfNotAlreadyApplied(project, kotlinLintPluginId)
        applyPluginIfNotAlreadyApplied(project, dokkaPluginId)

        // The following was an attempt to separate project's dependencies from plugin's dependencies
        /*
        val config = project.configurations.create("protelis-doc-config")
        project.dependencies.add(config.name, "$protelisGroup:$protelisInterpreterDepName:11.1.0")
        project.dependencies.add(config.name, "org.jetbrains.kotlin:kotlin-stdlib:1.2.41")
        val resolvedConfig = config.resolvedConfiguration
        val pluginDepsFirst = resolvedConfig.firstLevelModuleDependencies
        val pluginDeps = resolvedConfig.resolvedArtifacts

        Log.log("Plugin dependencies resolved " +
                if (resolvedConfig.hasError() ) "(errored)" else "(ok)\n" +
                "${pluginDepsFirst.map { it.toString() }.joinToString("\n - ","\n - ")}\n")

        project.dependencies.add("compileClasspath", "org.jetbrains.kotlin:kotlin-stdlib:1.2.41")
        project.dependencies.add("compileClasspath", "$protelisGroup:$protelisInterpreterDepName:11.1.0")
        // NOTE: it doesn't work on "kotlinCompilerClasspath" configuration
        //project.configurations.getByName("kotlinCompilerClasspath").dependencies.addAll(config.dependencies)
        */

        val compileKotlin = project.tasks.getByPath(compileKotlinTaskName) as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
        compileKotlin.dependsOn(generateKotlinFromProtelisTaskName)

        // Configure Dokka plugin
        val dokkaTask = project.tasks.getByName(dokkaTaskName)
        dokkaTask.setProperty("jdkVersion", 8)
        dokkaTask.setProperty("reportUndocumented", true)
        dokkaTask.dependsOn(compileKotlinTaskName)
        dokkaTask.setProperty("outputDirectory", extension.destDir.get())
        dokkaTask.setProperty("outputFormat", extension.outputFormat.get())

        // Configure Kotlin plugin
        val kotlinPluginExt = project.extensions.getByName("kotlin") as KotlinJvmProjectExtension
        val mainKotlinSrcset = kotlinPluginExt.sourceSets.getByName("main")
        // This doesn't work: mainKotlinSrcset.kotlin.srcDirs.add(File(*))
        // This also doesn't work: mainKotlinSrcset.resources.srcDirs.add(File(*))
        // This doesn't work as well: mainKotlinSrcset.kotlin.sourceDirectories.files.add(File(*))
        mainKotlinSrcset.kotlin.setSrcDirs(setOf(File(extension.kotlinDestDir.get())))

        val genKotlinTask = project.task(generateKotlinFromProtelisTaskName) {
            it.doLast {
                main(arrayOf(extension.baseDir.get(), extension.kotlinDestDir.get(), if (extension.debug.get()) "1" else "0"))
            }
            Log.log("[${it.name}]\nInputs: ${it.inputs.files.files}\nOutputs: ${it.outputs.files.files}")
        }

        val genDocTask = project.task(generateProtelisDocTaskName) {
            it.dependsOn(dokkaTaskName)
            Log.log("[${it.name}]\nInputs: ${it.inputs.files.files}\nOutputs: ${it.outputs.files.files}")
        }

        project.task("configureProtelis2KotlinPluginTasks") {
            genKotlinTask.dependsOn(it.name)
            compileKotlin.dependsOn("ktlintCheck")
            it.doLast {
                genKotlinTask.inputs.files(project.fileTree(extension.baseDir.get()))
                genKotlinTask.outputs.files(project.fileTree(extension.kotlinDestDir.get()))
                genDocTask.inputs.files(project.fileTree(extension.baseDir.get()))
                genDocTask.outputs.files(project.fileTree(extension.destDir.get()))
                if (extension.automaticDependencies.get()) {
                    Log.log("Automatically resolving dependencies")
                    val deps = project.configurations.getByName(compileConfig).dependencies
                    if (!deps.any { it.group==kotlinGroup && it.name==kotlinStdlibDepName }) {
                        project.dependencies.add(compileConfig, "$kotlinGroup:$kotlinStdlibDepName:${extension.kotlinVersion.get()}")
                    }

                    if (!deps.any { it.group==protelisGroup && it.name==protelisInterpreterDepName }) {
                        project.dependencies.add(compileConfig, "$protelisGroup:$protelisInterpreterDepName:${extension.protelisVersion.get()}")
                    }
                }
            }
        }
    }
}