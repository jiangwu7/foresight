package org.jameslick.foresight.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jameslick.foresight.R
import org.jameslick.foresight.i18n.Languages
import org.jameslick.foresight.ml.OverallOutcome
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ReferralCardPdf
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.GlowRing
import org.jameslick.foresight.ui.theme.RingState
import org.jameslick.foresight.ui.theme.Shapes
import java.text.DateFormat
import java.util.Date

/** Mockup 509: a preview of the printed card, "Share as PDF", "Print card", "Done, next participant". */
@Composable
fun CardScreen(vm: ScreeningViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val overall = state.overall ?: OverallOutcome.Ungradable
    val referral = state.referral
    val text = outcomeText(overall)
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    val dateText = remember(state.language) { DateFormat.getDateInstance(DateFormat.MEDIUM, Languages.locale(state.language)).format(Date()) }
    val content = ReferralCardPdf.Content(
        cardCode = referral?.cardCode ?: "----",
        dateText = dateText,
        outcomeLine = stringResource(text.title),
        nextStepLine = stringResource(text.card),
        clinicName = stringResource(R.string.clinic_placeholder_name),
        clinicPhone = stringResource(R.string.clinic_placeholder_phone),
        disclaimer = stringResource(R.string.medical_disclaimer),
        codeLabel = stringResource(R.string.card_code),
        dateLabel = stringResource(R.string.card_date),
    )
    val jobName = stringResource(R.string.card_pdf_name)
    val qr = remember(content.cardCode) { ReferralCardPdf.qrBitmap(content.cardCode, 256) }

    FlowScreen(
        headline = stringResource(R.string.card_title),
        primaryText = stringResource(R.string.action_print_card),
        onPrimary = {
            val pdf = ReferralCardPdf.create(context, content)
            ReferralCardPdf.print(context, pdf, jobName)
            vm.markCardPrinted()
        },
        secondary = {
            BigAction(stringResource(R.string.action_share_pdf), secondary = true, onClick = {
                val pdf = ReferralCardPdf.create(context, content)
                val uri = ReferralCardPdf.shareUri(context, pdf)
                val send = Intent(Intent.ACTION_SEND).apply { this.type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                context.startActivity(Intent.createChooser(send, jobName))
                vm.markCardPrinted()
            })
            Spacer(Modifier.height(Dimens.Space1))
            Text(stringResource(R.string.action_next_participant), style = type.action, color = colors.onBackgroundMuted,
                 modifier = Modifier.fillMaxWidth().clickable(onClick = onDone).padding(Dimens.Space1))
        },
    ) {
        // Preview of the paper card (the PDF is the same content).
        Column(Modifier.fillMaxWidth().clip(Shapes.Medium).background(colors.paper).padding(Dimens.Space3)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.foresight_mark_ink), contentDescription = null, modifier = Modifier.size(Dimens.Space6))
                Spacer(Modifier.width(Dimens.Space1))
                Text(stringResource(R.string.app_name).uppercase(), style = type.microLabel, color = colors.onPaper)
            }
            Spacer(Modifier.height(Dimens.Space2))
            Row {
                Column(Modifier.weight(1f)) {
                    Text(content.outcomeLine, style = type.title, color = colors.onPaper)
                    Spacer(Modifier.height(Dimens.Space1))
                    Text(content.nextStepLine, style = type.body, color = colors.onPaper)
                }
                Spacer(Modifier.width(Dimens.Space2))
                Image(qr.asImageBitmap(), contentDescription = content.cardCode, modifier = Modifier.size(Dimens.RingSizeSmall))
            }
            Spacer(Modifier.height(Dimens.Space2))
            Text("${content.codeLabel.uppercase()}  ${content.cardCode}   ${content.dateLabel.uppercase()}  ${content.dateText}", style = type.numeralSmall, color = colors.onPaper)
            Spacer(Modifier.height(Dimens.Space2))
            Text(content.disclaimer, style = type.microLabel, color = colors.onPaper)
        }
    }
}

/** Mockup 608: ring with a check, "All done", next participant. */
@Composable
fun DoneScreen(vm: ScreeningViewModel, onNext: () -> Unit) {
    val state by vm.state.collectAsState()
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    FlowScreen(
        micro = stringResource(R.string.done_participant, state.participantNumber),
        headline = stringResource(R.string.done_title),
        primaryText = stringResource(R.string.action_next_participant),
        onPrimary = onNext,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlowRing(RingState.Locked, contentDescription = stringResource(R.string.done_title))
        }
        Spacer(Modifier.height(Dimens.Space3))
        Text(stringResource(if (state.recordDeleted) R.string.delete_done else R.string.done_body), style = type.bodyLarge, color = colors.onBackground)
    }
}
