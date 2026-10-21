package com.example.ui.components

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AppIconMemoryCache {
    // Package icons are reused heavily while flinging the apps list. Keeping the small
    // decoded bitmaps in memory avoids repeated PackageManager + bitmap work per row.
    private val cache = LruCache<String, ImageBitmap>(96)

    fun get(packageName: String): ImageBitmap? = synchronized(cache) { cache.get(packageName) }

    fun put(packageName: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache.put(packageName, bitmap) }
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val context = LocalContext.current

    val bitmapState = produceState<ImageBitmap?>(
        initialValue = AppIconMemoryCache.get(packageName),
        key1 = packageName
    ) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            AppIconMemoryCache.get(packageName) ?: try {
                val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
                // 64px is ample for the 32-44dp list icons and is much cheaper to decode
                // during a fast fling than the old 128px bitmap.
                drawable.toBitmap(64, 64).asImageBitmap().also {
                    AppIconMemoryCache.put(packageName, it)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
