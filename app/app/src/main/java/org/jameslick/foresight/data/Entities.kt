package org.jameslick.foresight.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/*
 * Room entities, one per table in spec §3.6. Ids are UUID strings so exports and card QR codes never
 * expose a sequential count. Anything personal (contact, signature, images) is encrypted before it reaches
 * the database or the filesystem (see KeyStoreCrypto / ImageStore), and the database file itself is
 * SQLCipher-encrypted (ForesightDatabase).
 */

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val locationLabel: String,
    /** epoch millis */
    val date: Long,
    /** comma-separated screener ids (students' chosen handles, never full names) */
    val screenerIds: String = "",
    val notes: String = "",
    /** hand-entered per event for the dashboard map (spec §5); null until entered */
    val lat: Double? = null,
    val lng: Double? = null,
)

@Entity(
    tableName = "participants",
    foreignKeys = [ForeignKey(entity = Event::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId")],
)
data class Participant(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val createdAt: Long,
    /** "en" | "es" | "vi" */
    val language: String,
    /** "under18" | "18_39" | "40_59" | "60_plus" | null */
    val ageRange: String? = null,
    /** "yes" | "no" | "not_sure" | null */
    val dxDiabetes: String? = null,
    /** "within_1y" | "1_2y" | "over_2y" | "never" | null */
    val lastExam: String? = null,
    /** true | false | null */
    val hasDoctor: Boolean? = null,
    /** AES-GCM ciphertext of the phone number; null when not given or after "Delete my record" */
    val contactEncrypted: ByteArray? = null,
    /** default false (spec §3.3 Q6) */
    val consentShareImages: Boolean = false,
    val consentSignatureBlob: ByteArray? = null,
    /** set by "Delete my record": images, contact and inferences are gone; this row keeps an anonymous count */
    val deleted: Boolean = false,
)

@Entity(
    tableName = "captures",
    foreignKeys = [ForeignKey(entity = Participant::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("participantId")],
)
data class Capture(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val participantId: String,
    /** "R" | "L" */
    val eye: String,
    /** path of the AES-GCM encrypted image file under filesDir/images; null once purged */
    val imagePathEncrypted: String?,
    val qualityScore: Float? = null,
    val gradable: Boolean,
    val retakeN: Int = 0,
    val deviceModel: String,
    val focusCal: Float? = null,
    val createdAt: Long,
    /** "camera" | "gallery_import" — the Phase 1 flow imports images */
    val source: String = "camera",
)

@Entity(
    tableName = "inferences",
    foreignKeys = [ForeignKey(entity = Capture::class, parentColumns = ["id"], childColumns = ["captureId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("captureId")],
)
data class Inference(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val captureId: String,
    val modelName: String,
    val modelVersion: String,
    /** 0..4 argmax grade */
    val drGrade: Int,
    /** JSON array of the 5 probabilities */
    val drProbs: String,
    val referableProb: Float,
    /** JSON of experimental flags (Phase 4); "{}" until then */
    val experimentalJson: String = "{}",
    val heatmapPath: String? = null,
    val runtimeMs: Long,
)

@Entity(
    tableName = "referrals",
    foreignKeys = [ForeignKey(entity = Participant::class, parentColumns = ["id"], childColumns = ["participantId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("participantId")],
)
data class Referral(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val participantId: String,
    /** "green" | "yellow" | "red" | "ungradable" */
    val outcome: String,
    val clinicId: String? = null,
    val cardPrinted: Boolean = false,
    /** "none" | "pending" | "completed" | "declined" */
    val followUpStatus: String = "none",
    /** short code printed on the card and QR so the clinic can match the follow-up without any name */
    val cardCode: String,
)
