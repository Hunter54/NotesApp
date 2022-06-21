package com.ionutv.data.requests

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class AddNoteRequest(
    val title: String,
    val content: String,
    val date: Long,
    val owners: List<String>,
    val color: String
)