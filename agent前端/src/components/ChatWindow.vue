<template>
  <div class="chat-window">
    <header class="chat-header">
      <router-link to="/" class="back-btn">← 返回</router-link>
      <span class="chat-title">{{ title }}</span>
      <span class="chat-id">会话: {{ chatId.slice(0, 8) }}</span>
    </header>

    <div class="messages" ref="msgContainer">
      <div v-if="messages.length === 0" class="empty-hint">
        👋 你好！我是 {{ title }}，有什么可以帮你的？
      </div>
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['message-row', msg.role === 'user' ? 'msg-user' : 'msg-ai']"
      >
        <div class="avatar">{{ msg.role === 'user' ? '👤' : avatar }}</div>
        <div class="bubble">
          <!-- 用户消息：纯文本（可能含文件名） -->
          <div v-if="msg.role === 'user'" class="user-msg">{{ msg.content }}</div>
          <!-- 附件预览 -->
          <div v-if="msg.file" class="file-attach">📎 {{ msg.file }}</div>
          <!-- AI 消息：Markdown 渲染 -->
          <div v-else class="md-content" v-html="renderMd(msg.content)"></div>
        </div>
      </div>

      <!-- 流式输出中 -->
      <div v-if="streaming" class="message-row msg-ai">
        <div class="avatar">{{ avatar }}</div>
        <div class="bubble">
          <div v-if="streamingContent" class="md-content" v-html="renderMd(streamingContent)"></div>
          <div v-else>思考中<span class="cursor">...</span></div>
        </div>
      </div>
    </div>

    <footer class="input-area">
      <!-- 已选文件标签 -->
      <div v-if="attachedFile" class="file-tag">
        📎 {{ attachedFile.name }}
        <span class="file-remove" @click="removeFile">✕</span>
      </div>
      <button class="file-btn" @click="pickFile" title="发送文件">
        📎
      </button>
      <input type="file" ref="fileInput" @change="onFileSelected" style="display:none" />
      <input
        v-model="inputText"
        @keydown.enter="send"
        :disabled="streaming"
        :placeholder="streaming ? 'AI 正在回复...' : '输入消息，按 Enter 发送'"
        class="input-box"
        ref="inputRef"
      />
      <button @click="send" :disabled="streaming || (!inputText.trim() && !attachedFile)" class="send-btn">发送</button>
    </footer>
  </div>
</template>

<script setup>
import { ref, nextTick, onUnmounted, defineProps } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  title: { type: String, required: true },
  avatar: { type: String, default: '🤖' },
  sseApi: { type: Function, required: true }
})

const chatId = ref(crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36))
const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const streamingContent = ref('')
const msgContainer = ref(null)
const inputRef = ref(null)
const fileInput = ref(null)
const attachedFile = ref(null)
let closeSse = null

// 配置 marked
marked.setOptions({ breaks: true, gfm: true })

function renderMd(text) {
  if (!text) return ''
  try {
    // 将文件保存路径转为 Markdown 下载链接
    let processed = text.replace(
      /G:[/\\]IDEA_NEW[/\\]ai-agent[/\\]li-ai-agent[/\\]tmp[/\\](file|download)[/\\]([^\s<>"'`,，。；;、？！()]+)/g,
      (_, dir, fname) => {
        const rel = 'tmp/' + dir + '/' + fname.replace(/\\/g, '/')
        const link = '/api/ai/file/download?path=' + encodeURIComponent(rel)
        return '  📥 [下载 ' + fname + '](' + link + ')  '
      }
    )
    return marked.parse(processed)
  } catch { return text }
}

function pickFile() { fileInput.value?.click() }

function onFileSelected(e) {
  const file = e.target.files[0]
  if (file) attachedFile.value = file
  e.target.value = ''
}

function removeFile() { attachedFile.value = null }

function scrollToBottom() {
  nextTick(() => {
    const el = msgContainer.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function send() {
  const text = inputText.value.trim()
  const file = attachedFile.value
  if ((!text && !file) || streaming.value) return

  let displayText = text
  let sendText = text

  if (file) {
    const fileContent = await file.text()
    displayText = text || '(发送文件)'
    sendText = text ? text + '\n\n[文件: ' + file.name + ']\n' + fileContent : '[文件: ' + file.name + ']\n' + fileContent
  }

  messages.value.push({ role: 'user', content: displayText, file: file?.name })
  inputText.value = ''
  attachedFile.value = null
  scrollToBottom()

  streaming.value = true
  streamingContent.value = ''

  const cleanup = props.sseApi(
    sendText,
    chatId.value,
    (chunk) => { streamingContent.value += chunk; scrollToBottom() },
    () => {
      if (streamingContent.value) {
        messages.value.push({ role: 'ai', content: streamingContent.value })
      }
      streamingContent.value = ''
      streaming.value = false
      scrollToBottom()
    },
    (err) => {
      messages.value.push({ role: 'ai', content: '⚠️ 请求失败: ' + (err.message || '未知错误') })
      streamingContent.value = ''
      streaming.value = false
    }
  )
  if (typeof cleanup === 'function') closeSse = cleanup
}

onUnmounted(() => { if (closeSse) closeSse() })
</script>

<style scoped>
.chat-window { display: flex; flex-direction: column; height: 100vh; background: #f0f2f5; }
.chat-header { display: flex; align-items: center; padding: 12px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; gap: 16px; flex-shrink: 0; }
.back-btn { color: #fff; text-decoration: none; font-size: 16px; }
.chat-title { font-size: 18px; font-weight: 600; flex: 1; }
.chat-id { font-size: 12px; opacity: 0.7; }

.messages { flex: 1; overflow-y: auto; padding: 20px; }
.empty-hint { text-align: center; color: #999; margin-top: 40px; font-size: 16px; }

.message-row { display: flex; gap: 10px; margin-bottom: 20px; align-items: flex-start; }
.msg-user { flex-direction: row-reverse; }
.avatar { width: 40px; height: 40px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
  flex-shrink: 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.bubble { max-width: 75%; min-width: 60px; padding: 12px 16px; border-radius: 12px;
  font-size: 15px; line-height: 1.6; word-break: break-word; }
.msg-user .bubble { background: #667eea; color: #fff; border-bottom-right-radius: 4px; }
.msg-ai .bubble { background: #fff; color: #333; border-bottom-left-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.user-msg { white-space: pre-wrap; }
.file-attach { margin-top: 6px; font-size: 12px; opacity: 0.8; }
.streaming .cursor { animation: blink 0.8s infinite; }
@keyframes blink { 0%,50% { opacity: 1; } 51%,100% { opacity: 0; } }

/* Markdown 样式 */
.md-content :deep(h1), .md-content :deep(h2), .md-content :deep(h3) { margin: 10px 0 6px; font-weight: 600; }
.md-content :deep(h1) { font-size: 1.4em; } .md-content :deep(h2) { font-size: 1.2em; } .md-content :deep(h3) { font-size: 1.05em; }
.md-content :deep(p) { margin: 0 0 8px; }
.md-content :deep(ul), .md-content :deep(ol) { padding-left: 20px; margin: 6px 0; }
.md-content :deep(li) { margin: 3px 0; }
.md-content :deep(code) { background: #f0f0f0; padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: 'Consolas', 'Courier New', monospace; }
.md-content :deep(pre) { background: #2d2d2d; color: #e0e0e0; padding: 12px 16px; border-radius: 8px; overflow-x: auto; margin: 8px 0; font-size: 13px; line-height: 1.5; }
.md-content :deep(pre code) { background: none; padding: 0; color: inherit; }
.md-content :deep(blockquote) { border-left: 3px solid #667eea; padding: 6px 12px; margin: 8px 0; color: #666; background: #f8f8ff; border-radius: 0 6px 6px 0; }
.md-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 14px; }
.md-content :deep(th), .md-content :deep(td) { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
.md-content :deep(th) { background: #f0eeff; font-weight: 600; }
.md-content :deep(strong) { font-weight: 600; }
.md-content :deep(a) { color: #667eea; }

.input-area { display: flex; align-items: center; padding: 12px 16px; background: #fff; gap: 8px;
  border-top: 1px solid #e0e0e0; flex-shrink: 0; flex-wrap: wrap; }
.file-tag { display: flex; align-items: center; gap: 6px; padding: 4px 10px;
  background: #f0eeff; border-radius: 6px; font-size: 13px; color: #667eea; width: 100%; }
.file-remove { cursor: pointer; font-weight: bold; }
.file-btn { width: 38px; height: 38px; border: 1px solid #ddd; border-radius: 8px;
  background: #fff; font-size: 16px; cursor: pointer; flex-shrink: 0; }
.file-btn:hover { border-color: #667eea; }
.input-box { flex: 1; min-width: 120px; padding: 10px 14px; border: 1px solid #ddd; border-radius: 8px;
  font-size: 15px; outline: none; }
.input-box:focus { border-color: #667eea; }
.send-btn { padding: 10px 20px; background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; flex-shrink: 0; }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
