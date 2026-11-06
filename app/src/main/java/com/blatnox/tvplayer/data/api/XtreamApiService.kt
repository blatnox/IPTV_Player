package com.blatnox.tvplayer.data.api

import com.blatnox.tvplayer.data.api.dto.AuthResponseDto
import com.blatnox.tvplayer.data.api.dto.CategoryDto
import com.blatnox.tvplayer.data.api.dto.StreamDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamApiService {

    @GET
    suspend fun authenticate(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): AuthResponseDto

    @GET
    suspend fun getLiveCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<CategoryDto>

    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<StreamDto>
}
