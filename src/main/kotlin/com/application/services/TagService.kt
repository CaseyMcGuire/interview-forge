package com.application.services

import com.application.ent.EntClient
import com.application.ent.Tag
import com.application.schema.TagType
import com.application.security.CurrentUserService
import com.application.security.publicViewerContext
import entkt.runtime.privacy.ViewerContext
import entkt.runtime.result.EntConstraintViolationException
import org.springframework.stereotype.Service

@Service
class TagService(
  private val entClient: EntClient,
  private val currentUserService: CurrentUserService,
) {
  fun findTags(): List<Tag> = entClient.tags.query {
    orderBy(Tag.displayName.asc())
    orderBy(Tag.slug.asc())
  }.all(publicViewerContext).getOrThrow()

  fun createTag(slug: String, displayName: String, type: TagType): Tag {
    val context = ViewerContext.AsViewer(currentUserService.requireAdmin())

    return try {
      entClient.tags.create {
        this.slug = slug
        this.displayName = displayName
        this.type = type
      }.saveAndLoad(context).getOrThrow()
    } catch (exception: EntConstraintViolationException) {
      if (exception.driverCode == "23505" && exception.constraint == "idx_tags_slug_unique") {
        throw TagInputException("slug", "A tag with this slug already exists")
      }

      throw exception
    }
  }

  fun updateTag(id: Long, displayName: String): Tag {
    val context = ViewerContext.AsViewer(currentUserService.requireAdmin())

    return entClient.tags.update(id) {
      this.displayName = displayName
    }.saveAndLoad(context).getOrThrow()
  }

  fun deleteTag(id: Long): Boolean {
    val context = ViewerContext.AsViewer(currentUserService.requireAdmin())

    return entClient.tags.deleteById(context, id).getOrThrow()
  }
}
