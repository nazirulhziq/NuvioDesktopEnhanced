package com.nuvio.app.core.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlagTextTest {
    @Test fun streamMetadataOnly() {
        for (text in listOf("GB / IT", "Stream [GB/IT]", "Audio: GB, IT", "🇬🇧🇮🇹")) {
            assertEquals(listOf("gb", "it"), streamFlagSpans(text).map { it.country }, text)
        }
        for (text in listOf("10 GB", "Do IT now", "IT", "release.GB.IT.mkv", "https://x/GB/IT", "[GB/ZZ]")) {
            assertTrue(streamFlagSpans(text).isEmpty(), text)
        }
        assertEquals("gb", subtitleFlagCountry("en"))
        assertEquals("it", subtitleFlagCountry("it"))
        assertEquals("us", subtitleFlagCountry("en-US"))
        assertEquals(null, subtitleFlagCountry("__unknown__"))
    }

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    @Test fun paintsBritishAndItalianFlagsInOrder() {
        val scene = ImageComposeScene(width = 400, height = 100) {
            FlagText("GB / IT", TextStyle(fontSize = 40.sp), Color.White)
        }
        try {
            val bytes = scene.render().use { it.encodeToData()!!.use { data -> data.bytes } }
            val image = ImageIO.read(ByteArrayInputStream(bytes))
            // Locate saturated flag colours (plain text is white). Sample the pattern inside each tile.
            fun pixel(x: Int, y: Int) = java.awt.Color(image.getRGB(x, y), true)
            val colored = (0 until image.width).flatMap { x -> (0 until image.height).mapNotNull { y ->
                val c = pixel(x,y)
                if (c.alpha > 200 && maxOf(c.red,c.green,c.blue) - minOf(c.red,c.green,c.blue) > 80) x to y else null
            } }
            assertTrue(colored.isNotEmpty(), "No coloured bitmap pixels")
            val left = colored.minOf { it.first }; val top = colored.minOf { it.second }
            val right = colored.maxOf { it.first }; val bottom = colored.maxOf { it.second }
            val height = bottom - top + 1; val width = height * 4 / 3; val middle = top + height / 2
            val blue = pixel(left + width * 3 / 10, top + height / 10)
            assertTrue(blue.blue > blue.red && blue.blue > blue.green, "Union Jack blue field")
            assertTrue(pixel(left + width / 2, middle).red > 150, "Union Jack red cross")
            val green = pixel(right - width * 5 / 6, middle)
            assertTrue(green.green > green.red && green.green > green.blue, "Italian green stripe")
            val white = pixel(right - width / 2, middle)
            assertTrue(minOf(white.red,white.green,white.blue,white.alpha) > 230, "Italian white stripe")
            val red = pixel(right - width / 6, middle)
            assertTrue(red.red > 150 && red.green < 110, "Italian red stripe")
        } finally { scene.close() }
    }
}
