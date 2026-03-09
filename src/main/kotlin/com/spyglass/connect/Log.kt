package com.spyglass.connect

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Simple file + console logger for Spyglass Connect.
 * Writes to ~/.spyglass-connect/spyglass.log and to stdout.
 */
object Log {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val logFile: File
    private val writer: java.io.BufferedWriter

    init {
        val dir = File(System.getProperty("user.home"), ".spyglass-connect")
        dir.mkdirs()
        logFile = File(dir, "spyglass.log")

        // Rotate if over 2 MB
        if (logFile.exists() && logFile.length() > 2 * 1024 * 1024) {
            val old = File(dir, "spyglass.log.old")
            old.delete()
            logFile.renameTo(old)
        }

        writer = logFile.appendingWriter().buffered()
        i("Log", "--- Session started ---")
    }

    @Volatile private var pendingWrites = 0

    private fun write(level: String, tag: String, msg: String) {
        val ts = LocalDateTime.now().format(fmt)
        val line = "[$ts] $level/$tag: $msg"
        println(line)
        synchronized(writer) {
            writer.write(line)
            writer.newLine()
            pendingWrites++
            // Flush every 10 writes or on errors/warnings for timely persistence
            if (pendingWrites >= 10 || level == "E" || level == "W") {
                writer.flush()
                pendingWrites = 0
            }
        }
    }

    /** Info-level log. */
    fun i(tag: String, msg: String) = write("I", tag, msg)

    /** Warning-level log. */
    fun w(tag: String, msg: String) = write("W", tag, msg)

    /** Error-level log. */
    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        write("E", tag, msg)
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            write("E", tag, sw.toString().trimEnd())
        }
    }

    /** Debug-level log. */
    fun d(tag: String, msg: String) = write("D", tag, msg)

    /** Return the log file path (for UI display). */
    fun filePath(): String = logFile.absolutePath

    private fun File.appendingWriter() = this.outputStream().writer(Charsets.UTF_8)
}
