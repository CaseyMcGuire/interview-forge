package com.application.security

import com.application.db.models.UserDetailsImpl
import com.application.ent.EntClient
import com.application.ent.ReadOnlyEntClient
import com.application.ent.User
import com.application.ent.UserLoadPrivacyRule
import com.application.schema.UserRole
import entkt.runtime.privacy.PrivacyDecision
import entkt.runtime.privacy.PrivacyRuleContext
import entkt.runtime.privacy.ViewerContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

/** Uses the authenticated identity, but reads current permissions from the database. */
@Service
class CurrentUserService(
  // Privacy rules depend on this service, so resolve their EntClient after policy registration.
  private val entClient: ObjectProvider<EntClient>,
) {
  /** Privacy rules supply their client so identity reads stay inside the rule's transaction. */
  fun get(client: ReadOnlyEntClient? = null): AuthenticatedUser? {
    val authentication = SecurityContextHolder.getContext().authentication ?: return null
    if (!authentication.isAuthenticated) {
      return null
    }

    val userId = when (val principal = authentication.principal) {
      is UserDetailsImpl -> principal.user.id
      is UserIdPrincipal -> principal.userId
      else -> null
    } ?: return null

    val context = ViewerContext.AsViewer(CurrentUserLookup(userId))
    val result = if (client != null) {
      client.users.findById(context, userId)
    } else {
      entClient.getObject().users.findById(context, userId)
    }
    val user = result.getOrThrow() ?: return null

    // EntKt loads the complete entity; credentials must not leave this lookup.
    return AuthenticatedUser(user.id, user.role)
  }

  fun requireAdmin(): AuthenticatedUser = get()?.takeIf { it.role == UserRole.ADMIN }
    ?: throw AccessDeniedException("Administrator access is required")

  /** Match the operation's user to the session and read their current role within the rule's transaction. */
  fun getForViewer(viewer: AppViewer, client: ReadOnlyEntClient?): AuthenticatedUser? {
    if (viewer !is AuthenticatedUser) {
      return null
    }
    return get(client)?.takeIf { it.id == viewer.id }
  }

  fun isAdmin(viewer: AppViewer, client: ReadOnlyEntClient? = null): Boolean =
    getForViewer(viewer, client)?.role == UserRole.ADMIN

  /** Only this service can create the lookup identity; it permits reading one account. */
  private data class CurrentUserLookup(val userId: Long) : AppViewer

  internal class AllowIfCurrentUserLookupReadRule : UserLoadPrivacyRule {
    override fun run(
      context: PrivacyRuleContext<AppViewer, ReadOnlyEntClient>,
      item: User,
    ): PrivacyDecision {
      val lookup = context.viewer as? CurrentUserLookup

      return if (lookup?.userId == item.id) {
        PrivacyDecision.Allow
      } else {
        PrivacyDecision.Deny("User reads are restricted to internal identity lookup")
      }
    }
  }
}
