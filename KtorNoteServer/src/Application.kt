package com.ionutv

import com.ionutv.data.MongoNotesDataSource
import com.ionutv.routes.noteRoutes
import com.ionutv.security.TokenConfig
import com.ionutv.security.configureSecurity
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    configureMonitoring()
    val mongoPw = environment.config.property("db.dbPassword").getString()
    val dbName = environment.config.property("db.dbName").getString()
    val db = KMongo.createClient(
        connectionString = "mongodb+srv://ionutvalase:$mongoPw@cluster0.zmfmjrc.mongodb.net/${dbName}?retryWrites=true&w=majority"
    ).coroutine
        .getDatabase(dbName)
    val userDataSource = MongoNotesDataSource(db)

    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = environment.config.property("jwt.secret").getString()
    )

    routing {
        noteRoutes(userDataSource)
    }
    configureSecurity(tokenConfig)
}
fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.DEBUG
        filter { call -> call.request.path().startsWith("/") }
    }

}










