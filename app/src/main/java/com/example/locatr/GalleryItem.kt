package com.example.locatr

import android.net.Uri
import com.google.gson.annotations.SerializedName

class GalleryItem {
    lateinit var title: String
    lateinit var id: String
    lateinit var owner: String

    @SerializedName("url_s")
    var url: String = ""

    fun getPhotoPageUri(): Uri = Uri.parse("https://www.flickr.com/photos")
        .buildUpon()
        .appendPath(owner)
        .appendPath(id)
        .build()

    override fun toString() = title
}
