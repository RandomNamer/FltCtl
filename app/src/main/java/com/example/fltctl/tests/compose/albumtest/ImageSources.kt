package com.example.fltctl.tests.compose.albumtest

/**
 * Created by zeyu.zyzhang on 3/19/25
 * @author zeyu.zyzhang@bytedance.com
 */
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.core.graphics.createBitmap
import com.example.fltctl.AppMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.math.min
import kotlin.random.Random

// ======= MODEL LAYER =======

@Stable
// Interface for image sources
interface ImageSource {
    // Unique identifier for the image
    val id: String

    // Description of the image
    val description: String

    // Provides a thumbnail bitmap
    suspend fun provideThumbnail(): Bitmap

    suspend fun provideImage(): Bitmap

    // Image dimensions
    val width: Int
    val height: Int

}

val ImageSource.size: Size
    get() = Size(width.toFloat(), height.toFloat())

// Base implementation for all image sources
abstract class BaseImageSource(
    override val id: String,
    override val description: String,
    override val width: Int,
    override val height: Int
) : ImageSource {

    companion object {
        // Resolution ranges for different image orientations
        val portraitWidthRange = 750..1250
        val portraitHeightRange = 1500..2250

        val landscapeWidthRange = 1500..2250
        val landscapeHeightRange = 750..1250

        val squareRange = 1000..2000
    }

    protected fun getDefaultThumbnailSize(): Pair<Int, Int> {
        val maxThumbnailDimension = 150
        val thumbnailWidth: Int
        val thumbnailHeight: Int

        if (width >= height) {
            thumbnailWidth = min(maxThumbnailDimension, width)
            thumbnailHeight = (height.toFloat() * thumbnailWidth / width).toInt()
        } else {
            thumbnailHeight = min(maxThumbnailDimension, height)
            thumbnailWidth = (width.toFloat() * thumbnailHeight / height).toInt()
        }
        return Pair(thumbnailWidth, thumbnailHeight)
    }

}

// Pure color image source implementation
class ColorImageSource private constructor(
    id: String,
    description: String,
    width: Int,
    height: Int,
    private val seed: Int
) : BaseImageSource(id, description, width, height) {

    // Top-level helper functions for color generation
    companion object {
        // Generate a random RGB color from a seed
        fun generateRandomColor(seed: Int): Int {
            val random = Random(seed)
            return Color.rgb(
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)
            )
        }

        // Ensure color is not transparent
        fun maskAlphaChannel(color: Int): Int {
            return color or 0xFF000000.toInt() // Set alpha to 255 (fully opaque)
        }

        private val descriptions = listOf(
            "Sunset at the beach",
            "Mountain landscape",
            "Urban cityscape",
            "Forest trail",
            "Autumn leaves",
            "Desert dunes",
            "Snowy peaks",
            "Ocean waves",
            "Starry night",
            "Wildflowers",
            "Abandoned building",
            "Misty morning"
        )



        fun createSources(count: Int): List<ImageSource> {
            return List(count) { index ->
                val seed = index + 1
                val random = Random(seed)

                val orientation = random.nextInt(1, 4)

                val (width, height) = when (orientation) {
                    1 -> { // Portrait
                        Pair(
                            random.nextInt(portraitWidthRange.first, portraitWidthRange.last),
                            random.nextInt(portraitHeightRange.first, portraitHeightRange.last)
                        )
                    }
                    2 -> { // Landscape
                        Pair(
                            random.nextInt(landscapeWidthRange.first, landscapeWidthRange.last),
                            random.nextInt(landscapeHeightRange.first, landscapeHeightRange.last)
                        )
                    }
                    else -> { // Square
                        val size = random.nextInt(squareRange.first, squareRange.last)
                        Pair(size, size)
                    }
                }

                val description = descriptions.random(random)

                ColorImageSource(
                    id = "img_$index",
                    description = description,
                    width = width,
                    height = height,
                    seed = seed
                )
            }
        }
    }

    private val color: Int by lazy {
        maskAlphaChannel(generateRandomColor(seed))
    }

    override suspend fun provideThumbnail(): Bitmap {
        val (thumbnailWidth, thumbnailHeight) = getDefaultThumbnailSize()

        return createBitmap(thumbnailWidth, thumbnailHeight).apply {
            eraseColor(color)
        }
    }

    override suspend fun provideImage(): Bitmap {
        return withContext(Dispatchers.IO) {
            createBitmap(width, height, Bitmap.Config.RGB_565).apply {
                eraseColor(color)
            }
        }
    }
}

class BlurHashImageSource private constructor(
    id: String,
    description: String,
    width: Int,
    height: Int,
    private val blurHash: String
) : BaseImageSource(id, description, width, height) {

    companion object {
        private val descriptions = listOf(
            "Abstract pattern",
            "Colorful gradient",
            "Blurred landscape",
            "Soft focus portrait",
            "Hazy mountain view",
            "Dreamy seascape",
            "Bokeh lights",
            "Impressionist scene",
            "Motion blur",
            "Pastel colors",
            "Diffused light",
            "Muted tones"
        )

        private val blurHashCharset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~"

        // Generate a random BlurHash string
        private fun generateRandomBlurHash(random: Random): String {
            // BlurHash typically starts with "L" followed by a digit for components
            val componentX = random.nextInt(1, 10)
            val componentY = random.nextInt(1, 10)

            // Start with the standard format indicator
            val hashBuilder = StringBuilder("L")

            // Add component info - typically a single digit or character
            hashBuilder.append(blurHashCharset[componentX])
            hashBuilder.append(blurHashCharset[componentY])

            // Generate a random length for the hash (most are around 20-30 chars)
            val hashLength = random.nextInt(20, 32)

            // Fill the rest with random characters from the BlurHash charset
            repeat(hashLength - 3) {
                hashBuilder.append(blurHashCharset[random.nextInt(blurHashCharset.length)])
            }

            return hashBuilder.toString()
        }

        // Factory method to create a collection of BlurHash image sources
        fun createSources(count: Int): List<BlurHashImageSource> {
            return List(count) { index ->
                val random = Random(index + 1)

                // Decide on orientation (1: portrait, 2: landscape, 3: square)
                val orientation = random.nextInt(1, 4)

                // Generate width and height based on orientation
                val (width, height) = when (orientation) {
                    1 -> { // Portrait
                        Pair(
                            random.nextInt(portraitWidthRange.first, portraitWidthRange.last),
                            random.nextInt(portraitHeightRange.first, portraitHeightRange.last)
                        )
                    }
                    2 -> { // Landscape
                        Pair(
                            random.nextInt(landscapeWidthRange.first, landscapeWidthRange.last),
                            random.nextInt(landscapeHeightRange.first, landscapeHeightRange.last)
                        )
                    }
                    else -> { // Square
                        val size = random.nextInt(squareRange.first, squareRange.last)
                        Pair(size, size)
                    }
                }

                val description = descriptions.random(random)
                val blurHash = generateRandomBlurHash(random)

                BlurHashImageSource(
                    id = "blurhash_$index",
                    description = description,
                    width = width,
                    height = height,
                    blurHash = blurHash
                )
            }
        }

        // For a real implementation, you would use a BlurHash decoding library
        // This is a simplified version that generates a gradient-like image based on hash
        private fun decodeBlurHash(blurHash: String, width: Int, height: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Extract some "color" information from the hash
            val hashSum = blurHash.sumOf { it.code }
            val r = (hashSum % 255)
            val g = ((hashSum * 13) % 255)
            val b = ((hashSum * 7) % 255)

            // Create a base color
            val baseColor = Color.rgb(r, g, b)

            // Create a simple gradient based on the hash characters
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val xRatio = x.toFloat() / width
                    val yRatio = y.toFloat() / height

                    // Simple gradient calculation
                    val red = (r * xRatio).toInt()
                    val green = (g * yRatio).toInt()
                    val blue = (b * (1 - (xRatio + yRatio) / 2)).toInt()

                    val pixelColor = Color.rgb(
                        red.coerceIn(0, 255),
                        green.coerceIn(0, 255),
                        blue.coerceIn(0, 255)
                    )

                    bitmap.setPixel(x, y, pixelColor)
                }
            }

            return bitmap
        }
    }

    private var thumbnailCache: WeakReference<Bitmap> = WeakReference(null)

    private var imageCache: WeakReference<Bitmap> = WeakReference(null)

    override suspend fun provideThumbnail(): Bitmap {
        thumbnailCache.get()?.let { return it } ?: run {
            val (thumbnailWidth, thumbnailHeight) = getDefaultThumbnailSize()
            return decodeBlurHash(blurHash, thumbnailWidth, thumbnailHeight).also {
                thumbnailCache = WeakReference(it)
            }
        }
    }

    override suspend fun provideImage(): Bitmap {
        return imageCache.get()?.let { return it }?: withContext(Dispatchers.IO) {
            decodeBlurHash(blurHash, width, height).also {
                imageCache = WeakReference(it)
            }
        }
    }
}

class CheckerboardImageSource private constructor(
    id: String,
    description: String,
    width: Int,
    height: Int,
    private val tileSize: Int
) : BaseImageSource(id, description, width, height) {
    companion object {
        private val portraitWidthRange = 750..1250
        private val portraitHeightRange = 1500..2250

        private val landscapeWidthRange = 1500..2250
        private val landscapeHeightRange = 750..1250

        private val squareRange = 1000..2000

        private val tileSizeRange = 50..200

        fun createSources(count: Int): List<CheckerboardImageSource> = List(count) { index ->
                val random = Random(index + 1)
                // Decide on orientation (1: portrait, 2: landscape, 3: square)
                val orientation = random.nextInt(1, 4)
                // Generate width and height based on orientation
                val (width, height, tileSize) = when (orientation) {
                    1 -> { // Portrait
                        Triple(
                            random.nextInt(portraitWidthRange.first, portraitWidthRange.last),
                            random.nextInt(portraitHeightRange.first, portraitHeightRange.last),
                            random.nextInt(tileSizeRange.first, tileSizeRange.last)
                        )
                    }
                    2 -> { // Landscape
                        Triple(
                            random.nextInt(landscapeWidthRange.first, landscapeWidthRange.last),
                            random.nextInt(landscapeHeightRange.first, landscapeHeightRange.last),
                            random.nextInt(tileSizeRange.first, tileSizeRange.last)
                        )
                }
                    else -> { // Square
                        val size = random.nextInt(squareRange.first, squareRange.last)
                        Triple(size, size, random.nextInt(tileSizeRange.first, tileSizeRange.last))
                    }
                }
                val description = "Checkerboard $tileSize x $tileSize"
                CheckerboardImageSource(
                    id = "checkerboard_$index",
                    description = description,
                    width = width,
                    height = height,
                    tileSize = tileSize
                )
        }

        private fun generateCheckerboard(w: Int, h: Int, fgColor: Int = Color.BLACK, bgColor: Int = Color.WHITE, tileSize: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            for (y in 0 until h step tileSize) {
                for (x in 0 until w step tileSize) {
                    paint.color = if ((x / tileSize + y / tileSize) % 2 == 0) fgColor else bgColor
                    canvas.drawRect(
                        x.toFloat(),
                        y.toFloat(),
                        minOf(x + tileSize, w).toFloat(),
                        minOf(y + tileSize, h).toFloat(),
                        paint
                    )
                }
            }
            return bitmap
        }
    }

    override suspend fun provideThumbnail(): Bitmap {
        val (thumbnailWidth, thumbnailHeight) = getDefaultThumbnailSize()
        val scalingFactor = minOf(
            thumbnailWidth.toFloat() / width.toFloat(),
            thumbnailHeight.toFloat() / height.toFloat()
        )
        return generateCheckerboard(thumbnailWidth, thumbnailHeight, tileSize = (tileSize * scalingFactor).toInt())
    }

    override suspend fun provideImage(): Bitmap {
        return withContext(Dispatchers.Default) {
            generateCheckerboard(width, height, tileSize = tileSize)
        }
    }
}

// ======= REPOSITORY LAYER =======

// Repository for providing image sources
abstract class ImageRepository {
    companion object {
        const val DEFAULT_LOAD_SIZE = 30

        const val DEFAULT_TEST_REPO_SIZE = 1000

        fun create(type: Type): ImageRepository = when (type) {
            Type.COLOR -> ColorTileImageRepo()
            Type.BLURHASH -> BlurHashImageRepo()
            Type.CHECKERBORAD -> CheckerboardImageRepo()
            Type.SYSTEM_ALBUM -> {
                SystemAlbumImageRepo(AppMonitor.appContext)
            }
            else -> object: ImageRepository() {
                override suspend fun load(cursor: Int, count: Int)= emptyList<ImageSource>()
                override suspend fun count(): Int = 0
            }
        }
    }

    enum class Type{
        COLOR,
        BLURHASH,
        CHECKERBORAD,
        SYSTEM_ALBUM
    }

    open suspend fun refresh() = load(0)

    abstract suspend fun load(cursor: Int, count: Int = DEFAULT_LOAD_SIZE): List<ImageSource>

    abstract suspend fun count(): Int
}

abstract class BaseGeneratedImageRepo: ImageRepository() {
    private val _all = mutableListOf<ImageSource>()

    final override suspend fun load(cursor: Int, count: Int): List<ImageSource> {
        val generationsNeeded = cursor + count - _all.count()
        if (generationsNeeded > 0) generateImageSources(generationsNeeded).also { _all.addAll(it) }
        return _all.slice(cursor..cursor + count-1)
    }

    abstract suspend fun generateImageSources(count: Int): List<ImageSource>
}

class ColorTileImageRepo(private val count: Int = DEFAULT_TEST_REPO_SIZE): BaseGeneratedImageRepo() {
    override suspend fun generateImageSources(count: Int): List<ImageSource> = ColorImageSource.createSources(count)
    override suspend fun count(): Int = count
}

class BlurHashImageRepo(private val count: Int = DEFAULT_TEST_REPO_SIZE) : BaseGeneratedImageRepo() {
    override suspend fun count(): Int = count

    override suspend fun generateImageSources(count: Int): List<ImageSource> = BlurHashImageSource.createSources(count)

}

class CheckerboardImageRepo(private val count: Int = DEFAULT_TEST_REPO_SIZE): BaseGeneratedImageRepo() {
    override suspend fun generateImageSources(count: Int) = CheckerboardImageSource.createSources(count)
    override suspend fun count(): Int = count
}
