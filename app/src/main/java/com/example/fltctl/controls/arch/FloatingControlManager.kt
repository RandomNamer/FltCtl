package com.example.fltctl.controls.arch

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import com.example.fltctl.AppMonitor
import com.example.fltctl.R
import com.example.fltctl.SettingKeys
import com.example.fltctl.controls.DefaultVolumeKeyControl
import com.example.fltctl.controls.TuringPageByVolumeKeyControl
import com.example.fltctl.settings
import com.example.fltctl.ui.home.ControlSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.reflect.KClass


data class FloatingControlInfo(
    val displayName: String,
    val desc: String = "",
    val clazz: KClass<out FloatingControl>,
    val key: String = clazz.simpleName ?: clazz.java.name
)


object FloatingControlManager {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val controls by lazy{ mutableListOf<FloatingControlInfo>(
        FloatingControlInfo(
            displayName = getString(R.string.controlDisplayName_default_volume),
            clazz = DefaultVolumeKeyControl::class
        ),
        FloatingControlInfo(
            displayName = getString(R.string.controlDisplayName_volume_turn_page),
            clazz = TuringPageByVolumeKeyControl::class
        )
    ) }

    private var currentSelectedControl: FloatingControlInfo? = null

    val controlStateFlow: Flow<List<ControlSelection>>
        get() = AppMonitor.appContext.settings.data.map { pref ->
            val currentSelected = pref[SettingKeys.CURRENT_CONTROL]
            getControlsUiList().map {
                it.copy(selected = it.key == currentSelected)
            }
        }

    fun getControlsUiList(): List<ControlSelection> = controls.map { ControlSelection(it.displayName, it.key, false) }

    @JvmStatic
    private fun getString(@StringRes id: Int) = AppMonitor.appContext.getString(id)

    fun openConfigureControlPage(key: String, context: Context) {

    }

    fun selectControl(key: String) {
        val target = controls.find { it.key == key }
        target?.run {
            coroutineScope.launch {
                AppMonitor.appContext.settings.edit {
                    it[SettingKeys.CURRENT_CONTROL] = key
                }
            }
        }
    }

    fun tryShowWindow(onResult: ((Boolean) -> Unit)? = null) {

    }

    fun realShowWindow() {

    }

    fun setWindowVisibility(visible: Boolean) {

    }

    fun closeWindow() {

    }
}