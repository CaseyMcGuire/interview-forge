package com.application.security

import com.application.schema.UserRole
import io.github.caseymcguire.sparouting.spring.request.SpaRouteRequest
import io.github.caseymcguire.sparouting.spring.rules.SpaRouteRule
import io.github.caseymcguire.sparouting.spring.rules.SpaRouteRuleAction
import io.github.caseymcguire.sparouting.spring.rules.SpaRouteRuleResult
import org.springframework.stereotype.Component

@Component
class RequireAdminRouteRule(private val currentUser: CurrentUser) : SpaRouteRule {
  override fun evaluate(request: SpaRouteRequest): SpaRouteRuleResult {
    val user = currentUser.get()
    return when {
      user == null -> SpaRouteRuleResult.Deny(SpaRouteRuleAction.redirect("/login"))
      user.role != UserRole.ADMIN -> SpaRouteRuleResult.Deny(SpaRouteRuleAction.redirect("/problems"))
      else -> SpaRouteRuleResult.Allow
    }
  }
}
