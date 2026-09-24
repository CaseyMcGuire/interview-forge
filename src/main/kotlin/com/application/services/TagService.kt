package com.application.services

import com.application.ent.EntClient
import com.application.ent.Tag
import com.application.security.CurrentUserService
import entkt.runtime.privacy.Viewer
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntConstraintViolationException
import org.springframework.stereotype.Service

@Service
class TagService(
  private val entClient: EntClient,
  private val currentUserService: CurrentUserService,
) {
  private val publicContext = ViewerContext(Viewer.Anonymous)

  fun findTags(): List<Tag> = entClient.tags.query {
    orderBy(Tag.displayName.asc())
    orderBy(Tag.slug.asc())
  }.all(publicContext).getOrThrow()

  fun createTag(slug: String, displayName: String): Tag {
    val context = ViewerContext(Viewer.User(currentUserService.requireAdmin().id))

    return try {
      entClient.tags.create {
        this.slug = slug
        this.displayName = displayName
      }.saveAndLoad(context).getOrThrow()
    } catch (exception: EntConstraintViolationException) {
      if (exception.driverCode == "23505" && exception.constraint == "idx_tags_slug_unique") {
        throw TagInputException("slug", "A tag with this slug already exists")
      }

      throw exception
    }
  }

  fun updateTag(id: Long, displayName: String): Tag {
    val context = ViewerContext(Viewer.User(currentUserService.requireAdmin().id))

    return entClient.tags.update(id) {
      this.displayName = displayName
    }.saveAndLoad(context).getOrThrow()
  }

  fun deleteTag(id: Long): Boolean {
    val context = ViewerContext(Viewer.User(currentUserService.requireAdmin().id))

    return entClient.tags.deleteById(context, id).getOrThrow()
  }
}
