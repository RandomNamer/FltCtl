package com.example.fltctl

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fltctl.ui.BaseComposeActivity
import com.example.fltctl.ui.home.HomeScreen
import com.example.fltctl.ui.home.HomeViewModel
import com.example.fltctl.ui.theme.FltCtlTheme
import com.example.fltctl.utils.RuntimePermissionUtil
import com.example.fltctl.utils.hasEnabledAccessibilityService
import com.example.fltctl.utils.hasOverlaysPermission
import kotlinx.coroutines.flow.map

class MainActivity : BaseComposeActivity() {

    companion object {
        private const val REQ_CODE_DRAW_OVERLAY = 1919810
        private const val REQ_ACCESSIBILITY = 114514
    }

    private val vm: HomeViewModel by viewModels()

    private var overlayPermissionResultConsumer: ((Boolean) -> Unit)? = null

    private var accessibilityPermissionResultConsumer: ((Boolean) -> Unit)? = null

    @Composable
    override fun Content() {
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
    }

    private fun requestOverlaysPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivityForResult(intent, REQ_CODE_DRAW_OVERLAY)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.request_overlay_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestAccessibilityPermission() {
        startActivityForResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS, ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, REQ_ACCESSIBILITY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        RuntimePermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}



