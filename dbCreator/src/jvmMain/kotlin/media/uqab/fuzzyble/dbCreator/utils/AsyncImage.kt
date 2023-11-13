/*
MIT License

Copyright (c) [2023] [Shahriar Zaman]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package media.uqab.fuzzyble.dbCreator.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import media.uqab.fuzzyble.dbCreator.Const
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest


/**
 * Simple Composable function to load image from url.
 * This will download the image for the first time,
 * afterward it will load from cache.
 *
 * @author github/fCat97
 */
@OptIn(DelicateCoroutinesApi::class)
@Composable
fun AsyncImage(url: String, modifier: Modifier = Modifier.size(24.dp)) {
    var imageBitmap by remember(url) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(url) {
        GlobalScope.launch {
            imageBitmap = getImage(url)
        }
    }

    if (imageBitmap != null) {
        Image(bitmap = imageBitmap!!, null, modifier)
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun AsyncImage(url: String, content: @Composable (ImageBitmap) -> Unit) {
    var imageBitmap by remember(url) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(url) {
        GlobalScope.launch {
            imageBitmap = getImage(url)
        }
    }

    if (imageBitmap != null) {
        content(imageBitmap!!)
    }
}

private suspend fun getImage(url: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val cache = getFromCache(url)
            if (cache != null) return@withContext cache

            URL(url).openStream().use { ins ->
                saveToCache(url, ins)
            }
            getFromCache(url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private suspend fun getFromCache(url: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        val path = getCachePath(url) ?: return@withContext null
        if (!path.toFile().exists()) return@withContext null

        try {
            path.toFile().inputStream().use {
                loadImageBitmap(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private suspend fun saveToCache(url: String, bitmap: InputStream) {
    val path = getCachePath(url) ?: return
    withContext(Dispatchers.IO) {
        val percent = FileOutputStream(path.toFile()).use { fos ->
            val len = bitmap.available()
            val byteReadLen = bitmap.copyTo(fos)

            // success if 99% downloaded.
            byteReadLen.toFloat() / len
        }

        if (percent !in 0.99f..1f) {
            println("saveToCache: failed to cache ($percent%) $url")
            try {
                path.toFile().delete()
            } catch (e: Exception) {
                println("saveToCache: ${e.message}")
            }
        }
    }
}

private fun getCachePath(url: String): Path? {
    val userHome = System.getProperty("user.home")
    val cacheDirectory: Path = Paths.get(userHome, ".cache", Const.APP_NAME.lowercase(), "asyncImage")

    if (!cacheDirectory.toFile().exists()) {
        cacheDirectory.toFile().mkdirs()
        return null
    }

    val urlHash = calculateMD5(url)
    return Paths.get(cacheDirectory.toUri().path.replaceFirst("/", ""), urlHash)
}

private fun calculateMD5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val byteArray = md.digest(input.toByteArray())

    // Convert the byte array to a hexadecimal string
    val result = StringBuilder()
    for (byte in byteArray) {
        result.append(String.format("%02x", byte))
    }

    return result.toString()
}