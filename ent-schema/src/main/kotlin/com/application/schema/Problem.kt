package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/* Shared, curated coding problem. Content is editable in place; there are no revisions. */
class Problem : EntSchema("problems", clientName = "problems") {
  /* Database-generated identity, stable when the title or statement changes. */
  override fun id() = EntId.long()

  /* Stable, unique URL identifier, such as "two-sum". */
  val slug by string("slug").unique().immutable()

  /* Human-readable problem name shown in the catalog and editor. */
  val title by string("title")

  /* Markdown description, including requirements and constraints; examples also have test rows. */
  val statementMarkdown by text("statement_markdown")

  /* Broad difficulty classification used to filter the catalog. */
  val difficulty by enum<ProblemDifficulty>("difficulty")

  /* Determines whether inputs describe one function call or a constructor plus method calls. */
  val invocationKind by enum<ProblemInvocationKind>("invocation_kind")

  /* Exact JSON comparison by default; CUSTOM requires checker source for each supported language. */
  val checkerKind by enum<ProblemCheckerKind>("checker_kind")
    .default(ProblemCheckerKind.EXACT_JSON)

  /* User who originally authored the catalog entry; learners do not each get their own problem copy. */
  val createdByUserId by long("created_by_user_id").immutable()

  /* Author relationship; retain attribution while the problem exists. */
  val createdBy by belongsTo<User>("created_by")
    .field(createdByUserId)
    .onDelete(OnDelete.RESTRICT)

  /* Null while the problem is a draft; a timestamp makes it eligible for catalog publication. */
  val publishedAt by time("published_at").nullable()

  /* Null while active; archiving removes a problem from normal browsing without erasing history. */
  val archivedAt by time("archived_at").nullable()

  /* Starter-code configurations available for this problem's supported languages. */
  val languageConfigurations by hasMany<ProblemLanguage>("language_configurations")

  /* Official examples and hidden grading cases, shared across languages. */
  val testCases by hasMany<TestCase>("test_cases")

  /* Attempts by all users; future access policies must filter this history by the viewer. */
  val submissions by hasMany<Submission>("submissions")

  /* Time the catalog entry was created. */
  val createdAt by time("created_at").defaultNow().immutable()

  /* Time this problem row was last edited; child edits have their own timestamps. */
  val updatedAt by time("updated_at").defaultNow().updateDefaultNow()

  /* Supports author attribution lookups and the author foreign key. */
  val byCreator = index("idx_problems_created_by_user", createdByUserId)
}
