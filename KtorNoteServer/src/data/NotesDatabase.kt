package com.ionutv.data

import com.ionutv.data.collections.Note
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.contains
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

class MongoNotesDataSource(
    db: CoroutineDatabase
) {
    private val notes = db.getCollection<Note>()
    private val httpClient by lazy {
        HttpClient(CIO)
    }

    suspend fun checkIfUserExists(email: String): Boolean {
        val response = httpClient.get("http://auth:8080/userExists") {
            contentType(ContentType.Application.Json)
            setBody("{\"email\":$email}")
        }
        if (response.status.isSuccess()) {
            return true
        }
        return false
    }

    suspend fun getAllNotes(): List<Note> {
        return notes.find().toFlow().toList()
    }

    suspend fun getNotesForUser(email: String): List<Note> {
        val list = mutableListOf<Note>()
        notes.find(Note::owners contains email).toFlow().collect {
            list.add(it)
        }
        return list
    }

    suspend fun saveNote(note: Note): Boolean {
        val noteExists = notes.findOneById(note.id) != null
        return if (noteExists) {
            notes.updateOneById(note.id, note).wasAcknowledged()
        } else {
            notes.insertOne(note).wasAcknowledged()
        }
    }

    suspend fun isOwnerOfNote(noteID: String, owner: String): Boolean {
        val note = notes.findOneById(noteID) ?: return false
        return owner in note.owners
    }

    suspend fun addOwnerToNote(noteID: String, owner: String, userEmail: String): Boolean {
        val owners = notes.findOneById(noteID)?.owners ?: return false
        if (!owners.contains(userEmail)) return false
        return notes.updateOneById(noteID, setValue(Note::owners, owners + owner)).wasAcknowledged()
    }

    suspend fun deleteNoteForUser(email: String, noteID: String): Boolean {
        val note = notes.findOne(Note::id eq noteID, Note::owners contains email)
        note?.let { note ->
            if (note.owners.size > 1) {
                // the note has multiple owners, so we just delete the email from the owners list
                val newOwners = note.owners - email
                val updateResult = notes.updateOne(Note::id eq note.id, setValue(Note::owners, newOwners))
                return updateResult.wasAcknowledged()
            }
            return notes.deleteOneById(note.id).wasAcknowledged()
        } ?: return false
    }
}











