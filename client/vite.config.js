import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { monacoViteConfig } from '@caipira/vue-graphiql'
import $monacoEditorPlugin from 'vite-plugin-monaco-editor'

const monacoEditorPlugin = $monacoEditorPlugin.default || $monacoEditorPlugin

export default defineConfig({
  plugins: [vue(), monacoEditorPlugin(monacoViteConfig)],
  server: {
    port: 5173,
    proxy: {
      '/graphql': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
