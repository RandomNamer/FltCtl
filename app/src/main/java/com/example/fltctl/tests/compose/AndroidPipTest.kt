package com.example.fltctl.tests.compose

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fltctl.R
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.widgets.composable.EInkCompatCard

/**
 * Created by zeyu.zyzhang on 4/27/25
 * @author zeyu.zyzhang@bytedance.com
 */

val androidPipTest = UiTest.ComposeUiTest(
    title = "PiP Test",
    content =  {
        FltCtlTheme {
            Surface {
                AndroidPipScreen()
            }
        }
    },
    fullscreen = false
)

private const val ACTION_INC = "PIP_ACTION_INCREMENT"
private const val ACTION_DEC = "PIP_ACTION_DECREMENT"
private const val ACTION_RESET = "PIP_ACTION_RESET"

@Composable
fun AndroidPipScreen() {
    val isInPip = LocalActivity.current?.isInPictureInPictureMode == true
    val count = remember { mutableIntStateOf(0) }
    val logs = remember { mutableStateOf<List<String>>(emptyList()) }

    if (isInPip) PipUi(count, logs) else NormalUi(count, logs.value)
}

@Composable
private fun NormalUi(count: MutableIntState, logs: List<String>) {
    val hss = rememberScrollState()
    val activity = LocalActivity.current
    Column(Modifier.padding(12.dp)) {
        CommonCounterUi(count.intValue)
        Spacer(Modifier.size(16.dp))
        Column(Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(8.dp)) {
            Text(text="Logs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(Modifier.horizontalScroll(hss)) {
                items(items = logs, key = { it }) {
                    Text(it)
                }
            }
        }
        Row(Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(modifier = Modifier.weight(1f), onClick = {
                count.intValue = 0
            }) {
                Text("Refresh")
            }
            Spacer(Modifier.width(12.dp))
            Button(modifier = Modifier.weight(1f), onClick = {
                activity?.run {
                    if (Build.VERSION.SDK_INT >= 26) {
                        enterPictureInPictureMode(buildPipParams())
                    } else {
                        enterPictureInPictureMode()
                    }
                    actionBar?.hide()
                }
            }) {
                Text("Launch PiP")
            }
        }
    }
}

@RequiresApi(26)
private fun Activity.buildPipParams(): PictureInPictureParams {
    val customActions = listOf(
        RemoteAction(
            Icon.createWithResource(this, R.drawable.baseline_refresh_24),
            "Reset",
            "Reset counter",
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_RESET).apply {
                    setPackage(packageName)
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        ),
        RemoteAction(
            Icon.createWithResource(this, R.drawable.baseline_remove_24),
            "Decrease",
            "Decrease counter",
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_DEC).apply {
                    setPackage(packageName)
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        ),
        RemoteAction(
            Icon.createWithResource(this, R.drawable.baseline_add_24),
            "Increment",
            "Increment counter",
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_INC).apply {
                    setPackage(packageName)
                },
                PendingIntent.FLAG_IMMUTABLE
            )
        )

    )
    return PictureInPictureParams.Builder()
        .setActions(customActions)
        .setAspectRatio(Rational(16, 9))
        .apply { if (Build.VERSION.SDK_INT > 31) setSeamlessResizeEnabled(true) }
        .build()
}

@Composable
private fun PipUi(count: MutableIntState, logs: MutableState<List<String>>) {

    val ctx = LocalContext.current

    val activity = LocalActivity.current

    DisposableEffect(Unit) {
        val combinedReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                logs.value = listOf("${System.currentTimeMillis()}: Broadcast rcv: ${intent?.action}") + logs.value
                when(intent?.action) {
                    ACTION_INC -> {
                        count.intValue++
                    }
                    ACTION_DEC -> {
                        count.intValue--
                    }
                    ACTION_RESET -> {
                        count.intValue = 0
                    }
                    else -> {}
                }
            }
        }
        ContextCompat.registerReceiver(ctx, combinedReceiver, IntentFilter().apply {
            addAction(ACTION_INC)
            addAction(ACTION_DEC)
            addAction(ACTION_RESET)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)// ignore_security_alert_wait_for_fix [ByDesign2.5]AddPermissionForDynamicReceiver

        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        onDispose {
            ctx.unregisterReceiver(combinedReceiver)
            (activity as? AppCompatActivity)?.supportActionBar?.show()
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
        CommonCounterUi(count.intValue)
    }
}

@Composable
private fun CommonCounterUi(count: Int) {
    EInkCompatCard(tonalElevation = 8.dp) { pv ->
        Spacer(Modifier.size(pv.calculateTopPadding()))
        Text(text = "COUNTER", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(12.dp))
        Text(text = "${count}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(pv.calculateBottomPadding()))
    }
}