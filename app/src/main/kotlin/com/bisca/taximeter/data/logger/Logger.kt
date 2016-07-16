package com.bisca.taximeter.data.logger

object Logger {
  const val LEVEL_VERBOSE = 6
  const val LEVEL_DEBUG   = 5
  const val LEVEL_INFO    = 4
  const val LEVEL_WARN    = 3
  const val LEVEL_ERROR   = 2
  const val LEVEL_ASSERT  = 1

  lateinit var logManager: Array<LogManager>
  var initialized: Boolean = false

  fun init() {
    initImpl(arrayOf(BuiltInLogManager()))
  }

  fun initWith(logManagers: Array<LogManager>) {
    initImpl(logManagers)
  }

  fun verbose(tag: String, message: String, throwable: Throwable? = null) {
    checkIfLogIsInitialized()
    log(LEVEL_VERBOSE, tag, message, throwable)
  }

  fun debug(tag: String, message: String, throwable: Throwable? = null) {
    checkIfLogIsInitialized()
    log(LEVEL_DEBUG, tag, message, throwable)
  }

  fun info(tag: String, message: String, throwable: Throwable? = null) {
    checkIfLogIsInitialized()
    log(LEVEL_INFO, tag, message, throwable)
  }

  fun warn(tag: String, message: String, throwable: Throwable? = null) {
    checkIfLogIsInitialized()
    log(LEVEL_WARN, tag, message, throwable)
  }

  fun error(tag: String, message: String, throwable: Throwable? = null) {
    checkIfLogIsInitialized()
    log(LEVEL_ERROR, tag, message, throwable)
  }

  fun assert(tag: String, message: String, throwable: Throwable? = null) {
    checkIfLogIsInitialized()
    log(LEVEL_ASSERT, tag, message, throwable)
  }

  private fun log(level: Int, tag: String, message: String, throwable: Throwable? = null) {
    logManager.forEach {
      it.log(level, tag, message, throwable)
    }
  }

  private fun checkIfLogIsInitialized() {
    if (initialized.not()) {
      throw IllegalStateException("Logger is not initialized")
    }
  }

  private fun initImpl(logManager: Array<LogManager>) {
    synchronized(this, {
      if (initialized) {
        throw IllegalStateException("Logger already initialized")
      }

      this.logManager = logManager
      this.initialized = true
    })
  }
}