package com.commitpulse.app.github

import retrofit2.http.Body
import retrofit2.http.POST

interface GitHubApi {
    @POST("graphql")
    suspend fun graphql(@Body body: GraphQLRequest): GraphQLResponse
}
