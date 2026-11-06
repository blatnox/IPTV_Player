package com.blatnox.tvplayer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    @SerialName("category_id")
    @Serializable(with = StringOrIntSerializer::class)
    val categoryId: String = "",
    @SerialName("category_name")
    val categoryName: String = "",
    @SerialName("parent_id")
    val parentId: Int? = 0
)

@Serializable
data class StreamDto(
    @SerialName("num")
    val num: Int? = null,
    @SerialName("name")
    val name: String = "",
    @SerialName("stream_type")
    val streamType: String? = null,
    @SerialName("stream_id")
    @Serializable(with = IntOrStringSerializer::class)
    val streamId: Int = 0,
    @SerialName("stream_icon")
    val streamIcon: String? = null,
    @SerialName("epg_channel_id")
    val epgChannelId: String? = null,
    @SerialName("added")
    val added: String? = null,
    @SerialName("category_id")
    @Serializable(with = StringOrIntSerializer::class)
    val categoryId: String = "",
    @SerialName("custom_sid")
    val customSid: String? = null,
    @SerialName("tv_archive")
    val tvArchive: Int? = 0,
    @SerialName("direct_source")
    val directSource: String? = null,
    @SerialName("tv_archive_duration")
    val tvArchiveDuration: Int? = 0
)
