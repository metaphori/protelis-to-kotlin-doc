package it.unibo.protelis2kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

open class Protelis2KotlinDocPluginExtension @JvmOverloads constructor(
    private val project: Project,
    val baseDir: Property<String> = project.propertyWithDefault("."),
    val destDir: Property<String> = project.propertyWithDefault(project.buildDir.path + "/protelis-docs/"),
    val kotlinVersion: Property<String> = project.propertyWithDefault("+"),
    val protelisVersion: Property<String> = project.propertyWithDefault("+")
)

class Protelis2KotlinDocPlugin : Plugin<Project> {
    private val configureGenerateProtelisDocTaskName = "configureGenerateProtelisDoc"
    private val generateProtelisDocTaskName = "generateProtelisDoc"
    private val generateKotlinFromProtelisTaskName = "generateKotlinFromProtelis"
    private val compileKotlinTaskName = "compileKotlin"

    private val dokkaTaskName = "dokka"
    private val dokkaPluginName = "org.jetbrains.dokka"
    private val kotlinPluginName = "org.jetbrains.kotlin.jvm"

    override fun apply(project: Project) {
        val extension = project.extensions.create("Protelis2KotlinDoc", Protelis2KotlinDocPluginExtension::class.java, project)

        project.repositories.add(project.repositories.jcenter())
        project.repositories.add(project.repositories.mavenCentral())

        project.pluginManager.apply(kotlinPluginName)

        // Add dependency to Kotlin stdlib for TODO()s and Protelis
        project.dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:${extension.kotlinVersion.get()}")
        project.dependencies.add("implementation", "org.protelis:protelis-interpreter:${extension.protelisVersion.get()}")

        project.pluginManager.apply(Protelis2KotlinPlugin::class.java)
        project.pluginManager.apply(dokkaPluginName)

        // project.plugins.forEach { println("Plugin: $it") }
        // val dokka = project.plugins.getAt(dokkaPluginName)
        val p2kp = project.extensions.getByName("Protelis2Kotlin") as Protelis2KotlinPluginExtension
        p2kp.destDir.set(project.buildDir.path + "/protelis2kotlin/src/main/kotlin")
        val protelis2kotlintask = project.tasks.getByName(generateKotlinFromProtelisTaskName)
        protelis2kotlintask.dependsOn(configureGenerateProtelisDocTaskName)

        val dokkaTask = project.tasks.getByName(dokkaTaskName)
        dokkaTask.setProperty("outputFormat", "html")
        dokkaTask.setProperty("jdkVersion", 8)
        dokkaTask.setProperty("reportUndocumented", true)
        dokkaTask.dependsOn(compileKotlinTaskName)

        val kotlinPluginExt = project.extensions.getByName("kotlin") as KotlinJvmProjectExtension
        val mainKotlinSrcset = kotlinPluginExt.sourceSets.getByName("main")
        // This doesn't work: mainKotlinSrcset.kotlin.srcDirs.add(File(p2kp.destDir.get()))
        // This also doesn't work: mainKotlinSrcset.resources.srcDirs.add(File(p2kp.destDir.get()))
        // This doesn't work as well: mainKotlinSrcset.kotlin.sourceDirectories.files.add(File(p2kp.destDir.get()))
        mainKotlinSrcset.kotlin.setSrcDirs(setOf(File(p2kp.destDir.get())))

        val compileKotlin = project.tasks.getByPath(compileKotlinTaskName)
        compileKotlin.dependsOn(generateKotlinFromProtelisTaskName)

        project.task(configureGenerateProtelisDocTaskName) {
            it.doLast {
                p2kp.baseDir.set(extension.baseDir.get())
                dokkaTask.setProperty("outputDirectory", extension.destDir.get())
            }
        }

        project.task(generateProtelisDocTaskName) {
            it.outputs.files(project.fileTree(extension.destDir.get()))
            it.dependsOn(dokkaTaskName)
        }
    }
}