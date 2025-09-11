package com.example.urbanfinder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.*

class Landscape360ViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force landscape and full screen mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        setContent {
            Custom360ViewerScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset orientation if needed
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

@Composable
fun Custom360ViewerScreen() {
    val imageBitmap = remember360ImageBitmap(R.drawable.image360)

    Box(modifier = Modifier.fillMaxSize()) {
        Custom360Viewer(
            imageBitmap = imageBitmap,
            modifier = Modifier.fillMaxSize(),
            initialYaw = 180f,
            fov = 75f
        )
    }
}

@Composable
fun Custom360Viewer(
    imageBitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    initialYaw: Float = 0f,
    initialPitch: Float = 0f,
    fov: Float = 60f,
    sensitivity: Float = 0.2f
) {
    var yaw by remember { mutableFloatStateOf(initialYaw) }
    var pitch by remember { mutableFloatStateOf(initialPitch) }
    var gestureStart by remember { mutableStateOf<Offset?>(null) }
    var lastYaw by remember { mutableFloatStateOf(0f) }
    var lastPitch by remember { mutableFloatStateOf(0f) }

    val scaledBitmap = remember(imageBitmap) {
        val maxDimension = 2048
        val img = imageBitmap.asAndroidBitmap()
        if (img.width > maxDimension || img.height > maxDimension) {
            val scale = maxDimension.toFloat() / max(img.width, img.height)
            Bitmap.createScaledBitmap(
                img,
                (img.width * scale).toInt(),
                (img.height * scale).toInt(),
                true
            ).asImageBitmap()
        } else {
            imageBitmap
        }
    }

    val imageWidth = scaledBitmap.width.toFloat()
    val imageHeight = scaledBitmap.height.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureStart = offset
                        lastYaw = yaw
                        lastPitch = pitch
                    },
                    onDrag = { change, _ ->
                        gestureStart?.let { start ->
                            val deltaX = change.position.x - start.x
                            val deltaY = change.position.y - start.y
                            yaw = (lastYaw - deltaX * sensitivity).mod(360f)
                            pitch = (lastPitch + deltaY * sensitivity * 0.5f).coerceIn(-89f, 89f)
                        }
                    },
                    onDragEnd = { gestureStart = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val normalizedX = (yaw / 360f).mod(1f)
            val normalizedY = (pitch + 90f) / 180f

            val viewportWidthRatio = fov / 360f
            val viewportHeightRatio = (fov / 360f) * (size.width / size.height)

            val srcWidth = viewportWidthRatio * imageWidth
            val srcHeight = viewportHeightRatio * imageHeight

            var srcLeft = (normalizedX - viewportWidthRatio / 2f).mod(1f) * imageWidth
            val srcTop = ((normalizedY - viewportHeightRatio / 2f).coerceIn(0f, 1f) * imageHeight)

            val wrapAround = srcLeft + srcWidth > imageWidth

            if (wrapAround) {
                val rightWidth = imageWidth - srcLeft
                val leftWidth = srcWidth - rightWidth

                drawImage(
                    image = scaledBitmap,
                    srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
                    srcSize = IntSize(rightWidth.toInt(), srcHeight.toInt()),
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize((size.width * (rightWidth / srcWidth)).toInt(), size.height.toInt())
                )

                drawImage(
                    image = scaledBitmap,
                    srcOffset = IntOffset(0, srcTop.toInt()),
                    srcSize = IntSize(leftWidth.toInt(), srcHeight.toInt()),
                    dstOffset = IntOffset((size.width * (rightWidth / srcWidth)).toInt(), 0),
                    dstSize = IntSize((size.width * (leftWidth / srcWidth)).toInt(), size.height.toInt())
                )
            } else {
                drawImage(
                    image = scaledBitmap,
                    srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
                    srcSize = IntSize(srcWidth.toInt(), srcHeight.toInt()),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
        }

        // Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { yaw = (yaw - 30f).mod(360f) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Pan left")
                }
                IconButton(onClick = { yaw = (yaw + 30f).mod(360f) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Pan right")
                }
                IconButton(onClick = {
                    yaw = 0f
                    pitch = 0f
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset view")
                }
            }
        }
    }
}

@Composable
fun remember360ImageBitmap(resourceId: Int, maxSize: Int = 4096): ImageBitmap {
    val context = LocalContext.current
    return remember(resourceId) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resourceId, options)

        val scale = max(1, max(options.outWidth / maxSize, options.outHeight / maxSize))
        options.inJustDecodeBounds = false
        options.inSampleSize = scale

        BitmapFactory.decodeResource(context.resources, resourceId, options).asImageBitmap()
    }
}
