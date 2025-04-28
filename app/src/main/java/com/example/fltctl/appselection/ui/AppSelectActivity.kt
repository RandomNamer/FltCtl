package com.example.fltctl.appselection.ui

import android.content.Intent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.example.fltctl.ui.BaseComposeActivity
import com.example.fltctl.utils.logs

class AppSelectActivity : BaseComposeActivity() {

    @Stable
    data class PickOptions(
        val maxCount: Int = 1,
        val enableClickDetails: Boolean = true,
        val enableActivityListInspection: Boolean = false
    )

    companion object {
        const val EXTRA_PICK_SINGLE = "pick_single"
        const val EXTRA_PICK_MULTIPLE = "pick_multiple"
        const val MAX_COUNT = "max_count"
        const val ENABLE_CLICK_DETAILS = "enable_click_details"

        fun validateExternalIntent(intent: Intent): PickOptions? {
            if (!intent.hasExtra(EXTRA_PICK_SINGLE) && !intent.hasExtra(EXTRA_PICK_MULTIPLE)) return null
            var maxCount = intent.getIntExtra(MAX_COUNT, 15)
            if (intent.getBooleanExtra(EXTRA_PICK_SINGLE, false)) maxCount = 1
            val enableClickDetails = intent.getBooleanExtra(ENABLE_CLICK_DETAILS, true)
            return PickOptions(maxCount, enableClickDetails)
        }
    }

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