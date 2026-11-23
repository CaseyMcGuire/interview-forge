package com.application.security

import com.application.schema.UserRole

data class AuthenticatedUser(val id: Long, val role: UserRole) : AppViewer
