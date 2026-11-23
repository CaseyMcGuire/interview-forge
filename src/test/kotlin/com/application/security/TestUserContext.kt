package com.application.security

import com.application.schema.UserRole
import entkt.runtime.privacy.ViewerContext

internal fun testUserContext(id: Long, role: UserRole = UserRole.USER): ViewerContext.AsViewer<AuthenticatedUser> =
  ViewerContext.AsViewer(AuthenticatedUser(id, role))
