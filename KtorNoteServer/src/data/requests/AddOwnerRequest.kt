package com.ionutv.data.requests

data class AddOwnerRequest(
    val noteID: String,
    val newOwner: String
)