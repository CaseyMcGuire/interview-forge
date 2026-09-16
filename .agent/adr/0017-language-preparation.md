# 0017: Language-specific program preparation

Date: 2026-09-15

Status: Accepted.

## Decision

Define `com.application.execution.Language` with a language key and a `prepare` method
accepting submitted source and private test driver code. It returns a `PreparedProgram`:
relative source-file names and contents, an optional compile command, and a run command.
Commands are executable/argument lists, not shell strings. Preparation has no filesystem
or process side effects and does not parse or validate source code.

`KotlinLanguage` is the first implementation and is registered as a Spring component.
It keeps `Solution.kt` and `TestDriver.kt` separate so their imports and declarations
remain intact. It compiles both into `submission.jar` with the Kotlin standard library
included, then launches `TestDriverKt` explicitly. A submitted `main` therefore does not
change the selected entry point. The test driver must have a top-level `main` in the
default package and must not override its JVM class name with `@file:JvmName`.
See the Kotlin documentation for [compiler commands](https://kotlinlang.org/docs/command-line.html)
and [file class names](https://kotlinlang.org/docs/java-to-kotlin-interop.html#package-level-functions).

The runner will resolve `execution.runtimes[language.key]`, as specified in
[0016](0016-language-runtime.md). Kotlin's selected environment must provide `kotlinc`
and `java` on PATH. File creation, process execution, test input/output, limits, and
grading remain responsibilities of the future execution service. This change introduces
only the language preparation interface and its Kotlin implementation.
