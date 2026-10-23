package com.chuledigita.mobile.domain.repository

import com.chuledigita.mobile.domain.model.CustomerNote

/**
 * Domain repository contract defining data operations for business notes and customer narrative tracking.
 */
interface NoteRepository {
    /**
     * Retrieves all global shared family-business notes.
     */
    suspend fun getGlobalNotes(): Result<List<CustomerNote>>

    /**
     * Creates a new global shared family-business note.
     */
    suspend fun createGlobalNote(text: String): Result<CustomerNote>

    /**
     * Updates an existing global shared family-business note.
     */
    suspend fun updateGlobalNote(noteId: String, text: String): Result<CustomerNote>

    /**
     * Deletes a global shared family-business note.
     */
    suspend fun deleteGlobalNote(noteId: String): Result<Unit>

    /**
     * Deletes multiple global shared family-business notes.
     */
    suspend fun bulkDeleteGlobalNotes(noteIds: List<String>): Result<com.chuledigita.mobile.data.remote.model.BulkDeleteData>

    /**
     * Retrieves chronological communication or tracking notes for a customer profile.
     */
    suspend fun getCustomerNotes(customerId: String): Result<List<CustomerNote>>

    /**
     * Appends a real tracking text note entry into a customer's history logs.
     */
    suspend fun createCustomerNote(customerId: String, text: String): Result<CustomerNote>
}
