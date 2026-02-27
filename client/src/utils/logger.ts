/**
 * Structured logging utility for the UMRA client application.
 *
 * Provides leveled log functions ({@link logDebug}, {@link logInfo},
 * {@link logWarn}, {@link logError}) that prefix every message with `[UMRA]`
 * and a caller-supplied context tag.
 *
 * The active log level defaults to `'debug'` during development
 * (`import.meta.env.DEV === true`) and `'info'` in production builds.
 *
 * @module logger
 */

/** Supported log levels ordered by increasing severity. */
type LogLevel = 'debug' | 'info' | 'warn' | 'error'

/** @internal Numeric priority used for level comparison. */
const LOG_LEVEL_PRIORITY: Readonly<Record<LogLevel, number>> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
}

/**
 * Current minimum log level.
 * Messages below this severity are suppressed.
 */
const LOG_LEVEL: LogLevel = import.meta.env.DEV ? 'debug' : 'info'

/** @internal Returns `true` when the given level meets or exceeds the current threshold. */
function shouldLog(level: LogLevel): boolean {
  return LOG_LEVEL_PRIORITY[level] >= LOG_LEVEL_PRIORITY[LOG_LEVEL]
}

/** @internal Formats the common `[UMRA][context]` prefix. */
function formatPrefix(context: string): string {
  return `[UMRA][${context}]`
}

/**
 * Log a **debug**-level message.
 *
 * Debug messages are only emitted when the application runs in development
 * mode (`import.meta.env.DEV`).
 *
 * @param context - Short tag identifying the calling module or function
 *                  (e.g. `'SessionManager'`).
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logDebug(context: string, message: string, data?: unknown): void {
  if (!shouldLog('debug')) return
  data !== undefined
    ? console.debug(formatPrefix(context), message, data)
    : console.debug(formatPrefix(context), message)
}

/**
 * Log an **info**-level message.
 *
 * @param context - Short tag identifying the calling module or function.
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logInfo(context: string, message: string, data?: unknown): void {
  if (!shouldLog('info')) return
  data !== undefined
    ? console.info(formatPrefix(context), message, data)
    : console.info(formatPrefix(context), message)
}

/**
 * Log a **warn**-level message.
 *
 * @param context - Short tag identifying the calling module or function.
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logWarn(context: string, message: string, data?: unknown): void {
  if (!shouldLog('warn')) return
  data !== undefined
    ? console.warn(formatPrefix(context), message, data)
    : console.warn(formatPrefix(context), message)
}

/**
 * Log an **error**-level message.
 *
 * @param context - Short tag identifying the calling module or function.
 * @param message - Human-readable log message.
 * @param data    - Optional structured data attached to the log entry.
 */
export function logError(context: string, message: string, data?: unknown): void {
  if (!shouldLog('error')) return
  data !== undefined
    ? console.error(formatPrefix(context), message, data)
    : console.error(formatPrefix(context), message)
}
