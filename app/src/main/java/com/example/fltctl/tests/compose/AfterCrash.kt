package com.example.fltctl.tests.compose

import android.os.Build
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.fltctl.AppMonitor
import com.example.fltctl.FloatingControlApp
import com.example.fltctl.tests.UiTest
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.asLocaleTimeString
import com.example.fltctl.widgets.composable.TwoLineTopAppBar
import com.example.fltctl.widgets.composable.simplyScrollable
import kotlinx.parcelize.Parcelize

/**
 * Created by zeyu.zyzhang on 4/21/25
 * @author zeyu.zyzhang@bytedance.com
 */

private val afterCrash = UiTest.ComposeUiTest(
    title = "After Crash",
    description = "Test after crash",
    content = { FltCtlTheme { AfterCrashScreen() } },
    fullscreen = true
)

class AfterCrash: UiTest.ComposeUiTestWrap() {
    override val data: UiTest.ComposeUiTest
        get() = afterCrash

}

@Parcelize
data class CrashInfo(
    val time: Long,
    val title: String = "Crash",
    val stacktraceString: String,
    val message: String,
) : Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterCrashScreen() {

    val crashInfo = LocalActivity.current?.intent?.getParcelableExtra<CrashInfo>(FloatingControlApp.CRASH_RECOVER)
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val context = LocalContext.current
    val scrollBarBehaviour = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val backPressInterceptor = remember {
        object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                return
            }
        }
    }

    DisposableEffect (Unit) {
        onBackPressedDispatcher?.addCallback(backPressInterceptor)
        onDispose {
            backPressInterceptor.remove()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBarBehaviour.nestedScrollConnection),
        topBar = {
            TwoLineTopAppBar(
                title = {
                    Text("Crashed!")
                },
                subTitle = {
                    Text(crashInfo?.title ?: "Crash info not found")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                scrollBehavior = scrollBarBehaviour
            )
      },
    )  { pv ->
        Surface(Modifier
            .fillMaxSize()
            .padding(pv), tonalElevation = 4.dp) {
            Column(Modifier
                .fillMaxSize()
                .padding(12.dp)) {

                Text(text="Crashed at ${crashInfo?.time?.asLocaleTimeString()}: ${crashInfo?.message}", style = MaterialTheme.typography.labelMedium)
                Text(text="Device: ${Build.MODEL} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.labelMedium)
                Text(text="Stacktrace:", style = MaterialTheme.typography.titleMedium)
                Box(Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(8.dp)) {
                    Text(
                        text = crashInfo?.stacktraceString?: "",
                        modifier = Modifier.simplyScrollable()
                    )
                }
                Row(Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    val clipboardManager = LocalClipboardManager.current
                    Button(modifier = Modifier.weight(1f), enabled = crashInfo != null, onClick = {
                        crashInfo?.run {
                            clipboardManager.setText(AnnotatedString("App crashed at ${time.asLocaleTimeString()}: $title\nStacktrace:\n$stacktraceString"))
                        }
                    }) {
                        Text("Copy")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(modifier = Modifier.weight(1f), onClick = {
                        AppMonitor.restartApplication()
                    }) {
                        Text("Restart")
                    }
                }
            }
        }
    }
}
