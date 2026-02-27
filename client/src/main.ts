/**
 * Application bootstrap.
 *
 * Creates the root Vue 3 application, provides the shared Apollo Client
 * instance via {@link DefaultApolloClient}, and mounts the {@link App}
 * component to the `#app` DOM element.
 *
 * @module main
 */

import { createApp, h, provide } from 'vue'
import { DefaultApolloClient } from '@vue/apollo-composable'
import { apolloClient } from './apolloClient'
import App from './App.vue'
import '@caipira/vue-graphiql/style.css'

createApp({
  setup() {
    provide(DefaultApolloClient, apolloClient)
  },
  render: () => h(App),
}).mount('#app')