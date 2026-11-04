package org.jameslick.foresight.data

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.jameslick.foresight.ml.DrModelRunner
import org.jameslick.foresight.ml.EyeOutcome
import org.jameslick.foresight.ml.EyeResult
import org.jameslick.foresight.ml.OutcomeMapper
import org.jameslick.foresight.ml.OverallOutcome
import org.jameslick.foresight.ml.QualityHeuristic
import java.io.File
import java.security.SecureRandom

/**
 * The one place the screening flow talks to storage and the model. Every method runs on Dispatchers.IO.
 * Nothing here has a network call, by design (spec §6).
 */
class ScreeningRepository(private val context: Context) {
    private val db = ForesightDatabase.get(context)
    private val images = ImageStore(context)
    private val crypto = KeyStoreCrypto.get()
    private val runner: DrModelRunner by lazy { DrModelRunner.load(context) }

    val modelCard get() = runner.card

    /** Decrypts a stored capture image for display on the results screen (never leaves the process). */
    fun loadImage(fileName: String?): Bitmap? = fileName?.let { images.load(it) }

    // ---- events & participants ---------------------------------------------------------------
    suspend fun createEvent(name: String, location: String, now: Long = System.currentTimeMillis()): Event = withContext(Dispatchers.IO) {
        Event(name = name, locationLabel = location, date = now).also { db.events().upsert(it) }
    }

    fun events() = db.events().all()

    suspend fun startParticipant(eventId: String, language: String, now: Long = System.currentTimeMillis()): Participant = withContext(Dispatchers.IO) {
        Participant(eventId = eventId, createdAt = now, language = language).also { db.participants().upsert(it) }
    }

    suspend fun saveIntake(
        participantId: String, ageRange: String?, dxDiabetes: String?, lastExam: String?, hasDoctor: Boolean?,
        contactPhone: String?, consentShareImages: Boolean,
    ) = withContext(Dispatchers.IO) {
        val p = db.participants().byId(participantId) ?: error("participant missing")
        db.participants().update(
            p.copy(
                ageRange = ageRange, dxDiabetes = dxDiabetes, lastExam = lastExam, hasDoctor = hasDoctor,
                contactEncrypted = contactPhone?.takeIf { it.isNotBlank() }?.let { crypto.encryptString(it) },
                consentShareImages = consentShareImages,
            )
        )
    }

    // ---- capture + inference -------------------------------------------------------------------
    class EyeAnalysis(val capture: Capture, val quality: QualityHeuristic.Result, val inference: Inference?, val output: DrModelRunner.Output?) {
        val eyeResult: EyeResult get() = output?.toEyeResult(capture.gradable) ?: EyeResult(gradable = false)
        val outcome: EyeOutcome get() = OutcomeMapper.eyeOutcome(eyeResult)
    }

    /**
     * Stores the photo encrypted, checks gradability, and (if gradable) runs the model. Works for both the
     * gallery-import path (Phase 1) and the camera path (Phase 2).
     */
    suspend fun analyzeEye(participantId: String, eye: String, bitmap: Bitmap, source: String, retakeN: Int = 0,
                           now: Long = System.currentTimeMillis()): EyeAnalysis = withContext(Dispatchers.IO) {
        val w = bitmap.width; val h = bitmap.height
        val px = IntArray(w * h).also { bitmap.getPixels(it, 0, w, 0, 0, w, h) }
        val quality = QualityHeuristic.assess(px, w, h)
        val capture = Capture(
            participantId = participantId, eye = eye, imagePathEncrypted = null, qualityScore = quality.score,
            gradable = quality.gradable, retakeN = retakeN, deviceModel = Build.MODEL, createdAt = now, source = source,
        )
        val path = images.save(capture.id, bitmap)
        val stored = capture.copy(imagePathEncrypted = path)
        db.captures().upsert(stored)
        if (!quality.gradable) return@withContext EyeAnalysis(stored, quality, null, null)

        val out = runner.run(px, w, h)
        val inf = Inference(
            captureId = stored.id, modelName = runner.card.name, modelVersion = runner.card.version,
            drGrade = out.grade, drProbs = out.probs.joinToString(",", "[", "]"), referableProb = out.referableProb,
            runtimeMs = out.runtimeMs,
        )
        db.inferences().upsert(inf)
        EyeAnalysis(stored, quality, inf, out)
    }

    suspend fun finishResult(participantId: String, right: EyeOutcome, left: EyeOutcome): Referral = withContext(Dispatchers.IO) {
        val overall = OutcomeMapper.overall(right, left)
        val referral = Referral(participantId = participantId, outcome = overall.name.lowercase(), cardCode = newCardCode())
        db.referrals().upsert(referral)
        referral
    }

    suspend fun markCardPrinted(referralId: String, participantId: String) = withContext(Dispatchers.IO) {
        db.referrals().byParticipant(participantId)?.let { db.referrals().update(it.copy(cardPrinted = true)) }
    }

    /** Spec §6 "Delete my record": images, contact and inference results go; an anonymous row stays. */
    suspend fun deleteMyRecord(participantId: String) = withContext(Dispatchers.IO) {
        db.captures().byParticipant(participantId).forEach { images.delete(it.imagePathEncrypted) }
        db.inferences().deleteByParticipant(participantId)
        db.captures().deleteByParticipant(participantId)
        db.referrals().deleteByParticipant(participantId)
        db.participants().anonymize(participantId)
    }

    /** Spec §3.6 retention: images older than 30 days after their event are deleted unless consented to share. */
    suspend fun purgeExpiredImages(now: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        var n = 0
        for (c in db.captures().withImages()) {
            val p = db.participants().byId(c.participantId) ?: continue
            val e = db.events().byId(p.eventId) ?: continue
            if (!p.consentShareImages && now - e.date > ImageStore.RETENTION_MS) {
                images.delete(c.imagePathEncrypted); db.captures().clearImage(c.id); n++
            }
        }
        n
    }

    // ---- exports (spec §3.7) -------------------------------------------------------------------
    suspend fun exportDeid(eventId: String, appVersion: String, now: Long = System.currentTimeMillis()): File = withContext(Dispatchers.IO) {
        val event = db.events().byId(eventId) ?: error("event missing")
        val rows = db.export().deidRows(eventId)
        val model = DeidExport.ModelInfo(runner.card.name, runner.card.version)
        val text = DeidExport.encode(DeidExport.build(event, rows, model, appVersion, now))
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        File(dir, DeidExport.fileName(event)).also { it.writeText(text) }
    }

    /** Consented participants only; images + a result summary; AES-256 zip with the given password. Never auto-uploaded. */
    suspend fun exportClinicZip(eventId: String, password: CharArray): File = withContext(Dispatchers.IO) {
        require(password.size >= 8) { "password too short" }
        val event = db.events().byId(eventId) ?: error("event missing")
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val tmp = File(context.cacheDir, "clinic-${event.id}").apply { deleteRecursively(); mkdirs() }
        val summary = StringBuilder("card_code,eye,gradable,dr_grade,referable_prob,outcome\n")
        for (p in db.participants().consentedByEvent(eventId)) {
            val ref = db.referrals().byParticipant(p.id) ?: continue
            for (c in db.captures().byParticipant(p.id)) {
                val inf = db.inferences().latestForCapture(c.id)
                summary.append("${ref.cardCode},${c.eye},${c.gradable},${inf?.drGrade ?: ""},${inf?.referableProb ?: ""},${ref.outcome}\n")
                c.imagePathEncrypted?.let { images.loadBytes(it) }?.let { File(tmp, "${ref.cardCode}_${c.eye}.png").writeBytes(it) }
            }
        }
        File(tmp, "summary.csv").writeText(summary.toString())
        File(tmp, "README.txt").writeText(
            "Foresight clinic hand-off for event '${event.name}'. Images are only from participants who consented to share.\n" +
            "Foresight is a screening tool operated by students. It is not a medical diagnosis. Please see a healthcare provider.\n"
        )
        val out = File(dir, "foresight_export_${event.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}_clinic.zip")
        out.delete()
        val params = ZipParameters().apply { isEncryptFiles = true; encryptionMethod = EncryptionMethod.AES }
        ZipFile(out, password).use { zip -> tmp.listFiles()?.forEach { zip.addFile(it, params) } }
        tmp.deleteRecursively()
        out
    }

    private fun newCardCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"   // no 0/O/1/I
        val rnd = SecureRandom()
        return (0 until 8).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("").chunked(4).joinToString("-")
    }
}
