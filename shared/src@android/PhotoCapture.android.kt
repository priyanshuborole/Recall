package com.zerotwodev.recall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zerotwodev.recall.domain.MAX_PHOTOS_AT_ONCE
import com.zerotwodev.recall.domain.PhotoPickResult
import com.zerotwodev.recall.domain.PhotoPicker
import com.zerotwodev.recall.ui.ImageStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.uuid.Uuid

private const val MAX_SIDE = 2048
private const val JPEG_QUALITY = 85

internal class AndroidImageStore(context: Context) : ImageStore {
    private val mediaDir = File(context.filesDir, "media")

    override suspend fun load(fileName: String, maxSidePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val path = File(mediaDir, File(fileName).name).absolutePath
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSidePx) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }
}

/** The activity connects the system photo picker here, because only an activity can open it. */
class AndroidPhotoPicker(private val context: Context) : PhotoPicker {
    var launcher: (() -> Unit)? = null
    private var pending: CompletableDeferred<List<Uri>>? = null
    private val mediaDir = File(context.filesDir, "media")

    override suspend fun pick(): PhotoPickResult {
        val launch = launcher ?: return PhotoPickResult.Failed
        val result = CompletableDeferred<List<Uri>>()
        pending = result
        launch()
        val uris = result.await().take(MAX_PHOTOS_AT_ONCE)
        if (uris.isEmpty()) return PhotoPickResult.Cancelled
        val saved = withContext(Dispatchers.IO) { uris.mapNotNull { save(it) } }
        return if (saved.isEmpty()) PhotoPickResult.Failed else PhotoPickResult.Saved(saved)
    }

    fun onResult(uris: List<Uri>) {
        pending?.complete(uris)
        pending = null
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { File(mediaDir, File(fileName).name).delete() }
    }

    private fun save(uri: Uri): String? {
        val name = "${Uuid.random()}.jpg"
        val temp = File(mediaDir, "$name.tmp")
        return try {
            mediaDir.mkdirs()
            val bitmap = decode(uri) ?: return null
            val written = temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            if (written && temp.renameTo(File(mediaDir, name))) name else null
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            temp.delete()
        }
    }

    private fun decode(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = orientationMatrix(orientation)
        val longSide = max(decoded.width, decoded.height)
        if (longSide > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / longSide
            matrix.postScale(scale, scale)
        }
        val shaped = if (matrix.isIdentity) {
            decoded
        } else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }
        return if (shaped.hasAlpha()) onWhite(shaped) else shaped
    }

    /** JPEG has no transparency, and transparent pixels would otherwise turn black. */
    private fun onWhite(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        source.recycle()
        return result
    }

    private fun orientationMatrix(orientation: Int) = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                postRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                postRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }
}
