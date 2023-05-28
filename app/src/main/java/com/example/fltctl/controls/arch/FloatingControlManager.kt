package com.example.fltctl.controls.arch

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import com.example.fltctl.*
import com.example.fltctl.controls.DefaultVolumeKeyControl
import com.example.fltctl.controls.TuringPageByVolumeKeyControl
import com.example.fltctl.controls.VerticalTurnPageControl
import com.example.fltctl.ui.home.ControlSelection
import com.example.fltctl.widgets.window.FloatingWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass


data class FloatingControlInfo(
    val displayName: String,
    val desc: String = "",
    val klass: KClass<out FloatingControl>,
    val key: String = klass.simpleName ?: klass.java.name
)


@SuppressLint("StaticFieldLeak")
object FloatingControlManager {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val controls by lazy{ mutableListOf<FloatingControlInfo>(
        FloatingControlInfo(
            displayName = getString(R.string.controlDisplayName_default_volume),
            klass = DefaultVolumeKeyControl::class
        ),
        FloatingControlInfo(
            displayName = getString(R.string.controlDisplayName_volume_turn_page),
            klass = TuringPageByVolumeKeyControl::class
        ),
        FloatingControlInfo(
            displayName = getString(R.string.controlDisplayName_vert_turn_page),
            klass = VerticalTurnPageControl::class
        )
    ) }

    private var currentSelectedControl: FloatingControlInfo? = null

    private var currentControlInstance: FloatingControl? = null

    private var inEInkMode = false
        set(value) {
            field = value
            currentControlInstance?.handler?.post {
                currentControlInstance?.setEInkMode(value)
            }
        }

    private var window: FloatingWindow? = null

    val controlStateFlow: Flow<List<ControlSelection>>
        get() = settingsFlow.map { pref ->
            val currentSelected = pref[SettingKeys.CURRENT_CONTROL]
            getControlsUiList().map {
                currentSelectedControl = controls.find { c -> c.key == currentSelected }
                it.copy(selected = it.key == currentSelected)
            }
        }

    private val settingsFlow = AppMonitor.appContext.settings.data

    private var lastWindowPositionX = 0f
    private var lastWindowPositionY = 0f

    init {
        coroutineScope.launch {
            delay(1000L)
            settingsFlow.collect {
                lastWindowPositionX = it[SettingKeys.LAST_WINDOW_POSITION_X] ?: 0f
                lastWindowPositionY = it[SettingKeys.LAST_WINDOW_POSITION_Y] ?: 0f
                inEInkMode = it[SettingKeys.UI_EINK_MODE] ?: false
            }
        }
    }

    private fun getControlsUiList(): List<ControlSelection> = controls.map { ControlSelection(it.displayName, it.key, false) }

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

    fun tryShowWindowWithCurrentControl(onResult: ((Boolean) -> Unit)? = null) {
        (window ?: FloatingWindow(AppMonitor.appContext).also { window = it }).run {
            val result = show(getCurrentViewOutOfWindow(AppMonitor.appContext), Point(
                recoverXPositionValue(lastWindowPositionX),
                recoverYPositionValue(lastWindowPositionY)
            ))
            onResult?.invoke(result)
        }
    }

    fun getCurrentViewOutOfWindow(context: Context): View? {
        currentSelectedControl ?: return null
        if (window?.isShowing == true && currentControlInstance != null && currentControlInstance!!.lifecycle.currentState >= Lifecycle.State.STARTED) {
            closeWindow()
        } else {
            currentControlInstance?.destroy()
            currentControlInstance = createFloatingControl(currentSelectedControl!!, context)
        }
        return currentControlInstance!!.getView()
    }

    private fun createFloatingControl(control: FloatingControlInfo, context: Context) =
        control.klass.java.newInstance().apply {
            create(context)
            setEInkMode(inEInkMode)
        }

    fun closeWindow(rememberLocation: Boolean = true) {
        window?.hide { windowState ->
            if (rememberLocation) coroutineScope.launch {
                AppMonitor.appContext.settings.edit {
                    it[SettingKeys.LAST_WINDOW_POSITION_X] = windowState.relativePosition.first
                    it[SettingKeys.LAST_WINDOW_POSITION_Y] = windowState.relativePosition.second
                }
            }
        }
    }

    fun getCurrentViewOnly(): View? {
        return if (window?.isShowing == true && currentControlInstance != null && currentControlInstance!!.lifecycle.currentState >= Lifecycle.State.STARTED) {
            currentControlInstance?.getView()
        } else null
    }
}