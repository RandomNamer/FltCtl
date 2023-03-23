package com.example.fltctl.ui.home

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fltctl.R
import com.example.fltctl.SettingKeys
import com.example.fltctl.settings
import com.example.fltctl.ui.ColorPaletteActivity
import com.example.fltctl.ui.takeProportion
import com.example.fltctl.widgets.composable.*
import kotlinx.coroutines.launch
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val backgroundColor: Color = MaterialTheme.colorScheme.primary
    val cardColor = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
   BackdropScaffold(
       modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
       topBar = {
           HomeTopBar(
               scrollBehavior,
               isInEInkMode = state.eInkModeEnabled,
               backgroundColor = backgroundColor,
               contentColor = MaterialTheme.colorScheme.onPrimary
           )
       },
       frontLayerColor = MaterialTheme.colorScheme.surface,
       backLayerColor = backgroundColor,
       cornerRadius = 16.dp,
       backLayerPaddingTop = 2.dp,
       backLayerPaddingHorizontal = 2.dp
   ) { pv ->
       Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(top = 12.dp, start = 12.dp, end = 12.dp)
               .verticalScroll(rememberScrollState()),
           verticalArrangement = Arrangement.Top,
           horizontalAlignment = Alignment.CenterHorizontally,
       ) {
           SwitchCard(
               isInEInkMode = state.eInkModeEnabled,
               color = cardColor,
               switched = state.enabled,
               checked = state.isShowing,
               onSwitched = vm::toggleEnable,
               onChecked = vm::toggleShowWindow
           )
           Spacer(Modifier.height(16.dp))
           ControlSelectionCard(
               isInEInkMode = state.eInkModeEnabled,
               color = cardColor,
               controlSelectionState = state.controlSelectionList,
               onSelect = vm::onSelectControl,
               onConfigure = vm::onClickConfigureControl
           )
           Spacer(Modifier.height(16.dp))
           SettingsCard(isInEInkMode = state.eInkModeEnabled, color = cardColor) {

           }
       }
   }
}

@Composable
fun SettingsCard(
    isInEInkMode: Boolean,
    color: CardColors,
    onClick: () -> Unit
) {
    EInkCompatCard(isInEInkMode = isInEInkMode, colors = color) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick.invoke()
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.settings_card_title),
                style = MaterialTheme.typography.titleMedium
            )
            Icon(Icons.Outlined.ArrowForwardIos, null)
        }
    }
}

@Composable
fun ControlSelectionCard(
    isInEInkMode: Boolean,
    color: CardColors,
    controlSelectionState: List<ControlSelection>,
    onSelect: (String) -> Unit,
    onConfigure: (String, Context) -> Unit
) {
    val currentSelected = controlSelectionState.find { it.selected }
    EInkCompatExpandableCard(
        isInEInkMode = isInEInkMode,
        colors = color,
        switchExpandViewModifier = { padding(end = 12.dp) },
        beforeExpandPrompt = stringResource(id = R.string.control_card_expand),
        afterExpandPrompt = stringResource(id = R.string.control_card_collapse),
        contentAboveExpandable = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(id = R.string.control_card_title).uppercase(),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight(700),
                    letterSpacing = 1.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = "${stringResource(id = R.string.control_card_currently_selected)} ${currentSelected?.displayTitle ?: "None"}",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
            }
        },
        contentBelowExpandable = {
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = currentSelected != null, onClick = {
                        onConfigure.invoke(currentSelected!!.key, context)
                    })
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.control_card_configure),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(Icons.Outlined.NavigateNext, null, modifier = Modifier.size(28.dp))
            }
        }
    ) {
        ControlSelectionList(list = controlSelectionState, onSelect = onSelect)
    }
}

@Composable
@Preview
fun ControlSelectionCardPreview() {
    ControlSelectionCard(
        isInEInkMode = false,
        color = CardDefaults.elevatedCardColors(),
        controlSelectionState = listOf(
            ControlSelection(
                displayTitle = "control A",
                key = "114514",
                selected = false
            ),
            ControlSelection(
                displayTitle = "control B",
                key = "1919810",
                selected = true
            ),
            ControlSelection(
                displayTitle = "control C",
                key = "1145141919810",
                selected = false
            )
        ),
        onSelect = {}, onConfigure = { _, _ -> })
}

@Composable
fun ControlSelectionList(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(horizontal = 8.dp),
    list: List<ControlSelection>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = modifier
    ) {
        list.forEachIndexed { index, controlSelection ->
            ControlSelectionView(
                modifier = if (index > 0)
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .borderTop(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            padding = 24.dp
                        )
                else Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                state = controlSelection, onSelect = onSelect
            )
        }
    }
}

@Composable
fun ControlSelectionView(
    modifier: Modifier = Modifier,
    state: ControlSelection,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ){
        RadioButton(selected = state.selected, onClick = { onSelect.invoke(state.key) })
        Text(text = state.displayTitle, style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
fun SwitchCard(
    isInEInkMode: Boolean,
    switched: Boolean,
    checked: Boolean,
    color: CardColors,
    onSwitched: (Boolean) -> Unit,
    onChecked: (Boolean) -> Unit
) {
    EInkCompatCard(
        isInEInkMode = isInEInkMode,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = color,
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.floating_wnd_switch),
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
            Switch(checked = switched, onCheckedChange = onSwitched, colors = SwitchDefaults.colors(uncheckedTrackColor = Color.White))
        }
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(
                text = stringResource(id = R.string.show_wnd_in_app),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Preview
@Composable
fun SwitchCardPreview() {
    Surface(
        Modifier
            .size(300.dp, 400.dp)
            .background(Color.Yellow)) {
        SwitchCard(isInEInkMode = true, switched = true, checked = true, onSwitched = {}, onChecked = {}, color = CardDefaults.elevatedCardColors())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    isInEInkMode: Boolean = false,
    borderColor: Color = MaterialTheme.colorScheme.onBackground,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val ctx = LocalContext.current
    LargeTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
        modifier = if (isInEInkMode) Modifier.borderBottom(1.dp, borderColor) else Modifier,
        title = {
            val fraction = scrollBehavior.state.collapsedFraction
            Log.e("comp", "cFraction: $fraction")
            Text(
                text = stringResource(id = R.string.app_name).uppercase(),
                fontSize = IntRange(32, 20).takeProportion(fraction).sp,
                fontWeight = FontWeight(IntRange(300, 500).takeProportion(fraction).toInt()),
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            TopBarMenu(isInEInkMode = isInEInkMode) {
                Toast.makeText(ctx, "Floating Control", Toast.LENGTH_SHORT).show()
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
fun TopBarMenu(isInEInkMode: Boolean, onClickAbout: () -> Unit) {
    var expended by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    IconButton(onClick = { expended = true }) {
        Icon(Icons.Filled.MoreVert, null)
    }
    DropdownMenu(expanded = expended, onDismissRequest = { expended = false }) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(id = R.string.eink_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            leadingIcon = {
                if(isInEInkMode) Icon(Icons.Outlined.Check, null)
            }, trailingIcon = {},
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        } , onClick = {
            onClickAbout.invoke()
        }, leadingIcon = {}, trailingIcon = {})
        DropdownMenuItem(text = {
            Text(
                stringResource(id = R.string.goto_color_palette),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }, onClick = {
            ctx.startActivity(Intent(ctx, ColorPaletteActivity::class.java))
        }, leadingIcon = {} , trailingIcon = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun HomeTopBarPreview() {
    HomeTopBar(TopAppBarDefaults.enterAlwaysScrollBehavior(), true, Color.Black)
}