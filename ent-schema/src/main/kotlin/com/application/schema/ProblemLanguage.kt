package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Public starter code for one problem/language pair; private judge code lives separately. */
class ProblemLanguage : EntSchema("problem_languages", clientName = "problemLanguages") {
  /** Database-generated identity referenced by the judge configuration and submissions. */
  override fun id() = EntId.long()

  val problem by belongsTo<Problem>("problem_id")
    .immutable()
    .inverse(Problem::languageConfigurations)
    .onDelete(OnDelete.RESTRICT)
    .comment("Problem for which this language is supported; a configuration cannot be moved to another problem.")

  val language by belongsTo<Language>("language_id")
    .immutable()
    .inverse(Language::problemConfigurations)
    .onDelete(OnDelete.RESTRICT)
    .comment("Fixed language for the starter code and submissions; its runtime comes from application config.")

  val starterCode by string("starter_code")
    .comment("Code stencil placed in the editor when the user starts solving this problem in this language.")

  val judgeConfiguration by hasOne<JudgeConfiguration>()
    .comment("Optional during authoring; a usable language configuration must eventually have a private judge.")

  val problemSubmissions by hasMany<ProblemSubmission>()
    .comment("All attempts using this configuration; each attempt retains its own submitted source.")

  val customInputSubmissions by hasMany<CustomInputSubmission>()
    .comment("Custom attempts and their user-supplied inputs, retained until expiration cleanup.")

  val createdAt by instant("created_at").defaultNow().immutable()
    .comment("Time this language was added to the problem.")

  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
    .comment("Time the starter code was last edited.")

  /** Prevents competing starter-code configurations for the same problem and language. */
  val byProblemAndLanguage by
    index("uq_problem_languages_problem_language", problem.fk, language.fk).unique()

  /** Supports finding all problems that offer a given language. */
  val byLanguage by index("idx_problem_languages_language", language.fk)
}
