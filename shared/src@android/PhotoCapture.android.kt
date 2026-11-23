package com.zerotwodev.recall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.zerotwodev.recall.domain.PhotoPickResult
import com.zerotwodev.recall.domain.PhotoPicker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.uuid.Uuid

private const val MAX_SIDE = 2048
private const val JPEG_QUALITY = 85

/** The activity connects the system photo picker here, because only an activity can open it. */
class AndroidPhotoPicker(private val context: Context) : PhotoPicker {
    var launcher: (() -> Unit)? = null
    private var pending: CompletableDeferred<Uri?>? = null
    private val mediaDir = File(context.filesDir, "media")

    override suspend fun pick(): PhotoPickResult {
        val launch = launcher ?: return PhotoPickResult.Failed
        val result = CompletableDeferred<Uri?>()
        pending = result
        launch()
        val uri = result.await() ?: return PhotoPickResult.Cancelled
        return withContext(Dispatchers.IO) { save(uri) }
    }

    fun onResult(uri: Uri?) {
        pending?.complete(uri)
        pending = null
    }

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { File(mediaDir, File(fileName).name).delete() }
    }

    private fun save(uri: Uri): PhotoPickResult {
        val name = "${Uuid.random()}.jpg"
        val temp = File(mediaDir, "$name.tmp")
        return try {
            mediaDir.mkdirs()
            val bitmap = decode(uri) ?: return PhotoPickResult.Failed
            val written = temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            if (written && temp.renameTo(File(mediaDir, name))) PhotoPickResult.Saved(name) else PhotoPickResult.Failed
        } catch (_: Exception) {
            PhotoPickResult.Failed
        } catch (_: OutOfMemoryError) {
            PhotoPickResult.Failed
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
