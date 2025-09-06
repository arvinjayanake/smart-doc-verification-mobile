package com.arvin.smartdocmobile.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

fun readBitmapFromUri(context: Context, uri: Uri, maxSide: Int = 1600): Bitmap {
    val resolver: ContentResolver = context.contentResolver

    // Decode actual bitmap
    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
    resolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Cannot open InputStream for $uri" }
        var bmp = BitmapFactory.decodeStream(input, null, opts)
            ?: error("Failed to decode bitmap")

        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val largest = max(w, h)
        if (largest > maxSide) {
            val scale = maxSide / largest
            val nw = (w * scale).roundToInt()
            val nh = (h * scale).roundToInt()
            bmp = Bitmap.createScaledBitmap(bmp, nw, nh, true)
        }
        return bmp
    }
}

fun Bitmap.toBase64Jpeg(quality: Int = 90): String {
    val baos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, quality, baos)
    val bytes = baos.toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}