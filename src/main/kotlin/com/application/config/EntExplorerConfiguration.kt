package com.application.config

import com.application.ent.EntClient
import com.application.ent.GeneratedEntExplorerRegistry
import com.application.schema.UserRole
import com.application.security.AppViewer
import com.application.security.AuthenticatedUser
import com.application.security.CurrentUserService
import entkt.explorer.EntExplorer
import entkt.explorer.spring.EntExplorerPrincipalResolver
import entkt.runtime.privacy.ViewerContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EntExplorerConfiguration {
  @Bean
  fun entExplorer(client: EntClient): EntExplorer<AppViewer, EntClient> =
    EntExplorer(client, GeneratedEntExplorerRegistry) {
      path = "/_ent"
      authorize { request -> (request.principal as? AuthenticatedUser)?.role == UserRole.ADMIN }

      // Explorer reads use the signed-in administrator's ordinary entity permissions.
      viewerContext { request ->
        val user = checkNotNull(request.principal as? AuthenticatedUser)
        ViewerContext.AsViewer(user)
      }
    }

  @Bean
  fun entExplorerPrincipalResolver(currentUserService: CurrentUserService): EntExplorerPrincipalResolver =
    EntExplorerPrincipalResolver { currentUserService.get() }
}
