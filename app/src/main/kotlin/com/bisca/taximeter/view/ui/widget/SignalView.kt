package com.bisca.taximeter.view.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.bisca.taximeter.R

class SignalView : View {

  private val paint: Paint
  private val borderPaint: Paint
  private val borderWidth: Float
  private val startColor: Int
  private val endColor: Int

  private var percentage: Int

  @JvmOverloads
  constructor(
      context: Context,
      attributeSet: AttributeSet? = null,
      defAttr: Int = 0
  ) : super(context, attributeSet, defAttr) {

    val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.SignalView, defAttr, 0)

    startColor = ContextCompat.getColor(context, R.color.redLed)
    endColor = ContextCompat.getColor(context, R.color.greenLed)

    paint = Paint()
    paint.style = Paint.Style.FILL
    paint.isAntiAlias = true
    paint.color = endColor

    borderWidth = typedArray.getDimension(R.styleable.SignalView_borderWidth, 10f)
    percentage = typedArray.getInt(R.styleable.SignalView_percentage, 100)

    borderPaint = Paint()
    borderPaint.isAntiAlias = true
    borderPaint.style = Paint.Style.STROKE
    borderPaint.strokeWidth = borderWidth
    borderPaint.color = ContextCompat.getColor(
        context,
        typedArray.getColor(
            R.styleable.SignalView_borderColor,
            android.R.color.darker_gray
        )
    )
    typedArray.recycle()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = getMeasurement(widthMeasureSpec, 150)
    val height = getMeasurement(heightMeasureSpec, 100)
    setMeasuredDimension(width, height)
  }

  override fun onDraw(canvas: Canvas?) {
    canvas?.let {
      drawBorder(it)
      drawSignal(canvas)
    }
  }

  fun setSignalPercentage(percentage: Int) {
    this.percentage = when {
      percentage > 100 -> 100
      percentage < 20 -> 20
      else -> percentage
    }
    postInvalidate()
  }

  private fun drawSignal(canvas: Canvas) {
    val percentage = Math.min(percentage, 100)
    if (percentage < 10) return

    paint.color = blendColors(startColor, endColor, percentage / 100f)

    val halfBorderWidth = (borderWidth / 2)

    val partialWidth = ((width - halfBorderWidth) * percentage) / 100f
    val partialHeight = ((height - borderWidth) * percentage) / 100f

    val path = Path()
    path.moveTo(partialWidth, height - halfBorderWidth)
    path.lineTo(halfBorderWidth, height - halfBorderWidth)
    path.lineTo(partialWidth, height - partialHeight)
    path.close()

    canvas.drawPath(path, paint)
  }

  private fun drawBorder(canvas: Canvas) {
    val path = Path()
    val halfBorderWidth = (borderWidth / 2)

    path.moveTo(width - halfBorderWidth, height - halfBorderWidth)
    path.lineTo(halfBorderWidth, height - halfBorderWidth)
    path.lineTo(width - halfBorderWidth, halfBorderWidth)
    path.close()

    canvas.drawPath(path, borderPaint)
  }

  private fun getMeasurement(measureSpec: Int, preferred: Int): Int {
    val specSize = View.MeasureSpec.getSize(measureSpec)

    return when (View.MeasureSpec.getMode(measureSpec)) {
      View.MeasureSpec.EXACTLY -> specSize
      View.MeasureSpec.AT_MOST -> Math.min(preferred, specSize)
      else -> preferred
    }
  }

  private fun blendColors(startColor: Int, endColor: Int, fraction: Float): Int {
    val inverseRatio = 1.0F - fraction;

    val a = Color.alpha(startColor) * inverseRatio + Color.alpha(endColor) * fraction;
    val r = Color.red(startColor) * inverseRatio + Color.red(endColor) * fraction;
    val g = Color.green(startColor) * inverseRatio + Color.green(endColor) * fraction;
    val b = Color.blue(startColor) * inverseRatio + Color.blue(endColor) * fraction;

    return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt());
  }
}