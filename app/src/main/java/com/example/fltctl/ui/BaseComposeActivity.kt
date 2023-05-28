package com.example.fltctl.ui

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fltctl.SettingKeys
import com.example.fltctl.settings
import com.example.fltctl.ui.theme.FltCtlTheme
import kotlinx.coroutines.flow.map

abstract class BaseComposeActivity: ComponentActivity()  {

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val eInkSettings = settings.data.map {
            it[SettingKeys.UI_EINK_MODE] ?: false
        }
        setContent {
            val eInkState = eInkSettings.collectAsStateWithLifecycle(initialValue = false)
            FltCtlTheme(eInkTheme = eInkState.value) {
                Content()
            }
        }
    }

    @Composable
    abstract fun Content()
}