Use a main function when the driver needs to control JSON parsing and output itself.

Define a top-level `fun main()` or `fun main(args: Array<String>)` in the default package.
Do not use a package declaration or `@file:JvmName`.

The runtime compiles the submitted source (`Solution.kt`) and driver (`TestDriver.kt`)
together. The driver can directly call classes and functions from the submitted source.
The starter code must declare the API submissions should implement; the reference solution
must implement that same API. A solution class does not have to be named `Solution`.

For a problem whose starter code declares `Solution.sum(numbers: List<Int>): Int`:

```kotlin
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

data class SumInput(val numbers: List<Int>)

private val mapper = jacksonObjectMapper()

fun main() {
  val input = mapper.readValue<SumInput>(System.`in`.bufferedReader().readText())
  val solution = Solution()
  print(mapper.writeValueAsString(solution.sum(input.numbers)))
}
```

For input `{"numbers":[1,2,3]}`, this driver should print `6`.
Jackson is included in the runtime; no dependency declarations are needed.

Each invocation receives one JSON value on stdin, followed by EOF, and must write exactly
one JSON answer to stdout. Use stderr for diagnostics; extra stdout text makes the answer
invalid. The grader compares the returned JSON value with the expected answer.

The suite calls main once per case in the same JVM. Static fields, singleton objects, and
other global state persist between cases. Input parsing, execution, and output serialization
count toward the per-case time limit. Stdout and stderr each have a 20,000-byte limit.
