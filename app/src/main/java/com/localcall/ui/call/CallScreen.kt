package com.localcall.ui.call

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localcall.R
import com.localcall.call.CallState
import com.localcall.call.peerOrNull
import com.localcall.ui.components.Avatar
import com.localcall.ui.components.RoundButton
import com.localcall.ui.theme.LocalCallColors
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    state: CallState,
    muted: Boolean,
    speaker: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onSpeakerChange: (Boolean) -> Unit,
    onAccept: () -> Unit,
    onHangup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val peer = state.peerOrNull ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Avatar(peer.name, size = 132.dp, textSize = 56.sp)
        Spacer(Modifier.height(28.dp))
        Text(
            text = peer.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        StatusLine(state)

        Spacer(Modifier.weight(1f))

        when (state) {
            is CallState.Incoming -> IncomingControls(onAccept, onHangup)
            is CallState.Outgoing -> OutgoingControls(onHangup)
            is CallState.Active -> ActiveControls(
                muted = muted,
                speaker = speaker,
                onMuteChange = onMuteChange,
                onSpeakerChange = onSpeakerChange,
                onHangup = onHangup,
            )
            CallState.Idle -> Unit
        }
    }
}

@Composable
private fun StatusLine(state: CallState) {
    val text = when (state) {
        is CallState.Incoming -> stringResource(R.string.call_incoming)
        is CallState.Outgoing -> stringResource(R.string.call_calling)
        is CallState.Active -> elapsedText(state.startedAt)
        CallState.Idle -> ""
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (state is CallState.Active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/** mm:ss since [startedAt], refreshed every second. */
@Composable
private fun elapsedText(startedAt: Long): String {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val seconds = ((now - startedAt) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun IncomingControls(onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        RoundButton(
            icon = R.drawable.ic_call_end,
            label = stringResource(R.string.call_decline),
            background = LocalCallColors.Danger,
            iconTint = androidx.compose.ui.graphics.Color.White,
            onClick = onDecline,
            size = 76.dp,
        )
        RoundButton(
            icon = R.drawable.ic_call,
            label = stringResource(R.string.call_accept),
            background = LocalCallColors.Accent,
            iconTint = LocalCallColors.OnAccent,
            onClick = onAccept,
            size = 76.dp,
        )
    }
}

@Composable
private fun OutgoingControls(onHangup: () -> Unit) {
    RoundButton(
        icon = R.drawable.ic_call_end,
        label = stringResource(R.string.call_end),
        background = LocalCallColors.Danger,
        iconTint = androidx.compose.ui.graphics.Color.White,
        onClick = onHangup,
        size = 76.dp,
    )
}

@Composable
private fun ActiveControls(
    muted: Boolean,
    speaker: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onSpeakerChange: (Boolean) -> Unit,
    onHangup: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ToggleButton(
                on = muted,
                iconOn = R.drawable.ic_mic_off,
                iconOff = R.drawable.ic_mic,
                labelOn = stringResource(R.string.call_unmute),
                labelOff = stringResource(R.string.call_mute),
                onToggle = { onMuteChange(!muted) },
            )
            ToggleButton(
                on = speaker,
                iconOn = R.drawable.ic_volume_up,
                iconOff = R.drawable.ic_volume_up,
                labelOn = stringResource(R.string.call_speaker),
                labelOff = stringResource(R.string.call_speaker),
                onToggle = { onSpeakerChange(!speaker) },
            )
        }
        Spacer(Modifier.height(36.dp))
        RoundButton(
            icon = R.drawable.ic_call_end,
            label = stringResource(R.string.call_end),
            background = LocalCallColors.Danger,
            iconTint = androidx.compose.ui.graphics.Color.White,
            onClick = onHangup,
            size = 76.dp,
        )
    }
}

@Composable
private fun ToggleButton(
    on: Boolean,
    iconOn: Int,
    iconOff: Int,
    labelOn: String,
    labelOff: String,
    onToggle: () -> Unit,
) {
    RoundButton(
        icon = if (on) iconOn else iconOff,
        label = if (on) labelOn else labelOff,
        background = if (on) LocalCallColors.TextPrimary else LocalCallColors.SurfaceHigh,
        iconTint = if (on) LocalCallColors.Background else LocalCallColors.TextPrimary,
        onClick = onToggle,
    )
}
