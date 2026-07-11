package com.watering.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

// 컵 윤곽선 좌표 (70x104 기준 좌표계) — 위가 넓고 아래가 좁은 유리잔 형태.
// 물 채움은 이 좌표계의 y=CUP_FILL_TOP~CUP_FILL_BOTTOM 구간을 진행률만큼 차지하도록 clipPath로 잘라낸다
// (Glance/RemoteViews는 SVG나 Canvas 클리핑을 Composable에서 직접 지원하지 않아 비트맵으로 미리 그려서 Image로 넣음).
private const val CUP_FILL_TOP = 4f
private const val CUP_FILL_BOTTOM = 100f

private fun cupPath(sx: Float, sy: Float): Path = Path().apply {
    moveTo(9f * sx, 4f * sy)
    lineTo(61f * sx, 4f * sy)
    lineTo(54f * sx, 98f * sy)
    quadTo(54f * sx, 102f * sy, 50f * sx, 102f * sy)
    lineTo(20f * sx, 102f * sy)
    quadTo(16f * sx, 102f * sy, 16f * sx, 98f * sy)
    close()
}

fun createCupBitmap(widthPx: Int, heightPx: Int, rate: Float, fillColor: Int, outlineColor: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val sx = widthPx / 70f
    val sy = heightPx / 104f
    val path = cupPath(sx, sy)

    val fillTop = CUP_FILL_TOP * sy + (1f - rate) * (CUP_FILL_BOTTOM - CUP_FILL_TOP) * sy
    canvas.save()
    canvas.clipPath(path)
    canvas.drawRect(
        0f, fillTop, widthPx.toFloat(), heightPx.toFloat(),
        Paint().apply {
            isAntiAlias = true
            color = fillColor
            alpha = (0.85f * 255).toInt()
        }
    )
    canvas.restore()

    canvas.drawPath(
        path,
        Paint().apply {
            isAntiAlias = true
            color = outlineColor
            style = Paint.Style.STROKE
            strokeWidth = 3f * sx
        }
    )
    return bitmap
}
