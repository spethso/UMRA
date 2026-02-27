/**
 * Composable that manages legal / informational page content
 * (imprint, privacy statement, contact details).
 *
 * Markdown source files are fetched from `public/config/` at runtime and
 * rendered to HTML via `markdown-it`.
 *
 * @module composables/useLegalPages
 */

import MarkdownIt from 'markdown-it'
import { computed, ref, type ComputedRef, type Ref } from 'vue'
import type { FooterHtml } from '../types/risk'
import { logDebug, logWarn } from '../utils/logger'

/** Discriminated key for the three legal pages. */
type LegalPageKey = 'imprint' | 'privacy' | 'contact'

/** Shape returned by {@link useLegalPages}. */
export interface UseLegalPagesReturn {
  /** Mapping from pathname to legal-page key. */
  readonly legalPathToKey: Readonly<Record<string, LegalPageKey>>
  /** The current legal-page key, or `null` when on a non-legal route. */
  readonly legalPageKey: ComputedRef<LegalPageKey | null>
  /** Human-readable title for the current legal page. */
  readonly legalPageTitle: ComputedRef<string>
  /** Rendered HTML content for the current legal page. */
  readonly legalPageContent: ComputedRef<string>
  /** Raw rendered HTML for every legal page (keyed by page name). */
  readonly footerHtml: Ref<FooterHtml>
  /** Fetch and render all legal-page markdown files. */
  readonly loadFooterMarkdown: () => Promise<void>
}

const CTX = 'LegalPages'

/**
 * Provides reactive legal-page state and a loader for the markdown content.
 *
 * @param currentPath - A reactive ref holding the current normalised pathname.
 *                      Must be a `Readonly<Ref<string>>` so the composable
 *                      cannot accidentally mutate the navigation state.
 * @returns Reactive computed values and a markdown loader function.
 */
export function useLegalPages(currentPath: Readonly<Ref<string>>): UseLegalPagesReturn {
  const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })

  const legalPathToKey: Readonly<Record<string, LegalPageKey>> = {
    '/imprint': 'imprint',
    '/privacy': 'privacy',
    '/contact': 'contact',
  }

  /** Resolved legal-page key for the active route (or `null`). */
  const legalPageKey = computed<LegalPageKey | null>(() => legalPathToKey[currentPath.value] ?? null)

  /** Human-readable title for the active legal page. */
  const legalPageTitle = computed<string>(() => {
    switch (legalPageKey.value) {
      case 'imprint':
        return 'Imprint'
      case 'privacy':
        return 'Privacy statement'
      case 'contact':
        return 'Contact details'
      default:
        return ''
    }
  })

  const footerHtml = ref<FooterHtml>({
    imprint: '',
    privacy: '',
    contact: '',
  })

  /** Rendered HTML for the current legal page. */
  const legalPageContent = computed(() => (legalPageKey.value ? footerHtml.value[legalPageKey.value] : ''))

  const markdownConfigBasePath = `${import.meta.env.BASE_URL}config/`

  /**
   * Fetch the three markdown files (`imprint.md`, `privacy.md`, `contact.md`)
   * from the public config directory, render them to HTML, and store the
   * result in {@link footerHtml}.
   */
  const loadFooterMarkdown = async (): Promise<void> => {
    logDebug(CTX, 'Loading footer markdown files')

    const [imprint, privacy, contact] = await Promise.all([
      fetch(`${markdownConfigBasePath}imprint.md`),
      fetch(`${markdownConfigBasePath}privacy.md`),
      fetch(`${markdownConfigBasePath}contact.md`),
    ])

    const fallback = 'Content currently unavailable.'

    const [imprintText, privacyText, contactText] = await Promise.all([
      imprint.ok ? imprint.text() : Promise.resolve(fallback),
      privacy.ok ? privacy.text() : Promise.resolve(fallback),
      contact.ok ? contact.text() : Promise.resolve(fallback),
    ])

    if (!imprint.ok || !privacy.ok || !contact.ok) {
      logWarn(CTX, 'One or more legal-page markdown files could not be loaded')
    }

    footerHtml.value = {
      imprint: markdown.render(imprintText),
      privacy: markdown.render(privacyText),
      contact: markdown.render(contactText),
    }
  }

  return {
    legalPathToKey,
    legalPageKey,
    legalPageTitle,
    legalPageContent,
    footerHtml,
    loadFooterMarkdown,
  }
}
