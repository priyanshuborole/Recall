package com.zerotwodev.recall.domain

const val MAX_PHOTOS_AT_ONCE = 5

sealed interface PhotoPickResult {
    /** Saved in the app's media folder as JPEGs, at most 2048 px on the long side. */
    data class Saved(val fileNames: List<String>) : PhotoPickResult
    data object Cancelled : PhotoPickResult
    data object Failed : PhotoPickResult
}

interface PhotoPicker {
    /** Lets the person choose up to [MAX_PHOTOS_AT_ONCE] photos. */
    suspend fun pick(): PhotoPickResult

    /** Removes a saved photo whose memory could not be updated. */
    suspend fun delete(fileName: String)
}
