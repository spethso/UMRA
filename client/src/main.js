import { createApp, h, provide } from 'vue'
import {
  ApolloClient,
  createHttpLink,
  InMemoryCache,
} from '@apollo/client/core'
import { DefaultApolloClient } from '@vue/apollo-composable'
import App from './App.vue'

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

createApp({
  setup() {
    provide(DefaultApolloClient, apolloClient)
  },
  render: () => h(App),
}).mount('#app')
