package com.application.security

import java.security.Principal

/** Authenticated account identity without a password or a cached role. */
data class UserIdPrincipal(val userId: Long) : Principal {
  override fun getName(): String = userId.toString()
}
