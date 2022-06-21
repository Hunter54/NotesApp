package com.ionutv.routes

import com.ionutv.data.*
import com.ionutv.data.collections.Note
import com.ionutv.data.requests.AddNoteRequest
import com.ionutv.data.requests.AddOwnerRequest
import com.ionutv.data.requests.DeleteNoteRequest
import com.ionutv.data.responses.SimpleResponse
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Route.noteRoutes(notesDataSource: MongoNotesDataSource) {
    route("/notes") {
        authenticate {
            get {
                val allNotes = notesDataSource.getAllNotes()
                call.respondHtml {
                    head {
                        styleLink("/static/css/styles.css")
                    }
                    body {
                        h1 {
                            +"All Notes"
                        }
                        for (note in allNotes) {
                            h3 {
                                +"${note.title} (Belongs to ${note.owners}):"
                            }
                            p {
                                +note.content
                            }
                            br
                        }
                    }
                }
            }
        }
    }
    route("/getNotes") {
        authenticate {
            get {
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.getClaim("userEmail", String::class)!!

                val notes = notesDataSource.getNotesForUser(email)
                call.respond(OK, notes)
            }
        }
    }
    route("/addOwnerToNote") {
        authenticate {
            post {
                val request = try {
                    call.receive<AddOwnerRequest>()
                } catch (e: ContentTransformationException) {
                    call.respond(BadRequest)
                    return@post
                }
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.getClaim("userEmail", String::class)!!
                if (!notesDataSource.checkIfUserExists(request.newOwner)) {
                    call.respond(
                        OK,
                        SimpleResponse(false, "No user with this E-Mail exists")
                    )
                    return@post
                }
                if (notesDataSource.isOwnerOfNote(request.noteID, request.newOwner)) {
                    call.respond(
                        OK,
                        SimpleResponse(false, "This user is already an owner of this note")
                    )
                    return@post
                }
                if (notesDataSource.addOwnerToNote(request.noteID, request.newOwner, email)) {
                    call.respond(
                        OK,
                        SimpleResponse(true, "${request.newOwner} can now see this note")
                    )
                } else {
                    call.respond(Conflict, "Not an owner or unknown error")
                }
            }
        }
    }
    route("/deleteNote") {
        authenticate {
            post {
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.getClaim("userEmail", String::class)!!
                val request = try {
                    call.receive<DeleteNoteRequest>()
                } catch (e: ContentTransformationException) {
                    call.respond(BadRequest)
                    return@post
                }
                if (notesDataSource.deleteNoteForUser(email, request.id)) {
                    call.respond(OK)
                } else {
                    call.respond(Conflict)
                }
            }
        }
    }
    route("/addNote") {
        authenticate {
            post {
                val requestNote = try {
                    call.receive<AddNoteRequest>()
                } catch (e: ContentTransformationException) {
                    call.respond(BadRequest)
                    return@post
                }
                val note = Note(
                    title = requestNote.title,
                    content = requestNote.content,
                    date = requestNote.date,
                    owners = requestNote.owners,
                    color = requestNote.color
                )
                if (notesDataSource.saveNote(note)) {
                    call.respond(OK)
                } else {
                    call.respond(Conflict)
                }
            }
        }
    }
}














