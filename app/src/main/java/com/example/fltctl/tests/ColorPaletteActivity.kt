package com.example.fltctl.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.fltctl.ui.BaseComposeActivity

class ColorPaletteActivity : BaseComposeActivity() {
    @Composable
    override fun Content() {
        Surface {
            val colorsWithDesc = mutableMapOf<String, Color>().apply {
                androidx.compose.material3.MaterialTheme.colorScheme.run {
                    put("background", background)
                    put("surface", surface)
                    put("primary", primary)
                    put("secondary", secondary)
                    put("tertiary", tertiary)
                    put("onPrimary", onPrimary)
                    put("onSecondary", onSecondary)
                    put("onTertiary", onTertiary)
                    put("primaryContainer", primaryContainer)
                    put("secondaryContainer", secondaryContainer)
                    put("tertiaryContainer", tertiaryContainer)
                    put("onPrimaryContainer", onPrimaryContainer)
                    put("onSecondaryContainer", onSecondaryContainer)
                    put("onTertiaryContainer", onTertiaryContainer)
                    put("onBackground", onBackground)
                    put("onSurface", onSurface)
                    put("onSurfaceVariant", onSurfaceVariant)
                    put("surfaceVariant", surfaceVariant)
                }
            }
            Box(Modifier.fillMaxWidth().background(Color(0xFF6A6C6E))) {
                ColorPalette(colors = colorsWithDesc)
            }
        }
    }

    @Composable
    fun ColorPalette(colors: Map<String, Color>) {
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp), content = {
            items(colors.toList()) { colorDesc ->
                Box(
                    Modifier
                        .size(width = 160.dp, height = 80.dp)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(color = colorDesc.second),
                    Alignment.Center
                ) {
                    Text(text = colorDesc.first, color = generateTextColorFromBackground(colorDesc.second))
                }
            }
        })
    }

    private fun generateTextColorFromBackground(color: Color): Color {
        val hsl = FloatArray(3)
        //assert sRGB color space
        ColorUtils.RGBToHSL(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            hsl
        )
        return if (hsl[2] < 0.5f) Color.White else Color.Black
    }
}
