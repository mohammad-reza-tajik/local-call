package com.localcall.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localcall.R
import com.localcall.call.CallState
import com.localcall.call.Notice
import com.localcall.ui.call.CallScreen
import com.localcall.ui.peers.PeersScreen
import com.localcall.ui.theme.LocalCallColors

@Composable
fun LocalCallRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val identity by viewModel.identity.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val muted by viewModel.muted.collectAsStateWithLifecycle()
    val speaker by viewModel.speaker.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* Denied mic is reported by the engine when a call is attempted. */ }

    LaunchedEffect(Unit) {
        val wanted = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(wanted.toTypedArray())
    }

    val currentContext by rememberUpdatedState(context)
    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            snackbar.showSnackbar(currentContext.getString(notice.messageRes()))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalCallColors.Background)
            .safeDrawingPadding(),
    ) {
        Crossfade(targetState = callState is CallState.Idle, label = "screen") { idle ->
            if (idle) {
                PeersScreen(
                    identity = identity,
                    peers = peers,
                    onCall = viewModel::call,
                    onRename = viewModel::rename,
                )
            } else {
                CallScreen(
                    state = callState,
                    muted = muted,
                    speaker = speaker,
                    onMuteChange = viewModel::setMuted,
                    onSpeakerChange = viewModel::setSpeaker,
                    onAccept = viewModel::accept,
                    onHangup = viewModel::hangup,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

private fun Notice.messageRes(): Int = when (this) {
    Notice.Rejected -> R.string.notice_rejected
    Notice.NoAnswer -> R.string.notice_no_answer
    Notice.Missed -> R.string.notice_missed
    Notice.RemoteEnded -> R.string.notice_remote_ended
    Notice.ConnectionLost -> R.string.notice_connection_lost
    Notice.MicError -> R.string.notice_permission_denied
    Notice.NetworkError -> R.string.notice_network_error
}
