Use a typed JSON driver for problems where the driver can accept a Kotlin input value and
return the answer. The runtime handles JSON parsing and serialization.

Define a public class named `TestDriver` in the default package, with a public no-argument
constructor. Extend `JsonTestDriver<Input, Output>`, pass `Input::class` to its constructor,
and override `run(input: Input): Output`.

The runtime compiles the submitted source (`Solution.kt`) and driver (`TestDriver.kt`)
together. Construct and call the submitted classes in `run`; constructors can depend on the
input. The starter code must declare the API submissions should implement, and the reference
solution must implement that same API. There is no required solution class name.

For a problem whose starter code declares `Solution.sum(numbers: List<Int>): Int`:

```kotlin
import com.application.execution.JsonTestDriver

data class SumInput(val numbers: List<Int>)

class TestDriver : JsonTestDriver<SumInput, Int>(inputClass = SumInput::class) {
  override fun run(input: SumInput): Int {
    val solution = Solution()
    return solution.sum(input.numbers)
  }
}
```

For input `{"numbers":[1,2,3]}`, `run` should return `6`.

The root input must have a concrete, non-null type. Put generic or nullable values in an input
class, such as `data class Input(val values: List<Int>, val limit: Int?)`. Typed properties,
including sealed operations configured with Jackson annotations, retain their types.
The return type can be nullable, a collection, or another model.

Input conversion rejects unknown properties, missing required properties, nulls for non-null
properties, fractional integers, and string-to-number coercion. Return the answer instead of
printing it. The runtime serializes it to stdout; use stderr for diagnostics because extra
stdout text makes the answer invalid. The grader compares the JSON value with the expected answer.

The suite reuses one driver instance in one JVM. Instance fields and global state persist
between cases. Construct fresh solution objects inside `run` when the problem calls for it.
Driver construction, input parsing, `run`, and serialization count toward the per-case time
limit. Stdout and stderr each have a 20,000-byte limit.
