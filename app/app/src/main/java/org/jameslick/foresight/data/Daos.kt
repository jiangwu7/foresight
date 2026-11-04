package org.jameslick.foresight.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(event: Event)
    @Update suspend fun update(event: Event)
    @Query("SELECT * FROM events ORDER BY date DESC") fun all(): Flow<List<Event>>
    @Query("SELECT * FROM events WHERE id = :id") suspend fun byId(id: String): Event?
    @Query("SELECT COUNT(*) FROM participants WHERE eventId = :eventId") suspend fun participantCount(eventId: String): Int
}

@Dao
interface ParticipantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: Participant)
    @Update suspend fun update(p: Participant)
    @Query("SELECT * FROM participants WHERE id = :id") suspend fun byId(id: String): Participant?
    @Query("SELECT * FROM participants WHERE eventId = :eventId ORDER BY createdAt") suspend fun byEvent(eventId: String): List<Participant>
    @Query("SELECT * FROM participants WHERE eventId = :eventId AND consentShareImages = 1 AND deleted = 0") suspend fun consentedByEvent(eventId: String): List<Participant>

    /** "Delete my record" (spec §6): strip everything personal, keep an anonymous row for the count. */
    @Query("UPDATE participants SET contactEncrypted = NULL, consentSignatureBlob = NULL, consentShareImages = 0, deleted = 1 WHERE id = :id")
    suspend fun anonymize(id: String)
}

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(c: Capture)
    @Update suspend fun update(c: Capture)
    @Query("SELECT * FROM captures WHERE participantId = :participantId ORDER BY createdAt") suspend fun byParticipant(participantId: String): List<Capture>
    @Query("SELECT * FROM captures WHERE imagePathEncrypted IS NOT NULL") suspend fun withImages(): List<Capture>
    @Query("UPDATE captures SET imagePathEncrypted = NULL WHERE id = :id") suspend fun clearImage(id: String)
    @Query("DELETE FROM captures WHERE participantId = :participantId") suspend fun deleteByParticipant(participantId: String)
}

@Dao
interface InferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(i: Inference)
    @Query("SELECT * FROM inferences WHERE captureId = :captureId ORDER BY rowid DESC LIMIT 1") suspend fun latestForCapture(captureId: String): Inference?
    @Query("DELETE FROM inferences WHERE captureId IN (SELECT id FROM captures WHERE participantId = :participantId)") suspend fun deleteByParticipant(participantId: String)
}

@Dao
interface ReferralDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(r: Referral)
    @Update suspend fun update(r: Referral)
    @Query("SELECT * FROM referrals WHERE participantId = :participantId") suspend fun byParticipant(participantId: String): Referral?
    @Query("SELECT * FROM referrals WHERE participantId IN (SELECT id FROM participants WHERE eventId = :eventId)") suspend fun byEvent(eventId: String): List<Referral>
    @Query("DELETE FROM referrals WHERE participantId = :participantId") suspend fun deleteByParticipant(participantId: String)
}

/** One row per participant with everything the de-identified export needs, computed in SQL so nothing personal is read. */
data class DeidRow(
    val participantId: String,
    val language: String,
    val ageRange: String?,
    val dxDiabetes: String?,
    val lastExam: String?,
    val hasDoctor: Boolean?,
    val deleted: Boolean,
    val outcome: String?,
    val cardPrinted: Boolean?,
    val followUpStatus: String?,
    val gradableEyes: Int,
    val capturedEyes: Int,
)

@Dao
interface ExportDao {
    @Transaction
    @Query(
        """
        SELECT p.id AS participantId, p.language, p.ageRange, p.dxDiabetes, p.lastExam, p.hasDoctor, p.deleted,
               r.outcome, r.cardPrinted, r.followUpStatus,
               (SELECT COUNT(*) FROM captures c WHERE c.participantId = p.id AND c.gradable = 1) AS gradableEyes,
               (SELECT COUNT(*) FROM captures c WHERE c.participantId = p.id) AS capturedEyes
        FROM participants p LEFT JOIN referrals r ON r.participantId = p.id
        WHERE p.eventId = :eventId ORDER BY p.createdAt
        """
    )
    suspend fun deidRows(eventId: String): List<DeidRow>
}
