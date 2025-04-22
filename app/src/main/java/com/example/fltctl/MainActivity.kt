package com.example.fltctl

import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.fltctl.ui.BaseComposeActivity
import com.example.fltctl.ui.home.HomeScreen
import com.example.fltctl.ui.home.HomeViewModel
import com.example.fltctl.utils.RuntimePermissionUtil
import com.example.fltctl.utils.hasEnabledAccessibilityService
import com.example.fltctl.utils.hasOverlaysPermission

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



