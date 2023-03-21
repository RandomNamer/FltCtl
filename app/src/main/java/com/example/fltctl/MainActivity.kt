package com.example.fltctl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.fltctl.ui.home.HomeScreen
import com.example.fltctl.ui.home.HomeViewModel
import com.example.fltctl.ui.theme.FltCtlTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val eInkModeState = settings.data.map {
                it[SettingKeys.UI_EINK_MODE]
            }.collectAsStateWithLifecycle(initialValue = false)

            FltCtlTheme(
                isInEInkMode = eInkModeState.value ?: false
            ) {
                HomeScreen(
                    vm = vm
                )
            }
        }
    }
}



