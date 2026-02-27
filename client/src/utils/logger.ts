/**
 * Structured logging utility for the UMRA client application.
 *
 * Wraps the `loglevel` library and prefixes every message with `[UMRA]`
 * and a caller-supplied context tag.
 *
 * The active log level defaults to `debug` during development and
 * `info` in production builds.
 *
 * @module logger
 */

import log from 'loglevel'

/* Set the threshold once at module load. */
log.setLevel(import.meta.env.DEV ? 'debug' : 'info')

/** Formats the common `[UMRA][context]` prefix. */
function formatPrefix(context: string): string {
  return `[UMRA][${context}]`
}

/**
 * Log a **debug**-level message.
 *
 * Debug messages are only emitted in development mode.
 *
 * @param context - Short tag identifying the calling module or function
 *                  (e.g. `'SessionManager'`).
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logDebug(context: string, message: string, data?: unknown): void {
  data !== undefined
    ? log.debug(formatPrefix(context), message, data)
    : log.debug(formatPrefix(context), message)
}

/**
 * Log an **info**-level message.
 *
 * @param context - Short tag identifying the calling module or function.
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logInfo(context: string, message: string, data?: unknown): void {
  data !== undefined
    ? log.info(formatPrefix(context), message, data)
    : log.info(formatPrefix(context), message)
}

/**
 * Log a **warn**-level message.
 *
 * @param context - Short tag identifying the calling module or function.
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logWarn(context: string, message: string, data?: unknown): void {
  data !== undefined
    ? log.warn(formatPrefix(context), message, data)
    : log.warn(formatPrefix(context), message)
}

/**
 * Log an **error**-level message.
 *
 * @param context - Short tag identifying the calling module or function.
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logError(context: string, message: string, data?: unknown): void {
  data !== undefined
    ? log.error(formatPrefix(context), message, data)
    : log.error(formatPrefix(context), message)
}
