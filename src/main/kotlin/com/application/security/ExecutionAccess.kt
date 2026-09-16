package com.application.security

import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext

/** Provides an internal viewer context for official execution. */
internal object ExecutionAccess {
  val context = ViewerContext(Viewer.User(this))
}
