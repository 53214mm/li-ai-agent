import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000
})

// ==================== 恋爱大师 SSE（Flux<String>） ====================
export function chatWithLoveAppSse(message, chatId, onChunk, onDone, onError) {
  const url = `/api/ai/love_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`

  return fetch(url)
    .then(response => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      function read() {
        reader.read().then(({ done, value }) => {
          if (done) { onDone(); return }
          const text = decoder.decode(value, { stream: true })
          const lines = text.split("\n")
          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              if (data) onChunk(data)
            } else if (line.trim()) {
              if (line.trim()) onChunk(line.trim())
            }
          }
          read()
        }).catch(onError)
      }
      read()
    })
    .catch(onError)
}

// ==================== 超级智能体 SSE（fetch+ReadableStream） ====================
export function chatWithManusSse(message, chatId, onChunk, onDone, onError) {
  const url = `/api/ai/manus/chat?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`

  fetch(url)
    .then(response => {
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            if (buffer.trim()) onChunk(buffer.trim())
            onDone()
            return
          }
          const text = decoder.decode(value, { stream: true })
          buffer += text
          const parts = buffer.split("\n\n")
          buffer = parts.pop()
          for (const part of parts) {
            const lines = part.split("\n")
            for (const line of lines) {
              if (line.startsWith('data:')) {
                onChunk(line.substring(5).trim())
              } else if (line.trim() && !line.startsWith('event:')) {
                onChunk(line.trim())
              }
            }
          }
          read()
        }).catch(onError)
      }
      read()
    })
    .catch(onError)
}
