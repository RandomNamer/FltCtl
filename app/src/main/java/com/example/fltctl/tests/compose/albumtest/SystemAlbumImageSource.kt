package com.example.fltctl.tests.compose.albumtest

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.lang.ref.WeakReference

/**
 * Implementation of ImageSource that loads images from the device's system album
 * using MediaStore API
 */
class SystemAlbumImageSource private constructor(
    override val id: String,
    override val description: String,
    override val width: Int,
    override val height: Int,
    private val contentUri: Uri,
    private val thumbnailUri: Uri?,
    private val contentResolver: ContentResolver
) : BaseImageSource(id, description, width, height) {

    private var thumbnailCache: WeakReference<Bitmap> = WeakReference(null)
    private var imageCache: WeakReference<Bitmap> = WeakReference(null)

    override suspend fun provideThumbnail(): Bitmap {
        // Return cached thumbnail if available
        thumbnailCache.get()?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                // First try to use the system thumbnail if available
                if (thumbnailUri != null) {
                    try {
                        contentResolver.openInputStream(thumbnailUri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                                thumbnailCache = WeakReference(bitmap)
                                return@withContext bitmap
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        // Thumbnail not found, will fall back to scaling the full image
                    }
                }

                // If thumbnail not available, scale down the full image
                val (thumbnailWidth, thumbnailHeight) = getDefaultThumbnailSize()
                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(width, height, thumbnailWidth, thumbnailHeight)
                }

                contentResolver.openInputStream(contentUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)?.let { bitmap ->
                        thumbnailCache = WeakReference(bitmap)
                        return@withContext bitmap
                    }
                }

                // If all else fails, create a placeholder bitmap
                createPlaceholderBitmap(thumbnailWidth, thumbnailHeight).also {
                    thumbnailCache = WeakReference(it)
                }
            } catch (e: Exception) {
                // Create a placeholder bitmap in case of any errors
                val (thumbnailWidth, thumbnailHeight) = getDefaultThumbnailSize()
                createPlaceholderBitmap(thumbnailWidth, thumbnailHeight).also {
                    thumbnailCache = WeakReference(it)
                }
            }
        }
    }

    override suspend fun provideImage(): Bitmap {
        // Return cached image if available
        imageCache.get()?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(contentUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                        imageCache = WeakReference(bitmap)
                        return@withContext bitmap
                    }
                }

                // If loading fails, create a placeholder bitmap
                createPlaceholderBitmap(width, height).also {
                    imageCache = WeakReference(it)
                }
            } catch (e: Exception) {
                // Create a placeholder bitmap in case of any errors
                createPlaceholderBitmap(width, height).also {
                    imageCache = WeakReference(it)
                }
            }
        }
    }

    private fun calculateInSampleSize(imageWidth: Int, imageHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        if (imageHeight > reqHeight || imageWidth > reqWidth) {
            val halfHeight = imageHeight / 2
            val halfWidth = imageWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun createPlaceholderBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.LTGRAY)
        }
    }

    companion object {
        // Factory method to create a SystemAlbumImageSource from a MediaStore cursor
        fun fromMediaStoreCursor(cursor: Cursor, contentResolver: ContentResolver): SystemAlbumImageSource? {
            // Get column indices
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

            // Extract values
            val id = cursor.getLong(idColumn)
            val displayName = cursor.getStringOrNull(displayNameColumn) ?: "Unknown Image"
            
            // Get dimensions, with fallbacks for older API levels
            val width = cursor.getIntOrNull(widthColumn) ?: 1000
            val height = cursor.getIntOrNull(heightColumn) ?: 1000

            // Create content URI for the image
            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            // Get thumbnail URI if available (API 29+)
            val thumbnailUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentUris.withAppendedId(
                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    id
                )
            } else {
                null
            }

            return SystemAlbumImageSource(
                id = "system_$id",
                description = displayName,
                width = width,
                height = height,
                contentUri = contentUri,
                thumbnailUri = thumbnailUri,
                contentResolver = contentResolver
            )
        }
    }
}

/**
 * Repository implementation for loading images from the system album
 */
class SystemAlbumImageRepo(private val context: Context) : ImageRepository() {
    private val contentResolver = context.contentResolver
    private val cachedSources = mutableListOf<ImageSource>()
    private var totalCount: Int = -1

    override suspend fun count(): Int {
        if (totalCount == -1) {
            totalCount = queryImagesCount()
        }
        return totalCount
    }

    override suspend fun load(cursor: Int, count: Int): List<ImageSource> {
        // If we've already loaded these images, return from cache
        if (cursor < cachedSources.size && cursor + count <= cachedSources.size) {
            return cachedSources.subList(cursor, cursor + count)
        }

        // If we need to load more images
        return withContext(Dispatchers.IO) {
            val newImages = queryImages(cursor, count)
            
            // Add new images to cache
            if (cursor >= cachedSources.size) {
                cachedSources.addAll(newImages)
            } else {
                // Handle partial overlap
                val overlapCount = cachedSources.size - cursor
                cachedSources.addAll(newImages.drop(overlapCount))
            }
            
            newImages
        }
    }


    private fun queryImagesCount(): Int {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
        )?.use { cursor ->
            return cursor.count
        }
        
        return 0
    }

    private fun queryImages(offset: Int, limit: Int): List<ImageSource> {
        val images = mutableListOf<ImageSource>()
        imageCursorWithPagination(limit, offset)?.use { cursor ->
            while (cursor.moveToNext()) {
                SystemAlbumImageSource.fromMediaStoreCursor(cursor, contentResolver)?.let {
                    images.add(it)
                }
            }
        }
        
        return images
    }

    private fun imageCursorWithPagination(
        pageSize: Int = 20,
        pageNumber: Int = 0
    ): Cursor? {
        // Calculate offset based on page number and size
        val offset = pageSize * pageNumber

        // Define the URI for accessing images
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // Define columns to retrieve
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        // Sort by date added in descending order (newest first)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        // Apply pagination using LIMIT and OFFSET
        val limitOffset = "$pageSize OFFSET $offset"

        // Execute the query
        return context.contentResolver.query(
            collection.buildUpon().appendQueryParameter("limit", limitOffset).build(),
            projection,
            null,  // No selection criteria
            null,  // No selection arguments
            sortOrder  // Sort order + pagination
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLimitOffsetBundle(offset: Int, limit: Int): android.os.Bundle {
        return android.os.Bundle().apply {
            // In Android O and above, these constants are available
            putString(ContentResolver.QUERY_ARG_SQL_LIMIT, limit.toString())
            putString(ContentResolver.QUERY_ARG_OFFSET, offset.toString())
        }
    }
}