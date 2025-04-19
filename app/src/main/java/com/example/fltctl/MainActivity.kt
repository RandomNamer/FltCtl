package com.example.fltctl

import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.fltctl.ui.BaseComposeActivity
import com.example.fltctl.ui.home.HomeScreen
import com.example.fltctl.ui.home.HomeViewModel
import com.example.fltctl.utils.RuntimePermissionUtil
import com.example.fltctl.utils.hasEnabledAccessibilityService
import com.example.fltctl.utils.hasOverlaysPermission
import com.example.fltctl.widgets.composable.simplyScrollable

class MainActivity : BaseComposeActivity() {

    companion object {
        private const val REQ_CODE_DRAW_OVERLAY = 1919810
        private const val REQ_ACCESSIBILITY = 114514
    }

    private val vm: HomeViewModel by viewModels()

    private var overlayPermissionResultConsumer: ((Boolean) -> Unit)? = null

    private var accessibilityPermissionResultConsumer: ((Boolean) -> Unit)? = null

    private var waitingForActivityResult = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val crashMessage = remember { intent.getStringExtra(FloatingControlApp.CRASH_STACKTRACE_STR) }
        var shouldShowCrashDialog by remember { mutableStateOf(intent.getBooleanExtra(FloatingControlApp.CRASH_RECOVER, false)) }

        HomeScreen(
            vm = vm,
            requestOverlaysPermission = {
                overlayPermissionResultConsumer = it
                requestOverlaysPermission()
            },
            requestAccessibilityPermission = {
                accessibilityPermissionResultConsumer = it
                requestAccessibilityPermission()
            }
        )

        if (shouldShowCrashDialog && crashMessage != null) {
            AlertDialog(
                modifier = Modifier.fillMaxHeight(0.7f),
                onDismissRequest = { shouldShowCrashDialog = false },
                title = {
                   Text("Crashed!")
                },
                text = {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.simplyScrollable()) {
                            Text(crashMessage, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(onClick = {
                        shouldShowCrashDialog = false
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    private fun requestOverlaysPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                waitingForActivityResult = true
                startActivityForResult(intent, REQ_CODE_DRAW_OVERLAY)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.request_overlay_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestAccessibilityPermission() {
        waitingForActivityResult = true
        startActivityForResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS, ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, REQ_ACCESSIBILITY)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        onActivityResult(requestCode, resultCode, data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        waitingForActivityResult = false
        when (requestCode) {
            REQ_CODE_DRAW_OVERLAY -> {
                overlayPermissionResultConsumer?.invoke(hasOverlaysPermission(this))
                overlayPermissionResultConsumer = null
            }
            REQ_ACCESSIBILITY -> {
                accessibilityPermissionResultConsumer?.invoke(hasEnabledAccessibilityService(this))
                accessibilityPermissionResultConsumer = null
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume()
        if(!waitingForActivityResult) vm.onResumeCheckersComplete(
            hasOverlaysPermission(this),
            hasEnabledAccessibilityService(this)
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        RuntimePermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}



