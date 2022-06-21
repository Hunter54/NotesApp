package com.ionutv

import com.ionutv.data.requests.AuthRequest
import com.ionutv.data.requests.UserExistsRequest
import com.ionutv.data.responses.AuthResponse
import com.ionutv.data.user.User
import com.ionutv.data.user.UserDataSource
import com.ionutv.security.hashing.HashingService
import com.ionutv.security.hashing.SaltedHash
import com.ionutv.security.token.TokenClaim
import com.ionutv.security.token.TokenConfig
import com.ionutv.security.token.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.commons.codec.digest.DigestUtils

fun Route.signUp(
    hashingService: HashingService,
    userDataSource: UserDataSource
) {
    post("signup") {
        val request = call.receiveOrNull<AuthRequest>() ?: kotlin.run {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }
        val emailRegex = "^(.+)@(.+)\$"
        val areFieldsInvalid = !emailRegex.toRegex().matches(request.username) || request.password.isBlank()
        val isPwTooShort = request.password.length < 8
        if (areFieldsInvalid || isPwTooShort) {
            call.respond(HttpStatusCode.Conflict, "Invalid data")
            return@post
        }

        val saltedHash = hashingService.generateSaltedHash(request.password)
        val user = User(
            username = request.username,
            password = saltedHash.hash,
            salt = saltedHash.salt
        )
        val wasAcknowledged = userDataSource.insertUser(user)
        if (!wasAcknowledged) {
            call.respond(HttpStatusCode.Conflict)
            return@post
        }

        call.respond(HttpStatusCode.OK)
    }
}

fun Route.signIn(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    post("signin") {
        val request = call.receiveOrNull<AuthRequest>() ?: kotlin.run {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        val user = userDataSource.getUserByUsername(request.username)
        if (user == null) {
            call.respond(HttpStatusCode.Conflict, "Incorrect username or password")
            return@post
        }

        val isValidPassword = hashingService.verify(
            value = request.password,
            saltedHash = SaltedHash(
                hash = user.password,
                salt = user.salt
            )
        )
        if (!isValidPassword) {
            println("Entered hash: ${DigestUtils.sha256Hex("${user.salt}${request.password}")}, Hashed PW: ${user.password}")
            call.respond(HttpStatusCode.Conflict, "Incorrect username or password")
            return@post
        }

        val token = tokenService.generate(
            config = tokenConfig,
            TokenClaim(
                name = "userId",
                value = user.id
            ),
            TokenClaim(
                name = "userEmail",
                value = user.username
            )
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = AuthResponse(
                token = token
            )
        )
    }
}

fun Route.checkUser(userDataSource: UserDataSource) {
    get("userExists") {
        val request = call.receiveOrNull<UserExistsRequest>() ?: kotlin.run {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }
        userDataSource.getUserByUsername(request.email)?.let {
            call.respond(HttpStatusCode.OK)
        } ?: kotlin.run {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}

fun Route.authenticate() {
    authenticate {
        get("authenticate") {
            call.respond(HttpStatusCode.OK)
        }
    }
}

fun Route.getSecretInfo() {
    authenticate {
        get("secret") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.getClaim("userId", String::class)
            val email = principal?.getClaim("userEmail", String::class)
            call.respond(HttpStatusCode.OK, "Your userId is $userId with email $email")
        }
    }
}