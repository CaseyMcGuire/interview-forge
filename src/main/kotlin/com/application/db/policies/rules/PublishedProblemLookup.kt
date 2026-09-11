package com.application.db.policies.rules

import com.application.ent.Problem
import com.application.ent.ReadOnlyEntClient
import entkt.query.isNull
import entkt.runtime.privacy.PrivacyRuleContext
import java.time.Instant

/** Checks parent visibility in one query for a batch of child rows. */
internal fun publishedProblemIds(
  context: PrivacyRuleContext<ReadOnlyEntClient>,
  problemIds: Collection<Long>,
): Set<Long> {
  if (problemIds.isEmpty()) return emptySet()

  return context.client.problems.query {
    where(Problem.id `in` problemIds.distinct())
    where(Problem.publishedAt lte Instant.now())
    where(Problem.archivedAt.isNull())
  }.all(context.viewerContext).getOrThrow().map { it.id }.toSet()
}
