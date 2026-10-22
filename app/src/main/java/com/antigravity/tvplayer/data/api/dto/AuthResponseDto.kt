package com.antigravity.tvplayer.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    @SerialName("user_info")
    val userInfo: UserInfoDto? = null,
    @SerialName("server_info")
    val serverInfo: ServerInfoDto? = null
)

@Serializable
data class UserInfoDto(
    @SerialName("username")
    val username: String? = null,
    @SerialName("password")
    val password: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("auth")
    val auth: Int? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("exp_date")
    val expDate: String? = null,
    @SerialName("is_trial")
    val isTrial: String? = null,
    @SerialName("active_cons")
    val activeCons: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("max_connections")
    val maxConnections: String? = null,
    @SerialName("allowed_output_formats")
    val allowedOutputFormats: List<String>? = null
)

@Serializable
data class ServerInfoDto(
    @SerialName("url")
    val url: String? = null,
    @SerialName("port")
    val port: String? = null,
    @SerialName("https_port")
    val httpsPort: String? = null,
    @SerialName("server_protocol")
    val serverProtocol: String? = null,
    @SerialName("timezone")
    val timezone: String? = null,
    @SerialName("timestamp_now")
    val timestampNow: Long? = null,
    @SerialName("time_now")
    val timeNow: String? = null
)
