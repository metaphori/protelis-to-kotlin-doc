package it.unibo.protelis2kotlin

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.sequences.shouldNotBeEmpty
import it.unibo.protelis2kotlin.TestUtil.file
import it.unibo.protelis2kotlin.TestUtil.folder
import it.unibo.protelis2kotlin.TestUtil.runGradleTask
import java.io.File
import java.io.File.separator as SEP

class Protelis2KotlinTests : StringSpec({

    val workingDirectory = folder {
        file("src${SEP}main${SEP}protelis${SEP}file.pt") {
            """
            module protelis:coord:accumulation
            import protelis:coord:meta
            import protelis:coord:spreading
            import protelis:lang:utils
            import protelis:state:time
            
            module protelis:lang:utils
            import java.lang.Math.pow
            
            /**
             * @param a bool, first condition
             * @param b bool, second condition
             * @return  bool, true if both the conditions are true
             */
            public  def   and(a, b) {
                a && b
            }
            
            public def or(x,y){}
            
            /**
             * a
             */
            
            /**
              * b
              */
            /**
              * c
              */
            def add(x,y) = { x + y }
            
            /**
              * d
              */
            def sth(){}
            """.trimIndent()
        }
        file("build.gradle.kts") {
            """
            plugins {
                id("org.protelis.protelisdoc")
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                protelisdoc("org.protelis:protelis-interpreter:11.1.0")
            }
            protelisdoc {
                baseDir.set("src/main/protelis")
                debug.set(true)
            }
            """.trimIndent()
        }
    }
    "Generation of Kotlin interfaces from Protelis sources should work" {
        runGradleTask(workingDirectory, "generateKotlinFromProtelis")
        File(workingDirectory.root, "build").walkTopDown()
            .filter { it.extension == "kt" }
            .shouldNotBeEmpty()
    }
})
