package com.application.graphql

import com.application.graphql.types.CreateTagInput
import com.application.graphql.types.CreateTagResult
import com.application.graphql.types.CreateTagSuccess
import com.application.graphql.types.DeleteTagInput
import com.application.graphql.types.DeleteTagResult
import com.application.graphql.types.DeleteTagSuccess
import com.application.graphql.types.FieldError
import com.application.graphql.types.Tag
import com.application.graphql.types.TagForbidden
import com.application.graphql.types.TagNotFound
import com.application.graphql.types.TagValidationFailure
import com.application.graphql.types.UpdateTagInput
import com.application.graphql.types.UpdateTagResult
import com.application.graphql.types.UpdateTagSuccess
import com.application.services.TagInputException
import com.application.services.TagService
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import entkt.runtime.result.EntMutationPrivacyDeniedException
import entkt.runtime.result.EntTargetAbsentException
import entkt.runtime.result.EntValidationException
import org.springframework.security.access.AccessDeniedException
import com.application.ent.Tag as TagEntity

@DgsComponent
class TagDataFetcher(
  private val tagService: TagService,
  private val globalIdUtil: GlobalIdUtil,
) {
  @DgsQuery
  fun tags(): List<Tag> = tagService.findTags().map { it.toGraphqlTag(globalIdUtil) }

  @DgsMutation
  fun createTag(@InputArgument input: CreateTagInput): CreateTagResult = try {
    val tag = tagService.createTag(input.slug, input.displayName)
    CreateTagSuccess(tag.toGraphqlTag(globalIdUtil))
  } catch (_: AccessDeniedException) {
    forbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    forbidden()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: TagInputException) {
    validationFailure(exception)
  }

  @DgsMutation
  fun updateTag(@InputArgument input: UpdateTagInput): UpdateTagResult = try {
    val tag = tagService.updateTag(tagId(input.id), input.displayName)
    UpdateTagSuccess(tag.toGraphqlTag(globalIdUtil))
  } catch (_: AccessDeniedException) {
    forbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    forbidden()
  } catch (_: EntTargetAbsentException) {
    notFound()
  } catch (exception: EntValidationException) {
    validationFailure(exception)
  } catch (exception: TagInputException) {
    validationFailure(exception)
  }

  @DgsMutation
  fun deleteTag(@InputArgument input: DeleteTagInput): DeleteTagResult = try {
    if (tagService.deleteTag(tagId(input.id))) {
      DeleteTagSuccess(deletedTagId = input.id)
    } else {
      notFound()
    }
  } catch (_: AccessDeniedException) {
    forbidden()
  } catch (_: EntMutationPrivacyDeniedException) {
    forbidden()
  } catch (_: EntTargetAbsentException) {
    notFound()
  } catch (exception: TagInputException) {
    validationFailure(exception)
  }

  private fun tagId(id: String): Long = globalIdUtil.fromGlobalIdOrNull(id, Tag::class)
    ?: throw TagInputException("id", "Provide a valid tag ID")

  private fun forbidden() = TagForbidden("Administrator access is required")

  private fun notFound() = TagNotFound("Tag does not exist")

  private fun validationFailure(exception: EntValidationException) = TagValidationFailure(
    message = "Invalid tag",
    fieldErrors = exception.violations.map { FieldError(field = it.field.orEmpty(), message = it.message) },
  )

  private fun validationFailure(exception: TagInputException) = TagValidationFailure(
    message = "Invalid tag",
    fieldErrors = listOf(FieldError(field = exception.field, message = exception.message)),
  )
}

internal fun TagEntity.toGraphqlTag(globalIdUtil: GlobalIdUtil): Tag = Tag(
  id = globalIdUtil.toGlobalId(Tag::class, id),
  slug = slug,
  displayName = displayName,
)
