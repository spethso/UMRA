/**
 * Manages application routing state and navigation helpers.
 *
 * @module composables/useNavigation
 */

import { computed, ref, type ComputedRef, type Ref } from 'vue'

/** Shape returned by {@link useNavigation}. */
export interface UseNavigationReturn {
  /** The current pathname, normalised (trailing slashes stripped, defaults to `'/'`). */
  readonly currentPath: Ref<string>
  /** Sync `currentPath` with the browser's current `window.location.pathname`. */
  readonly updateCurrentPath: () => void
  /** Push a new path via the History API and update reactive state. */
  readonly navigateTo: (path: string) => void
  /** `true` when the current path is `/graphiql`. */
  readonly showGraphiql: ComputedRef<boolean>
  /** `true` when the current path matches a known legal-page route. */
  readonly showLegalPage: ComputedRef<boolean>
}

/** Set of pathname prefixes recognised as legal / informational pages. */
const LEGAL_PATHS: ReadonlySet<string> = new Set(['/imprint', '/privacy', '/contact'])

/**
 * Provides reactive navigation state and imperative helpers for the UMRA
 * single-page application.
 *
 * @returns An object containing reactive refs and navigation functions.
 */
export function useNavigation(): UseNavigationReturn {
  const currentPath = ref(window.location.pathname.replace(/\/+$/, '') || '/')

  const showGraphiql = computed(() => currentPath.value === '/graphiql')
  const showLegalPage = computed(() => LEGAL_PATHS.has(currentPath.value))

  /** Re-read the browser URL and update `currentPath`. Called on `popstate`. */
  const updateCurrentPath = (): void => {
    currentPath.value = window.location.pathname.replace(/\/+$/, '') || '/'
  }

  /**
   * Navigate to the given path, updating the browser URL and reactive state.
   *
   * @param path - Target pathname (e.g. `'/imprint'`).
   */
  const navigateTo = (path: string): void => {
    if (window.location.pathname !== path) {
      window.history.pushState({}, '', path)
      currentPath.value = path
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  return { currentPath, updateCurrentPath, navigateTo, showGraphiql, showLegalPage }
}
