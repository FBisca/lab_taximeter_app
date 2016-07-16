package com.bisca.taximeter.data.logger

interface LogManager {
  fun log(level: Int, tag: String, message: String, throwable: Throwable? = null)
}