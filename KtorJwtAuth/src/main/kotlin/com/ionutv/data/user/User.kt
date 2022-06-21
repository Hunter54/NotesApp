package com.ionutv.data.user

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    val username: String,
    val password: String,
    val salt: String,
    @BsonId val id: String = ObjectId().toString()
)