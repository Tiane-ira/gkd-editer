package li.gkd.studio.ui.snapshot

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import li.gkd.studio.data.model.SnapshotNode

@Composable
fun InteractiveCanvas(
    bitmap: Bitmap?,
    nodes: List<SnapshotNode>,
    selectedNode: SnapshotNode?,
    matchingNodes: List<SnapshotNode>,
    onNodeSelected: (SnapshotNode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            // Placeholder when no bitmap
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5.0f)
        offset += offsetChange
    }

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12131A))
            .transformable(state = transformState)
            .pointerInput(nodes, scale, offset) {
                detectTapGestures { tapOffset ->
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val imageWidth = bitmap.width.toFloat()
                    val imageHeight = bitmap.height.toFloat()

                    // Fit center scale factor
                    val fitScale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
                    val displayedWidth = imageWidth * fitScale
                    val displayedHeight = imageHeight * fitScale

                    val initialLeft = (canvasWidth - displayedWidth) / 2f
                    val initialTop = (canvasHeight - displayedHeight) / 2f

                    // Inverse coordinate transformation to image coordinates
                    // tapOffset = (canvasCenter + (screenCoord - canvasCenter)*scale + offset)
                    val centerX = canvasWidth / 2f
                    val centerY = canvasHeight / 2f

                    val transformedX = (tapOffset.x - centerX - offset.x) / scale + centerX
                    val transformedY = (tapOffset.y - centerY - offset.y) / scale + centerY

                    val imageX = (transformedX - initialLeft) / fitScale
                    val imageY = (transformedY - initialTop) / fitScale

                    // Hit test: find deepest / smallest node containing (imageX, imageY)
                    val hitNodes = nodes.filter { node ->
                        val attr = node.attr
                        imageX >= attr.left && imageX <= attr.right &&
                                imageY >= attr.top && imageY <= attr.bottom
                    }

                    if (hitNodes.isNotEmpty()) {
                        // Deepest node with smallest area
                        val targetNode = hitNodes.minByOrNull {
                            val area = it.attr.width * it.attr.height
                            if (area <= 0) Int.MAX_VALUE else area
                        } ?: hitNodes.maxByOrNull { it.attr.depth }!!

                        onNodeSelected(targetNode)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            val fitScale = minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
            val displayedWidth = imageWidth * fitScale
            val displayedHeight = imageHeight * fitScale

            val initialLeft = (canvasWidth - displayedWidth) / 2f
            val initialTop = (canvasHeight - displayedHeight) / 2f

            translate(offset.x, offset.y) {
                scale(scale, pivot = Offset(canvasWidth / 2f, canvasHeight / 2f)) {
                    // Draw screenshot
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = IntOffset(initialLeft.toInt(), initialTop.toInt()),
                        dstSize = IntSize(displayedWidth.toInt(), displayedHeight.toInt())
                    )

                    // Draw matching nodes (from Selector test)
                    for (node in matchingNodes) {
                        val attr = node.attr
                        val left = initialLeft + attr.left * fitScale
                        val top = initialTop + attr.top * fitScale
                        val w = attr.width * fitScale
                        val h = attr.height * fitScale

                        if (w > 0 && h > 0) {
                            // Green fill tint + border
                            drawRect(
                                color = Color(0x3310B981),
                                topLeft = Offset(left, top),
                                size = Size(w, h)
                            )
                            drawRect(
                                color = Color(0xFF10B981),
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                style = Stroke(width = 2.5f / scale)
                            )
                        }
                    }

                    // Draw selected node overlay
                    if (selectedNode != null) {
                        val attr = selectedNode.attr
                        val left = initialLeft + attr.left * fitScale
                        val top = initialTop + attr.top * fitScale
                        val w = attr.width * fitScale
                        val h = attr.height * fitScale

                        if (w > 0 && h > 0) {
                            drawRect(
                                color = Color(0x336366F1),
                                topLeft = Offset(left, top),
                                size = Size(w, h)
                            )
                            drawRect(
                                color = Color(0xFF6366F1),
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                style = Stroke(width = 3.5f / scale)
                            )
                        }
                    }
                }
            }
        }
    }
}
