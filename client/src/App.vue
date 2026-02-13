<script setup>
import { computed, ref } from 'vue'
import { useLazyQuery, useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'

const HELLO_QUERY = gql`
  query Hello {
    hello
  }
`

const TEXTS_QUERY = gql`
  query Texts {
    texts
  }
`

const ADD_TEXT_MUTATION = gql`
  mutation AddText($text: String!) {
    addText(text: $text)
  }
`

const { result, loading, error } = useQuery(HELLO_QUERY)
const {
  result: textsResult,
  loading: textsLoading,
  error: textsError,
  load: loadTexts,
  refetch: refetchTexts,
} = useLazyQuery(TEXTS_QUERY)
const { mutate: addTextMutate, loading: addLoading, error: addError } = useMutation(ADD_TEXT_MUTATION)

const helloMessage = computed(() => result.value?.hello ?? '')
const texts = computed(() => textsResult.value?.texts ?? [])
const inputText = ref('')

const storeText = async () => {
  const trimmedText = inputText.value.trim()
  if (!trimmedText) {
    return
  }

  await addTextMutate({ text: trimmedText })
  inputText.value = ''
}

const fetchTexts = async () => {
  if (textsResult.value) {
    await refetchTexts()
    return
  }

  await loadTexts()
}
</script>

<template>
  <main>
    <h1>Hello World App</h1>

    <p v-if="loading">Loading greeting from GraphQL...</p>
    <p v-else-if="error">Error: {{ error.message }}</p>
    <p v-else>{{ helloMessage }}</p>

    <section>
      <input
        v-model="inputText"
        type="text"
        placeholder="Enter text"
      />
      <button :disabled="addLoading" @click="storeText">
        {{ addLoading ? 'Storing...' : 'Store text' }}
      </button>
      <p v-if="addError">Error storing text: {{ addError.message }}</p>
    </section>

    <section>
      <button :disabled="textsLoading" @click="fetchTexts">
        {{ textsLoading ? 'Loading...' : 'Retrieve all texts' }}
      </button>

      <p v-if="textsError">Error loading texts: {{ textsError.message }}</p>
      <ul v-else>
        <li v-for="(text, index) in texts" :key="`${index}-${text}`">
          {{ text }}
        </li>
      </ul>
    </section>
  </main>
</template>

<style scoped>
main {
  max-width: 700px;
  margin: 3rem auto;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  line-height: 1.5;
}

h1 {
  margin-bottom: 1rem;
}

section {
  margin-top: 1.25rem;
}

input {
  width: 100%;
  max-width: 360px;
  padding: 0.5rem;
  margin-right: 0.5rem;
}

button {
  padding: 0.5rem 0.75rem;
  margin-top: 0.5rem;
  cursor: pointer;
}

ul {
  margin-top: 0.75rem;
  padding-left: 1.2rem;
}
</style>
