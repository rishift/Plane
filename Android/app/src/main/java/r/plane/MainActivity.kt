package r.plane

import android.os.Bundle
import android.os.StrictMode
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import r.plane.ui.theme.PlaneTheme
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

val socket = DatagramSocket()
val address: InetAddress = InetAddress.getByName("192.168.4.1")


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideBars(window)
        socket.broadcast = false

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())

        setContent {
            PlaneTheme {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    LocalView.current.keepScreenOn = true

                    val borderThickness = 5.dp
                    val thumbSize = 150.dp
                    val size = 270.dp
                    val maxR = with(LocalDensity.current) { 120.dp.toPx() }
                    val centre = with(LocalDensity.current) { ((size - thumbSize) / 2).toPx() }
                    val outerModifier = Modifier
                        .background(Color.Black)
                        .size(size)
                        .border(borderThickness, Color.White, CircleShape)

                    val data: ByteArray = byteArrayOf(0, 0)
                    val restData: ByteArray = byteArrayOf(90, 99)

                    Box(outerModifier) {
                        var r by remember { mutableFloatStateOf(maxR) }
                        var prevThrottle: Byte = 0

                        Box(
                            Modifier
                                .offset { IntOffset(centre.roundToInt(), (centre + r).roundToInt()) }
                                .clip(CircleShape)
                                .background(Color.White)
                                .size(thumbSize)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { change, dragAmount ->
                                        change.consume()
                                        r += dragAmount

                                        if (r >= maxR)
                                            r = maxR
                                        else if (r <= -maxR)
                                            r = -maxR

                                        data[0] = 255.toByte()
                                        data[1] = ((1 - r / maxR) * 127.5)
                                            .toInt()
                                            .toByte()
                                        if (data[1] != prevThrottle)
                                            sendUDP(data)

                                        prevThrottle = data[1]
                                    }
                                }
                        )
                    }

                    Box(outerModifier) {
                        var r: Float
                        var theta: Float
                        var x = 0f
                        var y = 0f
                        var posx by remember { mutableFloatStateOf(0f) }
                        var posy by remember { mutableFloatStateOf(0f) }

                        var prevX: Byte = 0
                        var prevY: Byte = 0

                        Box(
                            Modifier
                                .offset { IntOffset((centre + posx).roundToInt(), (centre + posy).roundToInt()) }
                                .clip(CircleShape)
                                .background(Color.White)
                                .size(thumbSize)
                                .pointerInput(Unit) {
                                    detectDragGestures(onDragEnd = {
                                        x = 0f
                                        y = 0f
                                        posx = 0f
                                        posy = 0f
                                        r = 0f
                                        theta = 0f

                                        sendUDP(restData)
                                        sendUDP(restData)
                                        sendUDP(restData)

                                    }) { change, dragAmount ->
                                        change.consume()

                                        x += dragAmount.x
                                        y += dragAmount.y
                                        r = sqrt(x * x + y * y)
                                        theta = angle(x, y)

                                        if (r > maxR)
                                            r = maxR

                                        posx = r * cos(theta)
                                        posy = r * sin(theta)

                                        data[1] = ((posx + maxR) * 30 / maxR + 69)
                                            .toInt()
                                            .toByte()
                                        data[0] = ((posy + maxR) * 40 / maxR + 50)
                                            .toInt()
                                            .toByte()

                                        if (data[0] != prevX || data[1] != prevY) {
                                            sendUDP(data)
                                        }

                                        prevX = data[0]
                                        prevY = data[1]
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun angle(x: Float, y: Float): Float {
    return if (x >= 0 && y >= 0)
        atan(y / x)
    else if (x < 0 && y >= 0)
        (Math.PI).toFloat() + atan(y / x)
    else if (x < 0 && y < 0)
        -(Math.PI).toFloat() + atan(y / x)
    else
        atan(y / x)
}

private fun sendUDP(data: ByteArray) {
    socket.send(DatagramPacket(data, 2, address, 11))
}

private fun hideBars(window: Window) {
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
}

