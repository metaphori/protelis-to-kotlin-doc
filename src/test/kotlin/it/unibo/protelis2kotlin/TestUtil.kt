package it.unibo.protelis2kotlin

import io.kotest.core.spec.style.scopes.StringSpecScope
import io.kotest.matchers.shouldBe
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

internal object TestUtil {
    fun folder(closure: TemporaryFolder.() -> Unit) = TemporaryFolder().apply {
        create()
        file("settings.gradle") { "rootProject.name = 'testproject'" }
        File("${root.absoluteFile.absolutePath}${File.separator}src${File.separator}main${File.separator}protelis")
            .mkdirs()
        closure()
    }

    fun TemporaryFolder.file(name: String, content: () -> String) =
        newFile(name).writeText(content().trimIndent())

    fun StringSpecScope.runGradleTask(workingDirectory: TemporaryFolder, task: String) {
        val result = GradleRunner.create()
            .withProjectDir(workingDirectory.root)
            .withPluginClasspath()
            .withArguments(task, "--stacktrace")
            .build()
        println(result.output)
        result.task(":$task")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
