package com.zerotwodev.recall.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

/** Loads saved photos from the app's media folder. */
interface ImageStore {
    /** Returns the photo scaled down to about [maxSidePx], or null if it can't be read. */
    suspend fun load(fileName: String, maxSidePx: Int): ImageBitmap?
}

@Composable
fun PhotoThumbnail(fileName: String, imageStore: ImageStore, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, fileName) { value = imageStore.load(fileName, 320) }
    Box(modifier.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let {
            Image(it, contentDescription = "Photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}
