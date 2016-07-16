package com.bisca.taximeter.view.ui.widget

import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.AppCompatTextView
import android.util.AttributeSet

class DigitalTextView : AppCompatTextView {

  @JvmOverloads
  constructor(
      context: Context,
      attributeSet: AttributeSet? = null,
      defAttr: Int = 0
  ) : super(context, attributeSet, defAttr) {
    if (!isInEditMode) {
      typeface = Typeface.createFromAsset(context.assets, "digital_font_new.ttf")
    }
  }

}