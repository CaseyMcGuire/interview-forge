package com.application.security

import entkt.runtime.privacy.ViewerContext

/** The identity whose access EntKt evaluates for an operation. */
sealed interface AppViewer {
  data object Anonymous : AppViewer
}

/** Public catalog reads and registration use the same permissions for every caller. */
internal val publicViewerContext: ViewerContext<AppViewer> = ViewerContext.AsViewer(AppViewer.Anonymous)
