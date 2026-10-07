package com.application.security

import com.application.generated.spa.routes.AppRoutes
import com.application.schema.UserRole
import com.sparouting.contract.AccessDecision
import org.springframework.stereotype.Component

@Component
class RequireAdminRouteAccess(private val currentUserService: CurrentUserService) {
  fun evaluate(): AccessDecision {
    val user = currentUserService.get()
    return when {
      user == null -> AccessDecision.Denied(destination = AppRoutes.Login())
      user.role != UserRole.ADMIN -> AccessDecision.Denied(destination = AppRoutes.Problems())
      else -> AccessDecision.Allowed
    }
  }
}
