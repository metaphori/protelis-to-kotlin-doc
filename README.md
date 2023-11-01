# ProtelisDoc: Protelis documentation engine

## Overview

This project is a Gradle plugin which has the goal of producing documentation for Protelis code.

It generates documentation from JavaDoc-like comments that are put just above Protelis functions.

It works as follows:

- Protelis code is converted to Kotlin code (including only documentation comments and function signatures)
- Documentation is produced from Kotlin code via [dokka](https://github.com/Kotlin/dokka)
(the tool for Kotlin corresponding to JavaDoc for Java)

## Usage

The plugin is designed to work in a Gradle build.
The following instructions refer to your `build.gradle.kts` configuration file.

First of all, apply the plugin in your `plugins` block:

```kotlin
plugins {
    id("org.protelis.protelisdoc") version "SELECT LATEST VERSION HERE"
}
```

If you are not using JCenter as repository,
then you need to explicitly add the repository where Dokka is to be found:

```kotlin
repositories {
    // Your repository configuration here, plus

    // You need to add JCenter.
    // Some dokka components are not released on Maven Central
    // Of course you can skip this if you are using JCenter already.
    jcenter {
        // You can filter though to avoid leaking dependencies
        content {
            includeGroup("com.soywiz.korlibs.korte")
            includeGroup("org.jetbrains")
            includeGroupByRegex("org.jetbrains.(dokka|kotlinx)")
        }
    }
```

The plugin imports all the dependencies in your `implementation` configuration
and exposes their types to the documentation engine.
However, if you have references to other types in your protelis code,
or your project does not include the protelis types (e.g. because it is a plain collection of scripts),
then you want to add them explictly so that protelisdoc can correctly represent them.

We do recommend including at least the protelis interpreter and the Kotlin standard library
(on which built-in types are mapped by the documentation engine).

```kotlin
dependencies {
    protelisdoc(kotlin("stdlib"))
    protelisdoc("org.protelis:protelis-interpreter:PROTELIS_VERSION")
}
```

Once you are setup, you can run the task ``./gradlew generateProtelisDoc``.

You can find the available versions on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.protelis.protelisdoc).
You will also find the syntax for importing the plugin in Groovy-based Gradle builds.

## Configuration

The plugin follows the convention-over-configuration principle.
So, with standard Gradle/Maven-like,
you should expect it to find protelis code under the working directory and generate docs under `build/protelis-docs`.

The configuration syntax, configuration options, and default values are shown in the following listing:

```kotlin
protelisdoc {
  baseDir.set(project.path) // base dir from which recursively looking for .pt files
  destDir.set("${project.buildDir.path}/protelis-docs/") // output dir for docs
  outputFormat.set("javadoc") // Dokka's output format (alternative: 'html')
  debug.set(false) // Debug prints are disabled by default
}
```

### Troubleshooting

- [A Dokka issue](https://github.com/Kotlin/dokka/issues/294) causes failure when generating `javadoc` format with Java version `> 8`.
As a workaround, use JDK 8 or generate `html` format.
```
* What went wrong:
Execution failed for task ':dokkaJavadoc'.
> com/sun/tools/doclets/formats/html/HtmlDoclet
```

## Documenting Protelis code

For the plugin to work correctly, Protelis code should be documented as follows:

```kotlin
/**
 * @param a bool, first condition
 * @param b bool, second condition
 * @return  bool, true if both the conditions are true
 */
public def and(a, b) { ... }

/**
 * Apply function to the children of the current device. Use this function if every
 * child has a single parent, see getAllChildren otherwise.
 *
 * @param potential num, potential to be followed
 * @param f         (ExecutionContext) -> T', function to be applied to the child
 * @param g         (T) -> T, function to be applied to the child value
 * @param default   T, default value for devices which are not children
 * @return          [num|T', T], children
 */
public def getChildren(potential, f, g, default) { ... }
```

I.e.,

* Parameters should be described with lines `@param <paramName> <paramType>, <paramDescription>`
* Return type should be described as follows `@return <retType>, <retDescription>`
* Function types take form `(T) -> T`, where single-letter uppercase symbols are assumed to be generic parameter types
* Tuple types are denoted as `[A,B,C]`
