package org.jameslick.foresight.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jameslick.foresight.BuildConfig
import org.jameslick.foresight.data.Event
import org.jameslick.foresight.data.Referral
import org.jameslick.foresight.data.ScreeningRepository
import org.jameslick.foresight.ml.EyeOutcome
import org.jameslick.foresight.ml.OutcomeMapper
import org.jameslick.foresight.ml.OverallOutcome
import java.io.File

/**
 * State of one screening session (spec §3.1): language -> consent -> intake -> right eye -> left eye ->
 * processing -> results -> card -> done. The ViewModel owns the state; screens are pure functions of it.
 * `nextParticipant()` wipes everything except the event (§3.1 step 9).
 */
class ScreeningViewModel(app: Application) : AndroidViewModel(app) {
    val repo = ScreeningRepository(app)

    data class Intake(
        val ageRange: String? = null,
        val dxDiabetes: String? = null,
        val lastExam: String? = null,
        val hasDoctor: Boolean? = null,
        val contactPhone: String = "",
        val consentShareImages: Boolean = false,
    )

    data class Session(
        val event: Event? = null,
        val participantId: String? = null,
        val participantNumber: Int = 0,
        val language: String = "en",
        val consentGiven: Boolean = false,
        val intake: Intake = Intake(),
        val refusedUnder18: Boolean = false,
        val right: ScreeningRepository.EyeAnalysis? = null,
        val left: ScreeningRepository.EyeAnalysis? = null,
        val rightAttempts: Int = 0,
        val leftAttempts: Int = 0,
        val analyzing: String? = null,            // "R" | "L" while the model runs
        val referral: Referral? = null,
        val overall: OverallOutcome? = null,
        val recordDeleted: Boolean = false,
        val error: String? = null,
        val lastExport: File? = null,
    ) {
        val rightOutcome: EyeOutcome get() = right?.outcome ?: EyeOutcome.Ungradable
        val leftOutcome: EyeOutcome get() = left?.outcome ?: EyeOutcome.Ungradable
        val bothEyesDone: Boolean get() = right != null && left != null
    }

    private val _state = MutableStateFlow(Session())
    val state: StateFlow<Session> = _state

    val events = repo.events()

    fun selectEvent(event: Event) = _state.update { it.copy(event = event) }

    fun createEvent(name: String, location: String) = viewModelScope.launch {
        val e = repo.createEvent(name.trim(), location.trim())
        _state.update { it.copy(event = e) }
    }

    fun setLanguage(tag: String) = _state.update { it.copy(language = tag) }

    /** Called when consent is given: this is the moment a participant record exists. */
    fun giveConsent() = viewModelScope.launch {
        val s = _state.value
        val event = s.event ?: return@launch
        if (s.participantId == null) {
            val p = repo.startParticipant(event.id, s.language)
            _state.update { it.copy(participantId = p.id, consentGiven = true, participantNumber = it.participantNumber + 1) }
        } else {
            _state.update { it.copy(consentGiven = true) }
        }
    }

    fun answerAge(v: String?) = _state.update { it.copy(intake = it.intake.copy(ageRange = v), refusedUnder18 = v == "under18") }
    fun answerDiabetes(v: String?) = _state.update { it.copy(intake = it.intake.copy(dxDiabetes = v)) }
    fun answerLastExam(v: String?) = _state.update { it.copy(intake = it.intake.copy(lastExam = v)) }
    fun answerDoctor(v: Boolean?) = _state.update { it.copy(intake = it.intake.copy(hasDoctor = v)) }
    fun answerContact(v: String) = _state.update { it.copy(intake = it.intake.copy(contactPhone = v)) }
    fun answerShare(v: Boolean) = _state.update { it.copy(intake = it.intake.copy(consentShareImages = v)) }

    fun saveIntake() = viewModelScope.launch {
        val s = _state.value
        val pid = s.participantId ?: return@launch
        val i = s.intake
        repo.saveIntake(pid, i.ageRange, i.dxDiabetes, i.lastExam, i.hasDoctor, i.contactPhone, i.consentShareImages)
    }

    /** Phase 1: the photo comes from the gallery picker (or a test hook). Phase 2 wires the camera here. */
    fun analyzeEye(eye: String, bitmap: Bitmap, source: String = "gallery_import", focusCal: Float? = null) = viewModelScope.launch {
        val s = _state.value
        val pid = s.participantId ?: return@launch
        val attempt = if (eye == "R") s.rightAttempts else s.leftAttempts
        _state.update { it.copy(analyzing = eye, error = null) }
        try {
            val result = repo.analyzeEye(pid, eye, bitmap, source, retakeN = attempt, focusCal = focusCal)
            _state.update {
                if (eye == "R") it.copy(right = result, rightAttempts = attempt + 1, analyzing = null)
                else it.copy(left = result, leftAttempts = attempt + 1, analyzing = null)
            }
        } catch (e: Exception) {
            _state.update { it.copy(analyzing = null, error = e.message ?: e.toString()) }
        }
    }

    /** Spec §3.2: at most 3 attempts per eye, then the eye is recorded as "not clear". */
    fun canRetake(eye: String): Boolean = (if (eye == "R") _state.value.rightAttempts else _state.value.leftAttempts) < MAX_ATTEMPTS

    fun computeResult() = viewModelScope.launch {
        val s = _state.value
        val pid = s.participantId ?: return@launch
        if (s.referral != null) return@launch
        val overall = OutcomeMapper.overall(s.rightOutcome, s.leftOutcome)
        val referral = repo.finishResult(pid, s.rightOutcome, s.leftOutcome)
        _state.update { it.copy(referral = referral, overall = overall) }
    }

    fun markCardPrinted() = viewModelScope.launch {
        val s = _state.value
        val r = s.referral ?: return@launch
        repo.markCardPrinted(r.id, r.participantId)
        _state.update { it.copy(referral = r.copy(cardPrinted = true)) }
    }

    fun deleteMyRecord() = viewModelScope.launch {
        val pid = _state.value.participantId ?: return@launch
        repo.deleteMyRecord(pid)
        _state.update { it.copy(recordDeleted = true, right = null, left = null, referral = null, overall = null) }
    }

    /** Wipes all participant state; keeps the event and the counter (spec §3.1 step 9). */
    fun nextParticipant() = _state.update { Session(event = it.event, participantNumber = it.participantNumber, language = it.language) }

    fun exportDeid() = viewModelScope.launch {
        val event = _state.value.event ?: return@launch
        val f = repo.exportDeid(event.id, BuildConfig.VERSION_NAME)
        _state.update { it.copy(lastExport = f) }
    }

    fun exportClinic(password: String) = viewModelScope.launch {
        val event = _state.value.event ?: return@launch
        try {
            val f = repo.exportClinicZip(event.id, password.toCharArray())
            _state.update { it.copy(lastExport = f, error = null) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    fun purgeExpired() = viewModelScope.launch { repo.purgeExpiredImages() }

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}
