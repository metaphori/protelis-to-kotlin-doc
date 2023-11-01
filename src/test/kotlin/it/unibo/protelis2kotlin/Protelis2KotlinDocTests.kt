package it.unibo.protelis2kotlin

import io.kotest.core.spec.style.StringSpec
import it.unibo.protelis2kotlin.TestUtil.file
import it.unibo.protelis2kotlin.TestUtil.folder
import it.unibo.protelis2kotlin.TestUtil.runGradleTask
import java.io.File.separator as SEP

class Protelis2KotlinDocTests : StringSpec({
    val workingDirectory = folder {
        file("src${SEP}main${SEP}protelis${SEP}file2.pt") {
            """
            unformed protelis file
            def prova
            /* ..
            hello
            """
        }
        file("src${SEP}main${SEP}protelis${SEP}file.java") {
            """
            /** prova
            */
            public static void main(String[] args){ }
            """.trimIndent()
        }
        file("src${SEP}main${SEP}protelis${SEP}collect.pt") {
            """
            module protelis:coord:some_collection
            
            /**
             * Aggregate a field of type T within a spanning tree built according to the maximum
             * decrease in potential. Accumulate the potential according to the reduce function.
             *
             * @param potential num, gradient of which gives aggregation direction
             * @param reduce    (T, T) -> T, function
             * @param local     T, local value
             * @param null      T, evaluated when the field is empty
             * @return          T, aggregated value
             */
            public def C(potential, reduce, local, null) {
                share (v <- local) {
                    reduce.apply(local,
                        /*
                         * TODO: switch to accumulateHood
                         */
                        hood(
                            (a, b) -> { reduce.apply(a, b) },
                            // expression that will be evaluated if the field is empty
                            null,
                            mux (nbr(getParent(potential, x -> { x.getDeviceUID() })) == self.getDeviceUID()) {
                                v
                            } else { null }
                        )
                    )
                }
            }
            
            public def another (
              x,
              y,z)
              {
            
            }
            
            def some_other_fun
             (param1,
              param2)  { }
            
            var default = 10;
            { /* b */; 2 }
            default = 8; { 3 }
            """.trimIndent()
        }
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
            public def and(a, b) {
                a && b
            }
            
            /**
             * Find the parent of the current device following the maximum decrease in
             * potential.
             *
             * @param potential num, potential
             * @param f         (ExecutionContext) -> T, what to do
             *                   with the parent
             * @param g         (T') -> T', what to do with the value
             * @param local     T', local value
             * @return          [num|T,T'], [imRoot()|noParent()|f(parent), g(value)]
             * @see imRoot, noParent
             */
            public def getParentExtended(potential, f, g, local) {
                getParentsExtended(potential, v -> { minHood(v) }, f, g, local, local)
            }
            
            /**
             * Find the parents of the current device following the decrease in
             * potential.
             *
             * @param potential num, potential
             * @param f         (ExecutionContext) -> T, what to do with the parent
             * @param g         (T') -> T', what to do with the value
             * @param local     T', local value
             * @return          [[num|T,T']], [[imRoot()|noParent()|f(parent), g(value)]]
             * @see imRoot, noParent
             */
            public def getParents(potential, f, g, local, default) {
                getParentsExtended(potential, identity, f, g, local, default)
            }
            
            /**
             * Aggregation of local information.
             *
             * @param local  T, local information
             * @param reduce (T, T) -> T, how to aggregate information
             * @param        T, aggregated information
             */
            public def aggregation(local, reduce) {
                hood((a, b) -> { reduce.apply(a, b) }, local, nbr(local))
            }
            """.trimIndent()
        }
        file("build.gradle.kts") {
            """
            plugins {
                id("org.protelis.protelisdoc")
                id("org.danilopianini.multi-jvm-test-plugin") version "0.3.4"
            }
            repositories {
                mavenCentral()
                jcenter {
                    content {
                        includeGroup("com.soywiz.korlibs.korte")
                        includeGroup("org.jetbrains")
                        includeGroupByRegex("org.jetbrains.(dokka|kotlinx)")
                    }
                }
            }
            dependencies {
                protelisdoc("org.protelis:protelis-interpreter:13.0.0")
            }
            multiJvm {
                jvmVersionForCompilation.set(8)
                maximumSupportedJvmVersion.set(latestJava)
            }
            protelisdoc {
                baseDir.set("src/main/protelis")
                destDir.set("docs")
                debug.set(true)
            }
            """
        }
    }
    "Generation of Kotlin docs from Protelis sources should work" {
        runGradleTask(workingDirectory, "protelisdoc")
    }
})
