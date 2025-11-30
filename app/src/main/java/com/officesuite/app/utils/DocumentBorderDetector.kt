package com.officesuite.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Document border detector using edge detection and contour analysis.
 * Implements a local (on-device) solution without requiring external libraries.
 */
class DocumentBorderDetector {

    data class DetectedCorners(
        val topLeft: PointF,
        val topRight: PointF,
        val bottomLeft: PointF,
        val bottomRight: PointF,
        val confidence: Float
    ) {
        fun toList(): List<PointF> = listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    /**
     * Detects document borders in the given bitmap.
     * @param bitmap The source image
     * @return DetectedCorners with the four corner points, or null if no document detected
     */
    fun detectBorders(bitmap: Bitmap): DetectedCorners? {
        // Scale down for faster processing
        val scaleFactor = calculateScaleFactor(bitmap.width, bitmap.height)
        val scaledBitmap = if (scaleFactor < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scaleFactor).toInt(),
                (bitmap.height * scaleFactor).toInt(),
                true
            )
        } else {
            bitmap
        }

        try {
            // Convert to grayscale
            val grayscale = toGrayscale(scaledBitmap)

            // Apply Gaussian blur to reduce noise
            val blurred = applyGaussianBlur(grayscale)

            // Apply Canny edge detection
            val edges = applyCanny(blurred)

            // Find contours and detect quadrilateral
            val corners = findDocumentCorners(edges, scaledBitmap.width, scaledBitmap.height)

            // Clean up intermediate bitmaps
            if (scaleFactor < 1.0f) {
                scaledBitmap.recycle()
            }
            grayscale.recycle()
            blurred.recycle()
            edges.recycle()

            // Scale corners back to original image size
            return corners?.let { c ->
                DetectedCorners(
                    topLeft = PointF(c.topLeft.x / scaleFactor, c.topLeft.y / scaleFactor),
                    topRight = PointF(c.topRight.x / scaleFactor, c.topRight.y / scaleFactor),
                    bottomLeft = PointF(c.bottomLeft.x / scaleFactor, c.bottomLeft.y / scaleFactor),
                    bottomRight = PointF(c.bottomRight.x / scaleFactor, c.bottomRight.y / scaleFactor),
                    confidence = c.confidence
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Applies perspective transform to crop and straighten the document.
     */
    fun cropDocument(bitmap: Bitmap, corners: DetectedCorners): Bitmap {
        val srcPoints = floatArrayOf(
            corners.topLeft.x, corners.topLeft.y,
            corners.topRight.x, corners.topRight.y,
            corners.bottomRight.x, corners.bottomRight.y,
            corners.bottomLeft.x, corners.bottomLeft.y
        )

        // Calculate output dimensions based on detected corners
        val topWidth = hypot(
            (corners.topRight.x - corners.topLeft.x).toDouble(),
            (corners.topRight.y - corners.topLeft.y).toDouble()
        )
        val bottomWidth = hypot(
            (corners.bottomRight.x - corners.bottomLeft.x).toDouble(),
            (corners.bottomRight.y - corners.bottomLeft.y).toDouble()
        )
        val leftHeight = hypot(
            (corners.bottomLeft.x - corners.topLeft.x).toDouble(),
            (corners.bottomLeft.y - corners.topLeft.y).toDouble()
        )
        val rightHeight = hypot(
            (corners.bottomRight.x - corners.topRight.x).toDouble(),
            (corners.bottomRight.y - corners.topRight.y).toDouble()
        )

        val outputWidth = maxOf(topWidth, bottomWidth).toInt().coerceAtLeast(100)
        val outputHeight = maxOf(leftHeight, rightHeight).toInt().coerceAtLeast(100)

        val dstPoints = floatArrayOf(
            0f, 0f,
            outputWidth.toFloat(), 0f,
            outputWidth.toFloat(), outputHeight.toFloat(),
            0f, outputHeight.toFloat()
        )

        val matrix = Matrix()
        if (matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)) {
            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
            }
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, matrix, paint)
            return output
        }

        // Fallback: return simple crop
        return cropSimple(bitmap, corners)
    }

    private fun cropSimple(bitmap: Bitmap, corners: DetectedCorners): Bitmap {
        val minX = minOf(corners.topLeft.x, corners.bottomLeft.x).toInt().coerceAtLeast(0)
        val maxX = maxOf(corners.topRight.x, corners.bottomRight.x).toInt().coerceAtMost(bitmap.width)
        val minY = minOf(corners.topLeft.y, corners.topRight.y).toInt().coerceAtLeast(0)
        val maxY = maxOf(corners.bottomLeft.y, corners.bottomRight.y).toInt().coerceAtMost(bitmap.height)

        val width = (maxX - minX).coerceAtLeast(1)
        val height = (maxY - minY).coerceAtLeast(1)

        return Bitmap.createBitmap(bitmap, minX, minY, width, height)
    }

    private fun calculateScaleFactor(width: Int, height: Int): Float {
        val maxDimension = 800
        val longestSide = maxOf(width, height)
        return if (longestSide > maxDimension) {
            maxDimension.toFloat() / longestSide
        } else {
            1.0f
        }
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(grayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return grayscale
    }

    private fun applyGaussianBlur(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 5x5 Gaussian kernel
        val kernel = arrayOf(
            intArrayOf(1, 4, 7, 4, 1),
            intArrayOf(4, 16, 26, 16, 4),
            intArrayOf(7, 26, 41, 26, 7),
            intArrayOf(4, 16, 26, 16, 4),
            intArrayOf(1, 4, 7, 4, 1)
        )
        val kernelSum = 273

        val result = IntArray(width * height)

        for (y in 2 until height - 2) {
            for (x in 2 until width - 2) {
                var sum = 0
                for (ky in -2..2) {
                    for (kx in -2..2) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val gray = pixel and 0xFF
                        sum += gray * kernel[ky + 2][kx + 2]
                    }
                }
                val blurred = (sum / kernelSum).coerceIn(0, 255)
                result[y * width + x] = (0xFF shl 24) or (blurred shl 16) or (blurred shl 8) or blurred
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyCanny(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sobel operators for gradient calculation
        val gradientX = IntArray(width * height)
        val gradientY = IntArray(width * height)
        val gradientMag = IntArray(width * height)

        // Calculate gradients using Sobel
        var maxGradient = 0
        var sumGradient = 0L
        var gradientCount = 0
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = (getGray(pixels, width, x + 1, y - 1) + 2 * getGray(pixels, width, x + 1, y) + getGray(pixels, width, x + 1, y + 1)) -
                        (getGray(pixels, width, x - 1, y - 1) + 2 * getGray(pixels, width, x - 1, y) + getGray(pixels, width, x - 1, y + 1))

                val gy = (getGray(pixels, width, x - 1, y + 1) + 2 * getGray(pixels, width, x, y + 1) + getGray(pixels, width, x + 1, y + 1)) -
                        (getGray(pixels, width, x - 1, y - 1) + 2 * getGray(pixels, width, x, y - 1) + getGray(pixels, width, x + 1, y - 1))

                gradientX[y * width + x] = gx
                gradientY[y * width + x] = gy
                val mag = sqrt((gx * gx + gy * gy).toFloat()).toInt()
                gradientMag[y * width + x] = mag
                
                if (mag > 0) {
                    maxGradient = maxOf(maxGradient, mag)
                    sumGradient += mag
                    gradientCount++
                }
            }
        }

        // Calculate adaptive thresholds based on gradient statistics
        val avgGradient = if (gradientCount > 0) (sumGradient / gradientCount).toInt() else 50
        val highThreshold = maxOf(avgGradient, 40)
        val lowThreshold = highThreshold / 2

        // Non-maximum suppression and double thresholding
        val result = IntArray(width * height)
        val strong = 255
        val weak = 100

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val mag = gradientMag[idx]

                if (mag < lowThreshold) {
                    result[idx] = 0
                    continue
                }

                val angle = atan2(gradientY[idx].toFloat(), gradientX[idx].toFloat())
                val direction = ((angle * 180 / Math.PI + 180) / 45).toInt() % 4

                val (dx1, dy1, dx2, dy2) = when (direction) {
                    0 -> arrayOf(1, 0, -1, 0)    // Horizontal
                    1 -> arrayOf(1, 1, -1, -1)   // Diagonal
                    2 -> arrayOf(0, 1, 0, -1)    // Vertical
                    else -> arrayOf(-1, 1, 1, -1) // Anti-diagonal
                }

                val neighbor1 = gradientMag[(y + dy1) * width + (x + dx1)]
                val neighbor2 = gradientMag[(y + dy2) * width + (x + dx2)]

                if (mag >= neighbor1 && mag >= neighbor2) {
                    result[idx] = if (mag >= highThreshold) strong else weak
                } else {
                    result[idx] = 0
                }
            }
        }
        
        // Edge tracking by hysteresis - connect weak edges to strong edges
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                if (result[idx] == weak) {
                    // Check if connected to a strong edge
                    var hasStrongNeighbor = false
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (result[(y + dy) * width + (x + dx)] == strong) {
                                hasStrongNeighbor = true
                                break
                            }
                        }
                        if (hasStrongNeighbor) break
                    }
                    result[idx] = if (hasStrongNeighbor) strong else 0
                }
            }
        }

        // Create edge bitmap
        val edgeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val edgePixels = IntArray(width * height)
        for (i in result.indices) {
            val v = if (result[i] == strong) 255 else 0
            edgePixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        edgeBitmap.setPixels(edgePixels, 0, width, 0, 0, width, height)

        return edgeBitmap
    }

    private fun getGray(pixels: IntArray, width: Int, x: Int, y: Int): Int {
        return pixels[y * width + x] and 0xFF
    }

    private fun findDocumentCorners(edges: Bitmap, width: Int, height: Int): DetectedCorners? {
        val pixels = IntArray(width * height)
        edges.getPixels(pixels, 0, width, 0, 0, width, height)

        // Find edge points
        val edgePoints = mutableListOf<PointF>()
        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
                if ((pixels[y * width + x] and 0xFF) > 128) {
                    edgePoints.add(PointF(x.toFloat(), y.toFloat()))
                }
            }
        }

        if (edgePoints.size < 50) {
            // Not enough edges detected, return default corners with margin
            return createDefaultCorners(width, height)
        }

        // Try to find the largest quadrilateral contour first
        val contourCorners = findLargestQuadrilateralContour(pixels, width, height)
        if (contourCorners != null) {
            return contourCorners
        }

        // Use Hough transform to detect lines
        val lines = detectLines(edgePoints, width, height)

        if (lines.size >= 4) {
            // Find intersection points of dominant lines
            val corners = findQuadrilateralCorners(lines, width, height)
            if (corners != null) {
                return corners
            }
        }

        // Fallback: find convex hull corners
        return findConvexHullCorners(edgePoints, width, height)
    }

    /**
     * Find the largest quadrilateral contour in the edge image.
     * This is better suited for detecting document boundaries.
     */
    private fun findLargestQuadrilateralContour(pixels: IntArray, width: Int, height: Int): DetectedCorners? {
        // Find contours using a simple contour tracing algorithm
        val visited = BooleanArray(width * height)
        val contours = mutableListOf<List<PointF>>()
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                if ((pixels[idx] and 0xFF) > 128 && !visited[idx]) {
                    // Start tracing a contour
                    val contour = traceContour(pixels, visited, width, height, x, y)
                    if (contour.size >= 20) { // Minimum contour size
                        contours.add(contour)
                    }
                }
            }
        }
        
        // Find the largest contour by area
        var largestContour: List<PointF>? = null
        var maxArea = 0.0
        
        for (contour in contours) {
            val area = calculateContourArea(contour)
            val imageArea = width.toDouble() * height
            
            // Contour should cover at least 10% of the image
            if (area > imageArea * 0.1 && area > maxArea) {
                maxArea = area
                largestContour = contour
            }
        }
        
        if (largestContour == null || largestContour.size < 4) {
            return null
        }
        
        // Simplify the contour to a quadrilateral using Douglas-Peucker algorithm
        val simplified = simplifyContour(largestContour, largestContour.size / 20.0)
        
        // Try to find 4 corner points
        val corners = findFourCorners(simplified, width, height)
        
        return if (corners != null) {
            // Calculate confidence based on how well the contour covers the image
            val confidence = (maxArea / (width.toDouble() * height)).coerceIn(0.3, 0.95).toFloat()
            DetectedCorners(
                topLeft = corners[0],
                topRight = corners[1],
                bottomRight = corners[2],
                bottomLeft = corners[3],
                confidence = confidence
            )
        } else {
            null
        }
    }

    private fun traceContour(
        pixels: IntArray, 
        visited: BooleanArray, 
        width: Int, 
        height: Int, 
        startX: Int, 
        startY: Int
    ): List<PointF> {
        val contour = mutableListOf<PointF>()
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(Pair(startX, startY))
        
        val directions = arrayOf(
            Pair(-1, -1), Pair(0, -1), Pair(1, -1),
            Pair(-1, 0),              Pair(1, 0),
            Pair(-1, 1),  Pair(0, 1),  Pair(1, 1)
        )
        
        while (stack.isNotEmpty() && contour.size < 5000) {
            val (x, y) = stack.removeLast()
            val idx = y * width + x
            
            if (x < 1 || x >= width - 1 || y < 1 || y >= height - 1) continue
            if (visited[idx]) continue
            if ((pixels[idx] and 0xFF) <= 128) continue
            
            visited[idx] = true
            contour.add(PointF(x.toFloat(), y.toFloat()))
            
            for ((dx, dy) in directions) {
                val nx = x + dx
                val ny = y + dy
                val nidx = ny * width + nx
                if (nidx in pixels.indices && !visited[nidx] && (pixels[nidx] and 0xFF) > 128) {
                    stack.add(Pair(nx, ny))
                }
            }
        }
        
        return contour
    }

    private fun calculateContourArea(contour: List<PointF>): Double {
        if (contour.size < 3) return 0.0
        
        // Shoelace formula for polygon area
        var area = 0.0
        val n = contour.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += contour[i].x * contour[j].y
            area -= contour[j].x * contour[i].y
        }
        return abs(area) / 2.0
    }

    private fun simplifyContour(contour: List<PointF>, epsilon: Double): List<PointF> {
        if (contour.size < 3) return contour
        
        // Find point with maximum distance from line between first and last
        var maxDistance = 0.0
        var maxIndex = 0
        
        val first = contour.first()
        val last = contour.last()
        
        for (i in 1 until contour.size - 1) {
            val distance = perpendicularDistance(contour[i], first, last)
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }
        
        return if (maxDistance > epsilon) {
            val left = simplifyContour(contour.subList(0, maxIndex + 1), epsilon)
            val right = simplifyContour(contour.subList(maxIndex, contour.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Double {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        
        if (dx == 0f && dy == 0f) {
            return hypot((point.x - lineStart.x).toDouble(), (point.y - lineStart.y).toDouble())
        }
        
        val num = abs(dy * point.x - dx * point.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x)
        val den = hypot(dx.toDouble(), dy.toDouble())
        
        return num / den
    }

    private fun findFourCorners(contour: List<PointF>, width: Int, height: Int): List<PointF>? {
        if (contour.size < 4) return null
        
        // Find corners as points closest to image corners
        val imageCorners = listOf(
            PointF(0f, 0f),           // top-left
            PointF(width.toFloat(), 0f),    // top-right
            PointF(width.toFloat(), height.toFloat()), // bottom-right
            PointF(0f, height.toFloat())    // bottom-left
        )
        
        val corners = mutableListOf<PointF>()
        
        for (imageCorner in imageCorners) {
            var minDist = Float.MAX_VALUE
            var closest = contour.first()
            
            for (point in contour) {
                val dist = hypot(
                    (point.x - imageCorner.x).toDouble(),
                    (point.y - imageCorner.y).toDouble()
                ).toFloat()
                
                if (dist < minDist) {
                    minDist = dist
                    closest = point
                }
            }
            corners.add(closest)
        }
        
        // Validate that corners form a reasonable quadrilateral
        // Corners should be in different quadrants
        val centerX = width / 2f
        val centerY = height / 2f
        
        val topLeft = corners[0]
        val topRight = corners[1]
        val bottomRight = corners[2]
        val bottomLeft = corners[3]
        
        // Basic validation: corners should be roughly in expected positions
        if (topLeft.x > centerX || topLeft.y > centerY) return null
        if (topRight.x < centerX || topRight.y > centerY) return null
        if (bottomRight.x < centerX || bottomRight.y < centerY) return null
        if (bottomLeft.x > centerX || bottomLeft.y < centerY) return null
        
        return corners
    }


    private fun detectLines(points: List<PointF>, width: Int, height: Int): List<Line> {
        // Simplified Hough transform
        @Suppress("UNUSED_VARIABLE")
        val diagonal = hypot(width.toDouble(), height.toDouble()).toInt()
        val thetaStep = Math.PI / 180

        val accumulator = mutableMapOf<Pair<Int, Int>, Int>()

        for (point in points) {
            for (thetaIndex in 0 until 180) {
                val theta = thetaIndex * thetaStep
                val rho = (point.x * cos(theta) + point.y * sin(theta)).toInt()
                val key = Pair(rho, thetaIndex)
                accumulator[key] = (accumulator[key] ?: 0) + 1
            }
        }

        // Find peaks in accumulator
        val threshold = points.size / 50
        val lines = accumulator.filter { it.value > threshold }
            .entries
            .sortedByDescending { it.value }
            .take(20)
            .map { entry ->
                val (rho, thetaIndex) = entry.key
                val theta = thetaIndex * thetaStep
                Line(rho.toFloat(), theta.toFloat())
            }

        return lines
    }

    private data class Line(val rho: Float, val theta: Float)

    private fun findQuadrilateralCorners(lines: List<Line>, width: Int, height: Int): DetectedCorners? {
        // Group lines by orientation (horizontal vs vertical)
        val horizontalLines = lines.filter { 
            val angle = Math.toDegrees(it.theta.toDouble())
            angle in 60.0..120.0 || angle in -120.0..-60.0
        }
        val verticalLines = lines.filter { 
            val angle = Math.toDegrees(it.theta.toDouble())
            angle in -30.0..30.0 || angle > 150 || angle < -150
        }

        if (horizontalLines.size < 2 || verticalLines.size < 2) {
            return null
        }

        // Get top/bottom horizontal lines and left/right vertical lines
        val sortedH = horizontalLines.sortedBy { 
            val y = (it.rho / sin(it.theta)).coerceIn(0f, height.toFloat())
            y
        }
        val sortedV = verticalLines.sortedBy {
            val x = (it.rho / cos(it.theta)).coerceIn(0f, width.toFloat())
            x
        }

        val topLine = sortedH.firstOrNull() ?: return null
        val bottomLine = sortedH.lastOrNull() ?: return null
        val leftLine = sortedV.firstOrNull() ?: return null
        val rightLine = sortedV.lastOrNull() ?: return null

        // Find intersections
        val topLeft = intersectLines(topLine, leftLine) ?: return null
        val topRight = intersectLines(topLine, rightLine) ?: return null
        val bottomLeft = intersectLines(bottomLine, leftLine) ?: return null
        val bottomRight = intersectLines(bottomLine, rightLine) ?: return null

        // Validate corners are within image bounds
        val corners = listOf(topLeft, topRight, bottomLeft, bottomRight)
        for (corner in corners) {
            if (corner.x < 0 || corner.x > width || corner.y < 0 || corner.y > height) {
                return null
            }
        }

        return DetectedCorners(
            topLeft = topLeft,
            topRight = topRight,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight,
            confidence = 0.8f
        )
    }

    private fun intersectLines(line1: Line, line2: Line): PointF? {
        val cos1 = cos(line1.theta.toDouble())
        val sin1 = sin(line1.theta.toDouble())
        val cos2 = cos(line2.theta.toDouble())
        val sin2 = sin(line2.theta.toDouble())

        val det = cos1 * sin2 - sin1 * cos2
        if (abs(det) < 1e-6) {
            return null // Parallel lines
        }

        val x = ((line1.rho * sin2 - line2.rho * sin1) / det).toFloat()
        val y = ((line2.rho * cos1 - line1.rho * cos2) / det).toFloat()

        return PointF(x, y)
    }

    private fun findConvexHullCorners(points: List<PointF>, width: Int, height: Int): DetectedCorners? {
        if (points.isEmpty()) {
            return createDefaultCorners(width, height)
        }

        // Find bounding box corners from edge points
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (point in points) {
            if (point.x < minX) minX = point.x
            if (point.x > maxX) maxX = point.x
            if (point.y < minY) minY = point.y
            if (point.y > maxY) maxY = point.y
        }

        // Add some margin
        val marginX = (maxX - minX) * 0.02f
        val marginY = (maxY - minY) * 0.02f

        minX = (minX + marginX).coerceAtLeast(0f)
        maxX = (maxX - marginX).coerceAtMost(width.toFloat())
        minY = (minY + marginY).coerceAtLeast(0f)
        maxY = (maxY - marginY).coerceAtMost(height.toFloat())

        return DetectedCorners(
            topLeft = PointF(minX, minY),
            topRight = PointF(maxX, minY),
            bottomLeft = PointF(minX, maxY),
            bottomRight = PointF(maxX, maxY),
            confidence = 0.5f
        )
    }

    private fun createDefaultCorners(width: Int, height: Int): DetectedCorners {
        // Default to 5% margin on each side
        val marginX = width * 0.05f
        val marginY = height * 0.05f

        return DetectedCorners(
            topLeft = PointF(marginX, marginY),
            topRight = PointF(width - marginX, marginY),
            bottomLeft = PointF(marginX, height - marginY),
            bottomRight = PointF(width - marginX, height - marginY),
            confidence = 0.3f
        )
    }
}
