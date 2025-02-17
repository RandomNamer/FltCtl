package com.example.fltctl.appselection.ui

import android.util.Log
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.example.fltctl.ui.BaseComposeActivity

class AppSelectActivity : BaseComposeActivity() {

    private val vm by viewModels<AppSelectPageVM>()

    @Composable
    override fun Content() {
        AppSelectScreen(
            vm = vm,
            onSelectEnd = this::doneSelecting,
            onBackPressedDelegate = {
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