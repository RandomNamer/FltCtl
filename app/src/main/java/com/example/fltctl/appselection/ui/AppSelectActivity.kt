package com.example.fltctl.appselection.ui

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.example.fltctl.ui.BaseComposeActivity
import com.example.fltctl.utils.logs

class AppSelectActivity : BaseComposeActivity() {

    private val vm by viewModels<AppSelectPageVM>()
    private val log by logs("selectAct")

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
        log.i(result.toString())
        finish()
    }
}