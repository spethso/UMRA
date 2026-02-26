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