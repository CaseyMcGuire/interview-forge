package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Shared, curated coding problem. Content is editable in place; there are no revisions. */
class Problem : EntSchema("problems", clientName = "problems") {
  /** Database-generated identity, stable when the title or statement changes. */
  override fun id() = EntId.long()

  val slug by string("slug").unique().immutable()
    .comment("Stable, unique URL identifier, such as \"two-sum\".")

  val title by string("title")
    .comment("Human-readable problem name shown in the catalog and editor.")

  val statementMarkdown by string("statement_markdown")
    .comment("Markdown description, including requirements and constraints; examples also have test rows.")

  val difficulty by enum<ProblemDifficulty>("difficulty")
    .comment("Broad difficulty classification used to filter the catalog.")

  val checkerKind by enum<ProblemCheckerKind>("checker_kind")
    .default(ProblemCheckerKind.EXACT_JSON)
    .comment("Exact JSON comparison by default; CUSTOM requires checker source for each supported language.")

  val createdByUser by belongsTo<User>("created_by_user_id")
    .immutable()
    .onDelete(OnDelete.RESTRICT)
    .comment("Original author of the shared catalog entry; attribution stays fixed while the problem exists.")

  val publishedAt by instant("published_at").nullable()
    .comment("Null while the problem is a draft; a timestamp makes it eligible for catalog publication.")

  val archivedAt by instant("archived_at").nullable()
    .comment("Null while active; archiving removes a problem from normal browsing without erasing history.")

  val languageConfigurations by hasMany<ProblemLanguage>()
    .comment("Starter-code configurations available for this problem's supported languages.")

  val testCases by hasMany<TestCase>()
    .comment("Official examples and hidden grading cases, shared across languages.")

  val problemSubmissions by hasMany<ProblemSubmission>()
    .comment("Attempts by all users; future access policies must filter this history by the viewer.")

  val createdAt by instant("created_at").defaultNow().immutable()
    .comment("Time the catalog entry was created.")

  val updatedAt by instant("updated_at").defaultNow().updateDefaultNow()
    .comment("Time this problem row was last edited; child edits have their own timestamps.")

  /** Supports author attribution lookups and the author foreign key. */
  val byCreator by index("idx_problems_created_by_user", createdByUser.fk)
}
