package com.commitpulse.app.github

import kotlinx.serialization.Serializable

@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String>,
)

@Serializable
data class GraphQLResponse(
    val data: ViewerData? = null,
    val errors: List<GraphQLError>? = null,
)

@Serializable
data class GraphQLError(val message: String)

@Serializable
data class ViewerData(val viewer: Viewer)

@Serializable
data class Viewer(
    val login: String,
    val avatarUrl: String,
    val contributionsCollection: ContributionsCollection,
)

@Serializable
data class ContributionsCollection(val contributionCalendar: ContributionCalendar)

@Serializable
data class ContributionCalendar(
    val totalContributions: Int,
    val weeks: List<ContributionWeek>,
)

@Serializable
data class ContributionWeek(val contributionDays: List<ContributionDay>)

@Serializable
data class ContributionDay(val date: String, val contributionCount: Int)

const val CONTRIBUTIONS_QUERY = """
query(${'$'}from: DateTime!, ${'$'}to: DateTime!) {
  viewer {
    login
    avatarUrl
    contributionsCollection(from: ${'$'}from, to: ${'$'}to) {
      contributionCalendar {
        totalContributions
        weeks {
          contributionDays {
            date
            contributionCount
          }
        }
      }
    }
  }
}
"""
