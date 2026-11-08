package com.example.util

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

class QrCodeScannerAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    private var isThrottled = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isThrottled) {
            imageProxy.close()
            return
        }

        try {
            val image = imageProxy.image
            if (image != null) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val width = imageProxy.width
                val height = imageProxy.height

                val source = PlanarYUVLuminanceSource(
                    bytes,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    val result = reader.decodeWithState(binaryBitmap)
                    if (result != null && result.text.isNotBlank()) {
                        isThrottled = true
                        onQrCodeDetected(result.text)
                    }
                } catch (_: NotFoundException) {
                    // Normal when frame has no QR code
                } catch (_: Exception) {
                    // Ignore transient decode errors
                } finally {
                    reader.reset()
                }
            }
        } catch (_: Exception) {
            // Safe fallback
        } finally {
            imageProxy.close()
        }
    }

    fun resetThrottle() {
        isThrottled = false
    }

    companion object {
        /**
         * Decodes a QR code from a static Bitmap (e.g. from gallery, photo picker, or test image).
         */
        fun decodeQrFromBitmap(bitmap: Bitmap): String? {
            return try {
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                val source = RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader().apply {
                    setHints(
                        mapOf(
                            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                            DecodeHintType.TRY_HARDER to true
                        )
                    )
                }
                reader.decodeWithState(binaryBitmap)?.text
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Decodes a QR code from a content Uri (e.g. from photo picker or gallery).
         */
        fun decodeQrFromUri(context: android.content.Context, uri: android.net.Uri): String? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) decodeQrFromBitmap(bitmap) else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
