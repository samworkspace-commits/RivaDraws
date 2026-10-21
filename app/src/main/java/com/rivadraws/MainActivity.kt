package com.rivadraws

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Bitmap
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var coloringView: ColoringView
    private lateinit var modeButton: Button
    private var brushMode = false
    private var templateIndex = 0
    private val palette = intArrayOf(
        Color.rgb(35, 35, 45), Color.rgb(216, 65, 82), Color.rgb(244, 130, 105),
        Color.rgb(245, 184, 75), Color.rgb(245, 220, 117), Color.rgb(103, 169, 126),
        Color.rgb(63, 137, 145), Color.rgb(83, 126, 190), Color.rgb(128, 101, 176),
        Color.rgb(202, 126, 174), Color.rgb(151, 111, 83), Color.WHITE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(248, 245, 239)
        window.navigationBarColor = Color.rgb(248, 245, 239)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(248, 245, 239))
            setPadding(dp(12), dp(10), dp(12), dp(8))
        }
        root.addView(TextView(this).apply {
            text = "RivaDraws"; textSize = 26f; setTextColor(Color.rgb(47, 48, 57))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }, LinearLayout.LayoutParams(-1, dp(42)))
        root.addView(TextView(this).apply {
            text = "A little space for mindful colour"; textSize = 13f
            setTextColor(Color.rgb(112, 108, 104)); gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, dp(26)))

        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        modeButton = actionButton("Mode: Fill") { toggleMode() }
        toolbar.addView(modeButton, weightedButtonParams())
        toolbar.addView(actionButton("Undo") { coloringView.undo() }, weightedButtonParams())
        toolbar.addView(actionButton("Next design") {
            templateIndex = (templateIndex + 1) % 3
            coloringView.setTemplate(templateIndex)
        }, weightedButtonParams())
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(48)))

        coloringView = ColoringView(this)
        root.addView(coloringView, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(TextView(this).apply {
            text = "COLOUR PALETTE"; textSize = 11f; letterSpacing = 0.12f
            setTextColor(Color.rgb(112, 108, 104)); setPadding(dp(4), dp(8), 0, dp(4))
        })
        val paletteScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val paletteRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        palette.forEachIndexed { index, color ->
            val swatch = Button(this).apply {
                text = if (index == 0) "✓" else ""; textSize = 13f
                setTextColor(if (luminance(color) < 0.45) Color.WHITE else Color.BLACK)
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(color))
                contentDescription = "Colour " + (index + 1)
                minWidth = dp(42); minimumWidth = dp(42); minHeight = dp(42); minimumHeight = dp(42)
                setPadding(0, 0, 0, 0)
                setOnClickListener {
                    coloringView.selectedColor = color
                    for (i in 0 until paletteRow.childCount) {
                        (paletteRow.getChildAt(i) as? Button)?.text = if (i == index) "✓" else ""
                    }
                }
            }
            paletteRow.addView(swatch, LinearLayout.LayoutParams(dp(42), dp(42)).apply {
                setMargins(dp(3), dp(2), dp(3), dp(2))
            })
        }
        paletteScroll.addView(paletteRow)
        root.addView(paletteScroll, LinearLayout.LayoutParams(-1, dp(50)))
        root.addView(actionButton("Save artwork") { saveArtwork() }, LinearLayout.LayoutParams(-1, dp(46)))
        setContentView(root)
    }

    private fun toggleMode() {
        brushMode = !brushMode
        coloringView.brushMode = brushMode
        modeButton.text = if (brushMode) "Mode: Brush" else "Mode: Fill"
    }

    private fun saveArtwork() {
        val bitmap = coloringView.exportBitmap() ?: run {
            Toast.makeText(this, "Artwork is not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "RivaDraws-" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RivaDraws")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create image file")
            contentResolver.openOutputStream(uri).use { stream ->
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Could not save image")
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completed = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                contentResolver.update(uri, completed, null, null)
            }
            Toast.makeText(this, "Saved to Pictures/RivaDraws", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: " + (e.localizedMessage ?: "unknown error"), Toast.LENGTH_LONG).show()
        } finally {
            bitmap.recycle()
        }
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false; setOnClickListener { action() }
    }
    private fun weightedButtonParams() = LinearLayout.LayoutParams(0, dp(44), 1f).apply {
        setMargins(dp(2), 0, dp(2), 0)
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun luminance(color: Int): Double =
        (0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color)) / 255.0
}
