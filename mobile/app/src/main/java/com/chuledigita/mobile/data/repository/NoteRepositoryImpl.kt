package com.chuledigita.mobile.data.repository

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.model.CreateNoteRequest
import com.chuledigita.mobile.data.remote.model.ApiErrorResponse
import com.chuledigita.mobile.domain.model.CustomerNote
import com.chuledigita.mobile.domain.repository.NoteRepository
import com.chuledigita.mobile.utils.DateTimeUtils
import com.google.gson.Gson

/**
 * Implementation of [NoteRepository] that communicates with the backend API.
 * Handles both customer-specific and global shared business notes.
 */
class NoteRepositoryImpl(
    private val apiService: ApiService
) : NoteRepository {

    private val gson = Gson()

    override suspend fun getGlobalNotes(): Result<List<CustomerNote>> {
        return try {
            val response = apiService.getAllGlobalNotes()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val notes = response.body()?.data?.map { dto ->
                    CustomerNote(
                        id = dto.id.toString(),
                        customerId = "", // Global notes are not tied to a customer
                        note = dto.noteText,
                        date = DateTimeUtils.formatDate(dto.createdAt),
                        time = DateTimeUtils.formatTime(dto.createdAt),
                        authorName = dto.createdByName ?: "የቤተሰብ አባል"
                    )
                } ?: emptyList()
                Result.success(notes)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ማስታወሻዎችን መጫን አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል። እባክዎ ግንኙነትዎን ያረጋግጡ።"))
        }
    }

    override suspend fun createGlobalNote(text: String): Result<CustomerNote> {
        return try {
            val response = apiService.createGlobalNote(CreateNoteRequest(noteText = text))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    val note = CustomerNote(
                        id = dto.id.toString(),
                        customerId = "",
                        note = dto.noteText,
                        date = DateTimeUtils.formatDate(dto.createdAt),
                        time = DateTimeUtils.formatTime(dto.createdAt),
                        authorName = dto.createdByName ?: "የቤተሰብ አባል"
                    )
                    Result.success(note)
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ማስታወሻውን መመዝገብ አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል። እባክዎ ግንኙነትዎን ያረጋግጡ።"))
        }
    }

    override suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>> {
        return try {
            val idLong = customerId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የደንበኛ መለያ"))
            val response = apiService.getCustomerNotes(idLong)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val notes = response.body()?.data?.map { dto ->
                    CustomerNote(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        note = dto.noteText,
                        date = DateTimeUtils.formatDate(dto.createdAt),
                        time = DateTimeUtils.formatTime(dto.createdAt),
                        authorName = dto.createdByName ?: "የቤተሰብ አባል"
                    )
                } ?: emptyList()
                Result.success(notes)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "የደንበኛውን ማስታወሻዎች መጫን አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል። እባክዎ ግንኙነትዎን ያረጋግጡ።"))
        }
    }

    override suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote> {
        return try {
            val idLong = customerId.toLongOrNull() ?: return Result.failure(Exception("ትክክለኛ ያልሆነ የደንበኛ መለያ"))
            val response = apiService.createCustomerNote(idLong, CreateNoteRequest(noteText = text))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    val note = CustomerNote(
                        id = dto.id.toString(),
                        customerId = dto.customerId.toString(),
                        note = dto.noteText,
                        date = DateTimeUtils.formatDate(dto.createdAt),
                        time = DateTimeUtils.formatTime(dto.createdAt),
                        authorName = dto.createdByName ?: "የቤተሰብ አባል"
                    )
                    Result.success(note)
                } else {
                    Result.failure(Exception("ባዶ ምላሽ ደርሷል።"))
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), "ማስታወሻውን ማስቀመጥ አልተቻለም።")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("የአውታረ መረብ ችግር አጋጥሟል። እባክዎ ግንኙነትዎን ያረጋግጡ።"))
        }
    }

    private fun parseErrorMessage(errorBodyJson: String?, fallback: String): String {
        if (errorBodyJson.isNullOrBlank()) return fallback
        return try {
            val errorObj = gson.fromJson(errorBodyJson, ApiErrorResponse::class.java)
            errorObj.message ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }
}
