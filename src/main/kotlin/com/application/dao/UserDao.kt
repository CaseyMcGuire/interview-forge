package com.application.dao

import com.application.ent.EntClient
import com.application.services.User
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import org.springframework.stereotype.Component
import com.application.ent.User as EntUser

@Component
class UserDao(private val entClient: EntClient) {

  // Login must read credentials before a viewer is authenticated. Keep this context private to
  // the server's credential lookup boundary; ordinary viewers cannot read password hashes.
  private val credentialLookupContext = ViewerContext.privacyBypass_DANGEROUS(
    "Internal credential lookup before Spring Security authentication"
  )

  fun findByEmail(email: String): User? {
    return entClient.users.query {
      where(EntUser.email eq email)
    }.firstOrNull(credentialLookupContext).getOrThrow()?.toUser()
  }

  fun createUser(email: String, hashedPassword: String): User {
    entClient.users.create {
      this.email = email
      this.hashedPassword = hashedPassword
    }.save(ViewerContext(Viewer.Anonymous)).getOrThrow()
    // Registration has CREATE permission, not LOAD permission for credential entities.
    return User(email, hashedPassword, null)
  }

  private fun EntUser.toUser() = User(email, hashedPassword, null)
}
