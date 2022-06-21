package com.ionutv

import com.ionutv.data.user.MongoUserDataSource
import com.ionutv.data.user.User
import io.ktor.server.application.*
import com.ionutv.plugins.*
import com.ionutv.security.hashing.SHA256HashingService
import com.ionutv.security.token.JwtTokenService
import com.ionutv.security.token.TokenConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    val mongoPw = environment.config.property("db.dbPassword").getString()
    val dbName = environment.config.property("db.dbName").getString()
    val db = KMongo.createClient(
        connectionString = "mongodb+srv://ionutvalase:$mongoPw@cluster0.zmfmjrc.mongodb.net/${dbName}?retryWrites=true&w=majority"
    ).coroutine
        .getDatabase(dbName)
    val userDataSource = MongoUserDataSource(db)

    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = environment.config.property("jwt.secret").getString()
    )

    val hashingService = SHA256HashingService()

    configureRouting(userDataSource, hashingService, tokenService, tokenConfig)
    configureSerialization()
    configureMonitoring()
    configureSecurity(tokenConfig)
}
