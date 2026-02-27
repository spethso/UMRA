import { describe, it, expect, vi, beforeEach } from 'vitest'

// The logger reads import.meta.env.DEV at module scope to decide the log level.
// In vitest with jsdom the default is `true` (dev mode), so all levels pass.

// We need to dynamically import so spies are set up before calls.
let logDebug: typeof import('../../utils/logger').logDebug
let logInfo: typeof import('../../utils/logger').logInfo
let logWarn: typeof import('../../utils/logger').logWarn
let logError: typeof import('../../utils/logger').logError

beforeEach(async () => {
  vi.restoreAllMocks()
  // Re-import to get fresh module (log level already set at load time, but
  // the functions themselves are stateless so a static import works too).
  const mod = await import('../../utils/logger')
  logDebug = mod.logDebug
  logInfo = mod.logInfo
  logWarn = mod.logWarn
  logError = mod.logError
})

describe('logger', () => {
  it('logInfo calls console.info with [UMRA] prefix', () => {
    const spy = vi.spyOn(console, 'info').mockImplementation(() => {})
    logInfo('Test', 'hello')
    expect(spy).toHaveBeenCalledWith('[UMRA][Test]', 'hello')
  })

  it('logWarn calls console.warn with [UMRA] prefix', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    logWarn('Ctx', 'warning message')
    expect(spy).toHaveBeenCalledWith('[UMRA][Ctx]', 'warning message')
  })

  it('logError calls console.error with [UMRA] prefix', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    logError('Err', 'bad thing')
    expect(spy).toHaveBeenCalledWith('[UMRA][Err]', 'bad thing')
  })

  it('logDebug calls console.debug with [UMRA] prefix', () => {
    const spy = vi.spyOn(console, 'debug').mockImplementation(() => {})
    logDebug('Dbg', 'trace')
    expect(spy).toHaveBeenCalledWith('[UMRA][Dbg]', 'trace')
  })

  it('logInfo includes optional data argument', () => {
    const spy = vi.spyOn(console, 'info').mockImplementation(() => {})
    logInfo('X', 'msg', { key: 42 })
    expect(spy).toHaveBeenCalledWith('[UMRA][X]', 'msg', { key: 42 })
  })

  it('each function includes [UMRA] in the output', () => {
    const infoSpy = vi.spyOn(console, 'info').mockImplementation(() => {})
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => {})

    logInfo('A', 'i')
    logWarn('B', 'w')
    logError('C', 'e')
    logDebug('D', 'd')

    for (const spy of [infoSpy, warnSpy, errorSpy, debugSpy]) {
      const firstArg = spy.mock.calls[0][0] as string
      expect(firstArg).toContain('[UMRA]')
    }
  })
})
