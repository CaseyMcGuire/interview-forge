/** Temporary editor fixture until problems are available through GraphQL. */
export const sampleProblem = {
  slug: "two-sum",
  title: "Two Sum",
  language: "Kotlin",
  filename: "Solution.kt",
  starterCode: `class Solution {
    fun twoSum(nums: IntArray, target: Int): IntArray {
        // Write your solution here.
        return intArrayOf()
    }
}
`,
  examples: [
    {
      nums: "[4, 8, 1, 6]",
      target: "10",
      expected: "[0, 3]",
      explanation: "The values at positions 0 and 3 are 4 and 6. Together, they make 10.",
    },
    {
      nums: "[5, 2, 9]",
      target: "11",
      expected: "[1, 2]",
      explanation: "Choose 2 and 9, at positions 1 and 2.",
    },
    {
      nums: "[3, 3]",
      target: "6",
      expected: "[0, 1]",
      explanation: "Equal values are allowed: these are two different positions in the array.",
    },
  ],
} as const;
