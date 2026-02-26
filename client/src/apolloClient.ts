import {
  ApolloClient,
  createHttpLink,
  InMemoryCache,
} from '@apollo/client/core'

const endpoint =
  import.meta.env.VITE_GRAPHQL_ENDPOINT ||
  `${window.location.protocol}//${window.location.hostname}:8080/graphql`

const httpLink = createHttpLink({
  uri: endpoint,
})

const apolloClient = new ApolloClient({
  link: httpLink,
  cache: new InMemoryCache(),
})

export default { apolloClient }
export { apolloClient }
