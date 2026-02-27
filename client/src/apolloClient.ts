/**
 * GraphQL client used by the UMRA application.
 *
 * The endpoint is resolved from the `VITE_GRAPHQL_ENDPOINT`
 * environment variable, falling back to the current host on port 8080.
 *
 * @module apolloClient
 */

import {
  ApolloClient,
  createHttpLink,
  InMemoryCache,
} from '@apollo/client/core'

/** Resolved GraphQL endpoint URL. */
const endpoint =
  import.meta.env.VITE_GRAPHQL_ENDPOINT ||
  `${window.location.protocol}//${window.location.hostname}:8080/graphql`

const httpLink = createHttpLink({
  uri: endpoint,
})

/**
 * Shared GraphQL client instance, available for standalone queries
 * (e.g. session loading).
 */
const apolloClient = new ApolloClient({
  link: httpLink,
  cache: new InMemoryCache(),
})

export default { apolloClient }
export { apolloClient }
