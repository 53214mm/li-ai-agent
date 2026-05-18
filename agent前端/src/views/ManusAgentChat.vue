<template>
  <div class="chat-window">
    <header class="chat-header">
      <router-link to="/" class="back-btn">← 返回</router-link>
      <span class="chat-title">AI 超级智能体</span>
    </header>

    <div class="messages" ref="msgContainer">
      <div v-if="messages.length === 0 && !streaming" class="empty-hint">
        🧠 你好！我是 LiManus，可以调用搜索、文件、PDF、终端等工具帮你完成任务。
      </div>

      <template v-for="(msg, idx) in messages" :key="idx">
        <!-- 用户消息 -->
        <div v-if="msg.role === 'user'" class="message-row msg-user">
          <div class="avatar">👤</div>
          <div class="bubble">
            <div class="user-msg">{{ msg.content }}</div>
            <div v-if="msg.file" class="file-attach">📎 {{ msg.file }}</div>
          </div>
        </div>

        <!-- 思考过程折叠区 -->
        <details v-else-if="msg.type === 'think-group'" class="think-group" :open="false">
          <summary class="think-group-summary">
            🧠 思考过程（{{ msg.steps.length }} 步）
          </summary>
          <div v-for="(step, si) in msg.steps" :key="si" class="think-step">
            <details class="step-detail" :open="false">
              <summary class="step-summary">{{ step.title }}</summary>
              <div class="step-body md-content" v-html="renderMd(step.content)"></div>
            </details>
          </div>
        </details>

        <!-- AI 最终回复：Markdown 渲染 -->
        <div v-else class="message-row msg-ai">
          <div class="avatar">🧠</div>
          <div class="bubble">
            <div class="md-content" v-html="renderMd(msg.content)"></div>
          </div>
        </div>
      </template>

      <!-- 流式输出中 -->
      <div v-if="streaming" class="message-row msg-ai">
        <div class="avatar">🧠</div>
        <div class="bubble">
          <div v-if="currentThink" class="md-content" v-html="renderMd(currentThink)"></div>
          <div v-else>思考中<span class="cursor">...</span></div>
        </div>
      </div>
    </div>

    <footer class="input-area">
      <div v-if="attachedFile" class="file-tag">
        📎 {{ attachedFile.name }}
        <span class="file-remove" @click="removeFile">✕</span>
      </div>
      <button class="file-btn" @click="pickFile" title="发送文件">📎</button>
      <input type="file" ref="fileInput" @change="onFileSelected" style="display:none" />
      <input
        v-model="inputText"
        @keydown.enter="send"
        :disabled="streaming"
        :placeholder="streaming ? 'AI 正在思考...' : '输入任务，按 Enter 发送'"
        class="input-box"
        ref="inputRef"
      />
      <button @click="send" :disabled="streaming || (!inputText.trim() && !attachedFile)" class="send-btn">发送</button>
    </footer>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { chatWithManusSse } from '../api/chat.js'
import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true })
function renderMd(text) {
  if (!text) return ''
  try {
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

const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const currentThink = ref('')
const msgContainer = ref(null)
const inputRef = ref(null)
const fileInput = ref(null)
const attachedFile = ref(null)
const chatId = crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36)

let pendingSteps = []
let hasFinal = false

function pickFile() { fileInput.value?.click() }
function onFileSelected(e) { const f = e.target.files[0]; if (f) attachedFile.value = f; e.target.value = '' }
function removeFile() { attachedFile.value = null }

function scrollToBottom() {
  nextTick(() => { const el = msgContainer.value; if (el) el.scrollTop = el.scrollHeight })
}

async function send() {
  const text = inputText.value.trim()
  const file = attachedFile.value
  if ((!text && !file) || streaming.value) return

  let displayText = text
  let sendText = text
  if (file) {
    const fc = await file.text()
    displayText = text || '(发送文件)'
    sendText = text ? text + '\n\n[文件: ' + file.name + ']\n' + fc : '[文件: ' + file.name + ']\n' + fc
  }

  messages.value.push({ role: 'user', content: displayText, file: file?.name })
  inputText.value = ''
  attachedFile.value = null
  pendingSteps = []
  hasFinal = false
  scrollToBottom()

  streaming.value = true
  currentThink.value = ''

  chatWithManusSse(sendText, chatId,
    (chunk) => {
      if (chunk.startsWith('[思考]')) {
        const content = chunk.substring(4).trim()
        const thinkMatch = content.match(/^Step (\d+) 思考: (.+)/)
        if (thinkMatch) {
          pendingSteps.push({ title: 'Step ' + thinkMatch[1] + ' 思考', content: thinkMatch[2] })
        } else {
          const colonIdx = content.indexOf(':')
          const title = colonIdx > 0 ? content.substring(0, colonIdx) : content
          const body = colonIdx > 0 ? content.substring(colonIdx + 1).trim() : ''
          pendingSteps.push({ title, content: body })
        }
      } else if (chunk.startsWith('[回复]')) {
        const content = chunk.substring(4).trim()
        const colonIdx = content.indexOf(':')
        let finalText = colonIdx > 0 ? content.substring(colonIdx + 1).trim() : content
        finalText = finalText.replace(/^Step \d+: /, '')
        currentThink.value = finalText
        hasFinal = true
      } else {
        currentThink.value += chunk
      }
      scrollToBottom()
    },
    () => {
      if (pendingSteps.length > 0) {
        messages.value.push({ type: 'think-group', steps: [...pendingSteps] })
        pendingSteps = []
      }
      if (currentThink.value.trim()) {
        messages.value.push({ role: 'ai', content: currentThink.value.trim() })
      } else if (!hasFinal && pendingSteps.length === 0) {
        messages.value.push({ role: 'ai', content: '任务已完成。' })
      }
      currentThink.value = ''
      streaming.value = false
      scrollToBottom()
    },
    (err) => {
      if (pendingSteps.length > 0) {
        messages.value.push({ type: 'think-group', steps: [...pendingSteps] })
        pendingSteps = []
      }
      messages.value.push({ role: 'ai', content: '⚠️ 请求失败: ' + (err.message || '未知错误') })
      currentThink.value = ''
      streaming.value = false
    }
  )
}
</script>

<style scoped>
.chat-window { display: flex; flex-direction: column; height: 100vh; background: #f0f2f5; }
.chat-header { display: flex; align-items: center; padding: 12px 20px;
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; gap: 16px; flex-shrink: 0; }
.back-btn { color: #fff; text-decoration: none; font-size: 16px; }
.chat-title { font-size: 18px; font-weight: 600; flex: 1; }

.messages { flex: 1; overflow-y: auto; padding: 20px; }
.empty-hint { text-align: center; color: #999; margin-top: 40px; font-size: 16px; }

.message-row { display: flex; gap: 10px; margin-bottom: 16px; align-items: flex-start; }
.msg-user { flex-direction: row-reverse; }
.avatar { width: 40px; height: 40px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
  flex-shrink: 0; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.bubble { max-width: 75%; min-width: 60px; padding: 12px 16px; border-radius: 12px; font-size: 15px;
  line-height: 1.6; word-break: break-word; }
.msg-user .bubble { background: #667eea; color: #fff; border-bottom-right-radius: 4px; }
.msg-ai .bubble { background: #fff; color: #333; border-bottom-left-radius: 4px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.user-msg { white-space: pre-wrap; }
.file-attach { margin-top: 6px; font-size: 12px; opacity: 0.8; }
.streaming .cursor { animation: blink 0.8s infinite; }
@keyframes blink { 0%,50% { opacity: 1; } 51%,100% { opacity: 0; } }

/* Markdown */
.md-content :deep(h1), .md-content :deep(h2), .md-content :deep(h3) { margin: 10px 0 6px; font-weight: 600; }
.md-content :deep(h1) { font-size: 1.4em; } .md-content :deep(h2) { font-size: 1.2em; } .md-content :deep(h3) { font-size: 1.05em; }
.md-content :deep(p) { margin: 0 0 8px; }
.md-content :deep(ul), .md-content :deep(ol) { padding-left: 20px; margin: 6px 0; }
.md-content :deep(li) { margin: 3px 0; }
.md-content :deep(code) { background: #f0f0f0; padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: 'Consolas','Courier New',monospace; }
.md-content :deep(pre) { background: #2d2d2d; color: #e0e0e0; padding: 12px 16px; border-radius: 8px; overflow-x: auto; margin: 8px 0; font-size: 13px; line-height: 1.5; }
.md-content :deep(pre code) { background: none; padding: 0; color: inherit; }
.md-content :deep(blockquote) { border-left: 3px solid #667eea; padding: 6px 12px; margin: 8px 0; color: #666; background: #f8f8ff; border-radius: 0 6px 6px 0; }
.md-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 14px; }
.md-content :deep(th), .md-content :deep(td) { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
.md-content :deep(th) { background: #f0eeff; font-weight: 600; }
.md-content :deep(strong) { font-weight: 600; }
.md-content :deep(a) { color: #667eea; }

/* 思考过程 */
.think-group { margin: 8px 0 16px 0; }
.think-group-summary { font-size: 14px; font-weight: 600; color: #667eea; cursor: pointer;
  padding: 8px 12px; background: #f0eeff; border-radius: 8px; user-select: none; }
.think-group-summary:hover { background: #e4e0ff; }
.think-step { margin: 4px 0 4px 16px; }
.step-summary { font-size: 13px; color: #888; cursor: pointer; padding: 5px 10px;
  background: #f8f8f8; border-radius: 6px; user-select: none; }
.step-summary:hover { color: #667eea; }
.step-body { margin-top: 4px; padding: 8px 12px; background: #fafafa; border-radius: 6px;
  font-size: 13px; color: #555; line-height: 1.5; white-space: pre-wrap; max-height: 200px; overflow-y: auto; }

/* 输入区 */
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
