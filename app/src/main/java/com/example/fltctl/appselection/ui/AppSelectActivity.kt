package com.example.fltctl.appselection.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fltctl.R
import com.example.fltctl.SettingKeys
import com.example.fltctl.settings
import com.example.fltctl.ui.BaseComposeActivity
import com.example.fltctl.ui.theme.FltCtlTheme
import kotlinx.coroutines.flow.map

class AppSelectActivity : BaseComposeActivity() {

    private val vm by viewModels<AppSelectPageVM>()

    @Composable
    override fun Content() {
        AppSelectScreen(
            vm = vm,
            onSelectEnd = this::doneSelecting,
            onBackPressed = {
                onBackPressed()
            }
        )
    }

    private fun doneSelecting() {
        val result = vm.uiState.value.appList.filter { it.selected }.map { it.value.displayName }
        Log.e("selectAct", result.toString())
        finish()
    }
}