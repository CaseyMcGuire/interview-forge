package com.application.schema

import entkt.schema.EntId
import entkt.schema.EntSchema
import entkt.schema.OnDelete

/** Pure link storage; relationship changes use the owning problem or tag's update policy. */
class ProblemTag : EntSchema("problem_tags", clientName = "problemTags") {
  override fun id() = EntId.long()

  val problem by belongsTo<Problem>("problem_id").onDelete(OnDelete.CASCADE)
  val tag by belongsTo<Tag>("tag_id").onDelete(OnDelete.CASCADE)

  val byProblemAndTag by index("uq_problem_tags_problem_tag", problem.fk, tag.fk).unique()
  val byTag by index("idx_problem_tags_tag", tag.fk)
}
