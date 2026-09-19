package com.application.config

import com.application.ent.EntClient
import com.application.ent.GeneratedEntViewerRegistry
import com.application.schema.UserRole
import com.application.security.AuthenticatedUser
import com.application.security.CurrentUser
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.viewer.EntViewer
import entkt.viewer.spring.EntViewerPrincipalResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EntViewerConfiguration {
  @Bean
  fun entViewer(client: EntClient): EntViewer<EntClient> =
    EntViewer(client, GeneratedEntViewerRegistry) {
      path = "/_ent"
      authorize { request -> (request.principal as? AuthenticatedUser)?.role == UserRole.ADMIN }

      // Admin access to the viewer does not bypass the entities' normal read policies.
      viewerContext { request ->
        val user = checkNotNull(request.principal as? AuthenticatedUser)
        ViewerContext(Viewer.User(user.id))
      }
    }

  @Bean
  fun entViewerPrincipalResolver(currentUser: CurrentUser): EntViewerPrincipalResolver =
    EntViewerPrincipalResolver { currentUser.get() }
}
