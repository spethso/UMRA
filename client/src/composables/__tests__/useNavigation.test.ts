import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useNavigation } from '../useNavigation'

describe('useNavigation', () => {
  beforeEach(() => {
    // Reset location to '/' before each test
    window.history.pushState({}, '', '/')
  })

  it('currentPath initializes from window.location.pathname', () => {
    window.history.pushState({}, '', '/some-path')
    const { currentPath } = useNavigation()
    expect(currentPath.value).toBe('/some-path')
  })

  it('navigateTo updates currentPath', () => {
    const { currentPath, navigateTo } = useNavigation()
    navigateTo('/new-path')
    expect(currentPath.value).toBe('/new-path')
  })

  it('showGraphiql is true when path is /graphiql', () => {
    window.history.pushState({}, '', '/graphiql')
    const { showGraphiql } = useNavigation()
    expect(showGraphiql.value).toBe(true)
  })

  it('showGraphiql is false for other paths', () => {
    window.history.pushState({}, '', '/')
    const { showGraphiql } = useNavigation()
    expect(showGraphiql.value).toBe(false)
  })

  it('showLegalPage is true for /imprint', () => {
    window.history.pushState({}, '', '/imprint')
    const { showLegalPage } = useNavigation()
    expect(showLegalPage.value).toBe(true)
  })

  it('showLegalPage is true for /privacy', () => {
    window.history.pushState({}, '', '/privacy')
    const { showLegalPage } = useNavigation()
    expect(showLegalPage.value).toBe(true)
  })

  it('showLegalPage is true for /contact', () => {
    window.history.pushState({}, '', '/contact')
    const { showLegalPage } = useNavigation()
    expect(showLegalPage.value).toBe(true)
  })

  it('showLegalPage is false for non-legal paths', () => {
    window.history.pushState({}, '', '/about')
    const { showLegalPage } = useNavigation()
    expect(showLegalPage.value).toBe(false)
  })
})
