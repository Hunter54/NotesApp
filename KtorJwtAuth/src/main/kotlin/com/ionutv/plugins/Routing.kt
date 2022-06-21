package com.ionutv.plugins

import com.ionutv.*
import com.ionutv.data.user.UserDataSource
import com.ionutv.security.hashing.HashingService
import com.ionutv.security.token.TokenConfig
import com.ionutv.security.token.TokenService
import io.ktor.server.routing.*
import io.ktor.server.application.*

fun Application.configureRouting(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    routing {
        signIn(userDataSource, hashingService, tokenService, tokenConfig)
        signUp(hashingService, userDataSource)
        authenticate()
        getSecretInfo()
        checkUser(userDataSource)
    }
}
