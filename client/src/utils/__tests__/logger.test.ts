import { describe, it, expect, vi, beforeEach } from 'vitest'
import log from 'loglevel'

// loglevel caches console method references at `setLevel()` time, so spies
// must be installed BEFORE calling `setLevel()` to intercept the calls.

let logDebug: typeof import('../../utils/logger').logDebug
let logInfo: typeof import('../../utils/logger').logInfo
let logWarn: typeof import('../../utils/logger').logWarn
let logError: typeof import('../../utils/logger').logError

beforeEach(async () => {
  vi.restoreAllMocks()
  const mod = await import('../../utils/logger')
  logDebug = mod.logDebug
  logInfo = mod.logInfo
  logWarn = mod.logWarn
  logError = mod.logError
})

/** Install spies and force loglevel to rebind to them. */
function spyAndRebind() {
  const spies = {
    log: vi.spyOn(console, 'log').mockImplementation(() => {}),
    info: vi.spyOn(console, 'info').mockImplementation(() => {}),
    warn: vi.spyOn(console, 'warn').mockImplementation(() => {}),
    error: vi.spyOn(console, 'error').mockImplementation(() => {}),
  }
  log.setLevel('debug')
  return spies
}

describe('logger', () => {
  it('logInfo calls console.info with [UMRA] prefix', () => {
    const spies = spyAndRebind()
    logInfo('Test', 'hello')
    expect(spies.info).toHaveBeenCalledWith('[UMRA][Test]', 'hello')
  })

  it('logWarn calls console.warn with [UMRA] prefix', () => {
    const spies = spyAndRebind()
    logWarn('Ctx', 'warning message')
    expect(spies.warn).toHaveBeenCalledWith('[UMRA][Ctx]', 'warning message')
  })

  it('logError calls console.error with [UMRA] prefix', () => {
    const spies = spyAndRebind()
    logError('Err', 'bad thing')
    expect(spies.error).toHaveBeenCalledWith('[UMRA][Err]', 'bad thing')
  })

  it('logDebug calls console.log with [UMRA] prefix', () => {
    const spies = spyAndRebind()
    logDebug('Dbg', 'trace')
    expect(spies.log).toHaveBeenCalledWith('[UMRA][Dbg]', 'trace')
  })

  it('logInfo includes optional data argument', () => {
    const spies = spyAndRebind()
    logInfo('X', 'msg', { key: 42 })
    expect(spies.info).toHaveBeenCalledWith('[UMRA][X]', 'msg', { key: 42 })
  })

  it('each function includes [UMRA] in the output', () => {
    const spies = spyAndRebind()

    logDebug('A', 'd')
    logInfo('B', 'i')
    logWarn('C', 'w')
    logError('D', 'e')

    for (const spy of [spies.log, spies.info, spies.warn, spies.error]) {
      expect(spy.mock.calls.length).toBeGreaterThan(0)
      const firstArg = spy.mock.calls[0][0] as string
      expect(firstArg).toContain('[UMRA]')
    }
  })
})
