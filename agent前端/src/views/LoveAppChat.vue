<template>
  <ChatWindow
    title="AI 恋爱大师"
    avatar="💕"
    :sseApi="chatApi"
  />
</template>

<script setup>
import ChatWindow from '../components/ChatWindow.vue'
import { chatWithLoveAppSse } from '../api/chat.js'

function chatApi(message, chatId, onChunk, onDone, onError) {
  chatWithLoveAppSse(message, chatId, onChunk, onDone, onError)
}
</script>
