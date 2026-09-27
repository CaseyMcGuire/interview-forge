package com.application.graphql

import com.application.execution.LanguageExecutionConfig
import com.application.graphql.types.Language
import com.application.graphql.types.TestDriverTypeInfo
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment

@DgsComponent
class LanguageDataFetcher(languageExecutionConfigs: List<LanguageExecutionConfig>) {
  private val languageConfigurations = languageExecutionConfigs.associateBy { it.key }

  @DgsData(parentType = DgsConstants.LANGUAGE.TYPE_NAME, field = DgsConstants.LANGUAGE.TestDriverTypes)
  fun testDriverTypes(environment: DgsDataFetchingEnvironment): List<TestDriverTypeInfo> {
    val language = checkNotNull(environment.getSource<Language>())

    return languageConfigurations[language.key]?.testDriverTypes.orEmpty().map { documentation ->
      TestDriverTypeInfo(
        type = documentation.type.toGraphqlType(),
        displayName = documentation.displayName,
        documentationMarkdown = documentation.documentationMarkdown,
      )
    }
  }
}
