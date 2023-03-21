package com.example.fltctl.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fltctl.R
import com.example.fltctl.SettingKeys
import com.example.fltctl.settings
import com.example.fltctl.widgets.composable.BackdropScaffold
import com.example.fltctl.widgets.composable.borderBottom
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    vm: HomeViewModel
) {
    val state by vm.getUiState(LocalContext.current).collectAsStateWithLifecycle()
   BackdropScaffold(
       topBar = {
           HomeTopBar(
               isInEInkMode = state.eInkModeEnabled,
               backgroundColor = backgroundColor
           )
       },
       backLayerColor = backgroundColor,
       cornerRadius = 12.dp
   ) { pv ->
       Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(pv),
           verticalArrangement = Arrangement.Top,
           horizontalAlignment = Alignment.CenterHorizontally
       ) {
           SwitchCard(
               isInEInkMode = state.eInkModeEnabled,
               switched = state.enabled,
               checked = state.isShowing,
               onSwitched = vm::toggleEnable,
               onChecked = vm::toggleShowWindow
           )
       }
   }
}


@Composable
fun SwitchCard(
    isInEInkMode: Boolean,
    switched: Boolean,
    checked: Boolean,
    cornerRadius: Dp = 12.dp,
    onSwitched: (Boolean) -> Unit,
    onChecked: (Boolean) -> Unit
) {
    EInkCompatCard(
        isInEInkMode = isInEInkMode,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.floating_wnd_switch),
                fontWeight = FontWeight(700),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            Switch(checked = switched, onCheckedChange = onSwitched)
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(
                text = stringResource(id = R.string.show_wnd_in_app),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight(700),
                modifier = Modifier.padding(start = 8.dp),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Preview
@Composable
fun SwitchCardPreview() {
    Surface(
        Modifier
            .size(300.dp, 400.dp)
            .background(Color.Yellow)) {
        SwitchCard(isInEInkMode = true, switched = true, checked = true, onSwitched = {}, onChecked = {})
    }
}

@Composable
fun EInkCompatCard(
    isInEInkMode: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.onBackground
    if (isInEInkMode) modifier.background(Color.White)
    Card(
        modifier = modifier,
        shape,
        if (!isInEInkMode) colors else CardDefaults.outlinedCardColors( containerColor = Color.White ),
        elevation,
        border = if (isInEInkMode) BorderStroke(2.dp, outlineColor) else border,
        content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isInEInkMode: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.onBackground,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val ctx = LocalContext.current
    TopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor
        ),
        modifier = if (isInEInkMode) Modifier.borderBottom(1.dp, borderColor) else Modifier,
        title = {
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
                fontWeight = FontWeight(700),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            TopBarMenu(isInEInkMode = isInEInkMode) {
                Toast.makeText(ctx, "Floating Control", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
fun TopBarMenu(isInEInkMode: Boolean, onClickAbout: () -> Unit) {
    var expended by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    IconButton(onClick = { expended = true }) {
        Icon(Icons.Filled.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = expended, onDismissRequest = { expended = false }) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(id = R.string.eink_mode),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            leadingIcon = {
                if(isInEInkMode) Icon(Icons.Outlined.Check, null)
            },
            onClick = {
                scope.launch {
                    ctx.settings.edit {
                        it[SettingKeys.UI_EINK_MODE] = isInEInkMode.not()
                    }
                }
            }
        )
        DropdownMenuItem(text = {
            Text(
                stringResource(id = R.string.about),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } , onClick = {
            onClickAbout.invoke()
        }, leadingIcon = {})
    }
}

@Preview
@Composable
fun HomeTopBarPreview() {
    HomeTopBar(true, Color.Black)
}