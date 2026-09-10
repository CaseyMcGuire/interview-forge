package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/* Public starter code for one problem/language pair; private judge code lives separately. */
class ProblemLanguage : EntSchema("problem_languages", clientName = "problemLanguages") {
  /* Database-generated identity referenced by the judge configuration and submissions. */
  override fun id() = EntId.long()

  /* Problem for which this language is supported; a configuration cannot be moved to another problem. */
  val problemId by long("problem_id").immutable()

  /* Link back to the shared problem and its language configurations. */
  val problem by belongsTo<Problem>("problem")
    .field(problemId)
    .inverse(Problem::languageConfigurations)
    .onDelete(OnDelete.RESTRICT)

  /* Programming language used by the starter code and submissions. */
  val languageId by long("language_id").immutable()

  /* Language catalog entry; runtime selection belongs to the private judge configuration. */
  val language by belongsTo<Language>("language")
    .field(languageId)
    .inverse(Language::problemConfigurations)
    .onDelete(OnDelete.RESTRICT)

  /* Code stencil placed in the editor when the user starts solving this problem in this language. */
  val starterCode by text("starter_code")

  /* Relative source filename expected by the harness, such as "solution.py" or "Solution.kt". */
  val solutionFilename by string("solution_filename")

  /* Optional during authoring; a usable language configuration must eventually have a private judge. */
  val judgeConfiguration by hasOne<JudgeConfiguration>("judge_configuration")

  /* All attempts using this configuration; each attempt retains its own submitted source. */
  val submissions by hasMany<Submission>("submissions")

  /* Time this language was added to the problem. */
  val createdAt by time("created_at").defaultNow().immutable()

  /* Time the starter code or filename was last edited. */
  val updatedAt by time("updated_at").defaultNow().updateDefaultNow()

  /* Prevents competing starter-code configurations for the same problem and language. */
  val byProblemAndLanguage =
    index("uq_problem_languages_problem_language", problemId, languageId).unique()

  /* Supports finding all problems that offer a given language. */
  val byLanguage = index("idx_problem_languages_language", languageId)
}
