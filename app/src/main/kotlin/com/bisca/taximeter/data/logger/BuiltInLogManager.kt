package com.bisca.taximeter.data.logger

import android.util.Log

class BuiltInLogManager : LogManager {
  override fun log(level: Int, tag: String, message: String, throwable: Throwable?) {
    when (level) {
      Logger.LEVEL_VERBOSE -> Log.v(tag, message, throwable)
      Logger.LEVEL_DEBUG -> Log.d(tag, message, throwable)
      Logger.LEVEL_INFO -> Log.i(tag, message, throwable)
      Logger.LEVEL_WARN -> Log.w(tag, message, throwable)
      Logger.LEVEL_ERROR -> Log.e(tag, message, throwable)
      Logger.LEVEL_ASSERT -> Log.wtf(tag, message, throwable)
    }
  }

}