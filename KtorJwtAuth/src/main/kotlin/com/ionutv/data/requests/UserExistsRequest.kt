package com.ionutv.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class UserExistsRequest(
    val email: String
)