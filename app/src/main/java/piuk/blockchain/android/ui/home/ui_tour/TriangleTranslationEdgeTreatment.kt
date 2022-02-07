package piuk.blockchain.android.ui.home.ui_tour

import androidx.annotation.FloatRange
import androidx.annotation.Px
import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.ShapePath

/**
 * Instantiates a triangle treatment of the given size
 *
 * @param triangleHeight the length in pixels that the triangle extends out of the shape. The length
 *     of the side of the triangle coincident with the rest of the edge is 2 * size.
 * @param inset the padding in pixels that is given between the corner and the triangle start or end in case the
 *        triangle is being drawn next to a corner
 * @param translate where the triangle should be drawn, 0 is the start of the edge and 1 being the end.
 *     These values refer to the Top edge, keep in mind that the edge will be rotated when applying to different
 *     edges, meaning that when drawing the bottom edge translate = 0.0 would actually draw the triangle in the right side
 */
class TriangleTranslationEdgeTreatment(
    @Px private val triangleHeight: Float,
    @Px private val inset: Float,
    @FloatRange(from = 0.0, to = 1.0)
    private val translate: Float
) : EdgeTreatment() {
    private val triangleBaseWidth = triangleHeight * 2

    override fun getEdgePath(
        length: Float,
        center: Float,
        interpolation: Float,
        shapePath: ShapePath
    ) {
        val usableLength = length - triangleBaseWidth - inset * 2
        if (usableLength < 0) {
            // Don't draw if the edge is not long enough to fully draw the triangle + the needed insets
            shapePath.lineTo(length, 0f)
            return
        }

        val startOfTriangleX = usableLength * translate + inset
        val centerOfTriangleX = startOfTriangleX + triangleBaseWidth / 2
        val endOfTriangleX = startOfTriangleX + triangleBaseWidth
        shapePath.lineTo(startOfTriangleX, 0f)
        shapePath.lineTo(centerOfTriangleX, -triangleHeight)
        shapePath.lineTo(endOfTriangleX, 0f)
        shapePath.lineTo(length, 0f)
    }
}
