package com.example.fltctl.ui.settings

import androidx.compose.runtime.Composable
import com.example.fltctl.ui.BaseComposeActivity

class SettingsActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        SettingScreen(
            onClickBack = {
                onBackPressed()
            }
        )
    }
}
