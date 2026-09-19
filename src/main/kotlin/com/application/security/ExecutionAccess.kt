package com.application.security

import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext

/** Provides an internal viewer context for official and custom execution. */
internal object ExecutionAccess {
  val context = ViewerContext(Viewer.User(this))
}
