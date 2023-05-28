package com.example.fltctl.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fltctl.ui.theme.FltCtlTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FltCtlTheme(eInkTheme = false) {
                SettingScreen(
                    onClickBack = {
                        onBackPressed()
                    }
                )
            }
        }
    }
}
