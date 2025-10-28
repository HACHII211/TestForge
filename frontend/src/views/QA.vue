<template>
  <div class="chat-layout">
    <!-- 左侧会话列表，宽度 15% -->
    <aside class="left-panel">
      <div class="left-card">
        <div class="left-card-header">
          <div class="title">会话列表</div>
          <div class="sub">user: {{ authUser.username || authUser.id }}</div>
        </div>

        <div class="left-list" ref="leftList">
          <div
              v-for="(item, idx) in conversationNames"
              :key="item.id || item.conversationUuid || idx"
              :class="['left-item', { active: currentConversationUuid === (item.conversationUuid || item.conversationUuid) }]"
              @click="onSelectConversation(item)"
          >
            <div class="left-item-main">
              <div class="left-item-title">{{ item.name || '未命名会话' }}</div>
              <div class="left-item-sub">{{ formatShortDate(item.createdAt) }}</div>
            </div>
          </div>

          <div v-if="conversationNames.length === 0" class="left-empty">暂无会话</div>
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区 -->
    <main class="right-panel">
      <div class="chat-container">
        <!-- 主聊天区域 -->
        <div class="chat-messages" ref="messagesContainer">
          <div class="messages-wrapper">
            <div
                v-for="(message, index) in messages"
                :key="index"
                :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']"
            >
              <!-- 用户消息 -->
              <template v-if="message.role === 'user'">
                <div class="message-content user-content">
                  <div class="message-text">{{ message.content }}</div>
                  <div class="message-time">{{ message.time }}</div>
                </div>
              </template>

              <!-- AI 消息 -->
              <template v-else>
                <div class="message-content ai-content">
                  <!-- AI 处理状态提示 -->
                  <div v-if="message.status_message" class="ai-status-message">
                    <p>{{ message.status_message }}</p>
                    <div v-if="message.status_message !== '用户中止'" class="shimmer"></div>
                  </div>

                  <!-- 多用例数组 -->
                  <template v-if="isTestCaseJson(message.content)">
                    <template v-if="Array.isArray(parseTestCase(message.content))">
                      <TestCaseCard
                          v-for="(test, i) in parseTestCase(message.content)"
                          :key="i"
                          :testCase="test"
                      />
                    </template>
                    <!-- 单用例对象 -->
                    <template v-else>
                      <TestCaseCard :testCase="parseTestCase(message.content)" />
                    </template>
                  </template>

                  <!-- 否则按 markdown 渲染 -->
                  <div
                      v-else
                      class="message-text markdown-body"
                      v-html="renderMarkdown(message.content)"
                  ></div>
                  <div class="message-time">{{ message.time }}</div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-container">
          <div class="input-wrapper">
            <textarea
                v-model="userInput"
                @keydown.enter.prevent="sendMessage"
                placeholder="输入您的问题..."
                rows="1"
                ref="inputArea"
                @input="autoResize"
                :disabled="isSending"
            ></textarea>
            <button
                v-if="!isSending"
                @click="sendMessage"
                class="send-button"
                :disabled="!userInput.trim()"
            >
              <i class="fas fa-paper-plane"></i>
            </button>
            <button
                v-else
                @click="stopMessage"
                class="stop-button"
            >
              <i class="fas fa-stop"></i>
            </button>
          </div>
          <div class="input-tips">
            按 Enter 发送，Shift + Enter 换行
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { marked } from 'marked'
import TestCaseCard from './TestCaseCard.vue'

marked.setOptions({
  breaks: true,
  gfm: true,
})

export default {
  name: 'ChatWithSidebar',
  components: { TestCaseCard },
  data() {
    return {
      // 右侧消息（默认欢迎）
      messages: [
        {
          role: 'ai',
          content:
              '您好！我是TFBots!请问有什么可以帮您？\n\n我可以帮您：\n- 生成用例\n- 编写代码\n- 回答问题\n- 提供建议',
          time: this.getCurrentTime(),
          status_message: '',
        },
      ],
      userInput: '',
      isSending: false,
      abortController: null,

      // 左侧：会话名列表
      conversationNames: [],

      // 当前会话 uuid（字符串）
      currentConversationUuid: null,

      // auth user
      authUser: {},

      // token
      authToken: null,

      // 用于生成消息 id 的计数器
      messageCounter: 0,
    }
  },
  methods: {
    // ------------------------------
    // 基础工具与格式化
    // ------------------------------
    getCurrentTime() {
      const now = new Date()
      return `${now.getHours().toString().padStart(2, '0')}:${now
          .getMinutes()
          .toString()
          .padStart(2, '0')}`
    },
    formatShortDate(v) {
      if (!v) return ''
      try {
        const d = new Date(v)
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      } catch {
        return ''
      }
    },

    // ------------------------------
    // 左侧：加载会话名列表 & 选择会话
    // ------------------------------
    async loadAuthUser() {
      try {
        const raw = localStorage.getItem('auth.user')
        if (raw) {
          this.authUser = JSON.parse(raw)
        } else {
          if (window && window.auth && window.auth.user) {
            this.authUser = window.auth.user
          } else {
            this.authUser = {
              id: 13,
              username: '23222',
            }
          }
        }
      } catch (e) {
        console.warn('解析 auth.user 失败，使用默认：', e)
        this.authUser = { id: 13, username: '23222' }
      }

      // token
      this.authToken = localStorage.getItem('auth.token') || null
    },

    async loadConversationNames() {
      if (!this.authUser || !this.authUser.id) return
      const uid = this.authUser.id
      const url = `http://localhost:9090/conversation-names/user/${uid}`
      try {
        const resp = await fetch(url, {
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}),
          },
        })
        if (!resp.ok) {
          console.error('获取 conversation-names 失败', resp.status)
          this.conversationNames = []
          return
        }
        const body = await resp.json()
        const data = (body && (body.data ?? body)) || []
        this.conversationNames = Array.isArray(data) ? data : []
        if (this.conversationNames.length > 0) {
          if (!this.currentConversationUuid) {
            this.onSelectConversation(this.conversationNames[0])
          }
        }
      } catch (err) {
        console.error('loadConversationNames error', err)
        this.conversationNames = []
      }
    },

    onSelectConversation(item) {
      const uuid = item.conversationUuid || item.conversationUuid
      if (!uuid) {
        console.warn('selected conversation item missing conversationUuid', item)
        return
      }
      this.currentConversationUuid = uuid
      this.loadConversationByUuid(uuid)
    },

    async loadConversationByUuid(uuid) {
      if (!uuid) return
      const url = `http://localhost:9090/conversations/by-uuid/${encodeURIComponent(uuid)}`
      try {
        const resp = await fetch(url, {
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}),
          },
        })
        if (!resp.ok) {
          console.error('加载 conversation 失败', resp.status)
          return
        }
        const body = await resp.json()
        const payload = (body && (body.data ?? body)) || {}
        let msgs = payload.messages ?? payload.messages
        if (typeof msgs === 'string') {
          try {
            msgs = JSON.parse(msgs)
          } catch (e) {
            console.warn('解析 messages JSON 失败，保留原文本', e)
            msgs = []
          }
        }
        if (!Array.isArray(msgs)) msgs = []

        const mapped = msgs.map((m) => {
          const role = m.role || m.sender || (m.from === 'assistant' ? 'assistant' : (m.from === 'user' ? 'user' : 'ai'))
          const content = m.content ?? m.message ?? m.text ?? ''
          const ts = m.timestamp ?? m.createdAt ?? m.created_at ?? m.time ?? m.date
          return {
            role: role === 'assistant' || role === 'ai' ? 'ai' : (role === 'user' ? 'user' : role),
            content: typeof content === 'object' ? JSON.stringify(content) : String(content ?? ''),
            time: ts ? this.formatTime(ts) : this.getCurrentTime(),
            status_message: '',
          }
        })

        if (mapped.length > 0) {
          this.messages = mapped
        } else {
          this.messages = [
            {
              role: 'ai',
              content:
                  '您好！我是TFBots!请问有什么可以帮您？\n\n我可以帮您：\n- 生成用例\n- 编写代码\n- 回答问题\n- 提供建议',
              time: this.getCurrentTime(),
              status_message: '',
            },
          ]
        }

        this.$nextTick(this.scrollToBottom)
      } catch (err) {
        console.error('loadConversationByUuid error', err)
      }
    },

    formatTime(v) {
      try {
        const d = new Date(v)
        if (isNaN(d.getTime())) return String(v)
        const hh = String(d.getHours()).padStart(2, '0')
        const mm = String(d.getMinutes()).padStart(2, '0')
        return `${hh}:${mm}`
      } catch {
        return String(v)
      }
    },

    // ------------------------------
    // 右侧：现有聊天逻辑（保持大体不变）
    // ------------------------------
    isTestCaseJson(text) {
      try {
        const obj = JSON.parse(text)
        if (Array.isArray(obj)) {
          return obj.every(item =>
              (typeof item.case_id === 'string' || typeof item.caseId === 'string') &&
              typeof item.title === 'string' &&
              Array.isArray(item.steps) &&
              typeof item.expected_result === 'string'
          )
        } else {
          return (
              (typeof obj.case_id === 'string' || typeof obj.caseId === 'string') &&
              typeof obj.title === 'string' &&
              Array.isArray(obj.steps) &&
              typeof obj.expected_result === 'string'
          )
        }
      } catch {
        return false
      }
    },

    parseTestCase(text) {
      try {
        return JSON.parse(text)
      } catch {
        return null
      }
    },

    async typewriterEffect(aiMessage, content) {
      const delay = 10
      let index = 0
      return new Promise((resolve) => {
        const timer = setInterval(() => {
          if (index < content.length) {
            aiMessage.content += content[index]
            this.$forceUpdate()
            this.$nextTick(this.scrollToBottom)
            index++
          } else {
            clearInterval(timer)
            resolve()
          }
        }, delay)
      })
    },

    // ------------------------------
    // 新增：生成唯一消息 id
    // ------------------------------
    generateMessageId(prefix = 'm') {
      this.messageCounter = (this.messageCounter || 0) + 1
      return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2,6)}-${this.messageCounter}`
    },

    // ------------------------------
    // 新增：把单条消息追加到后端 conversation.messages jsonb
    // ------------------------------
    async appendMessageToServer(messageObj) {
      if (!this.currentConversationUuid) {
        console.warn('appendMessageToServer: no currentConversationUuid, skip append')
        return
      }
      const url = `http://localhost:9090/conversations/${encodeURIComponent(this.currentConversationUuid)}/append`
      try {
        const resp = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}),
          },
          body: JSON.stringify(messageObj),
        })
        if (!resp.ok) {
          const text = await resp.text().catch(()=>'')
          console.warn('appendMessageToServer failed', resp.status, text)
        }
      } catch (err) {
        console.error('appendMessageToServer error', err)
      }
    },

    // ------------------------------
    // 修改：sendMessage（完整实现）
    // ------------------------------
    async sendMessage() {
      const content = this.userInput.trim()
      if (!content || this.isSending) return

      // 构建并追加用户消息到后端（异步）
      const userMsgId = this.generateMessageId('m')
      const userMsgPayload = {
        id: userMsgId,
        role: 'user',
        content,
        timestamp: new Date().toISOString()
      }

      // 推送用户消息到界面
      this.messages.push({
        role: 'user',
        content,
        time: this.getCurrentTime(),
      })
      this.userInput = ''
      this.$nextTick(() => {
        this.scrollToBottom()
      })

      // 异步追加到后端（不阻塞 UI）
      this.appendMessageToServer(userMsgPayload).catch(err => {
        console.warn('append user message error', err)
      })

      // 占位 AI 消息
      let aiMessage = {
        role: 'ai',
        content: '',
        time: this.getCurrentTime(),
        status_message: '',
      }
      this.messages.push(aiMessage)

      this.isSending = true
      this.abortController = new AbortController()
      const signal = this.abortController.signal
      const token = this.authToken

      try {
        const response = await fetch('http://localhost:9090/api/chat/normal', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
          body: JSON.stringify({
            messages: this.messages.map(msg => ({
              role: msg.role,
              content: msg.role === 'user' ? msg.content + '/no_think' : msg.content
            })),
            ...(this.currentConversationUuid ? { conversationUuid: this.currentConversationUuid } : {})
          }),
          signal,
        })
        if (!response.ok || !response.body) {
          throw new Error(`网络错误：${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        let streamDone = false
        while (!streamDone) {
          const { done, value } = await reader.read()
          if (done) {
            streamDone = true
            break
          }
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split(/\r?\n/)
          buffer = lines.pop()
          for (const line of lines) {
            const trimmed = line.trim()
            if (!trimmed) continue
            if (!trimmed.startsWith('data:')) continue
            const jsonStr = trimmed.replace(/^(?:data:\s*)+/, '').trim()
            if (!jsonStr) continue
            if (jsonStr === '[DONE]') {
              streamDone = true
              break
            }
            try {
              const data = JSON.parse(jsonStr)
              if (data.status_message !== undefined) {
                aiMessage.status_message = data.status_message
                this.$forceUpdate()
              } else if (data.choices && data.choices[0] && data.choices[0].delta && data.choices[0].delta.content) {
                aiMessage.status_message = ''
                aiMessage.content += data.choices[0].delta.content
                this.$forceUpdate()
                this.$nextTick(this.scrollToBottom)
              }
            } catch (e) {
              console.error('Error parsing SSE data:', e, 'raw:', jsonStr)
            }
          }
        }

        // 流结束后把 assistant 消息追加到后端
        if (aiMessage) aiMessage.status_message = ''
        const assistantMsgPayload = {
          id: this.generateMessageId('m'),
          role: 'assistant',
          content: aiMessage.content,
          timestamp: new Date().toISOString()
        }
        this.appendMessageToServer(assistantMsgPayload).catch(err => {
          console.warn('append assistant message error', err)
        })
      } catch (error) {
        if (error.name === 'AbortError') {
          console.log('Fetch aborted by user.')
          if (aiMessage) {
            aiMessage.status_message = '用户中止'
            this.$forceUpdate()
          }
        } else {
          console.error('发送消息失败:', error)
          if (aiMessage) {
            aiMessage.status_message = `AI处理出错: ${error.message}`
            this.$forceUpdate()
          }
        }
      } finally {
        this.isSending = false
        this.abortController = null
      }
    },

    stopMessage() {
      if (this.abortController) {
        this.abortController.abort()
        console.log('消息发送已中断')
      }
    },

    scrollToBottom() {
      const container = this.$refs.messagesContainer
      if (container) {
        container.scrollTop = container.scrollHeight
      }
    },

    autoResize() {
      if (!this.isSending) {
        const textarea = this.$refs.inputArea
        if (!textarea) return
        textarea.style.height = 'auto'
        textarea.style.height = `${textarea.scrollHeight}px`
      }
    },

    renderMarkdown(text) {
      return marked(text)
    },
  },

  watch: {
    isSending(newValue) {
      if (!newValue) {
        this.$nextTick(this.autoResize)
      }
    }
  },

  mounted() {
    this.$nextTick(this.autoResize)
    this.loadAuthUser()
        .then(() => this.loadConversationNames())
        .catch(err => {
          console.warn('初始化 auth 或 conversationNames 失败', err)
        })

    if (this.$route && this.$route.query && this.$route.query.initialMessage) {
      const initialMessageContent = this.$route.query.initialMessage
      this.messages.push({
        role: 'user',
        content: initialMessageContent,
        time: this.getCurrentTime(),
      })
      this.$router.replace({ query: null })
      this.userInput = initialMessageContent
      this.sendMessage()
      this.userInput = ''
    }
  }
}
</script>

<style scoped>
/* overall layout */
.chat-layout {
  display: flex;
  height: 100vh;
  background: #ffffff;
  overflow: hidden;
}

/* left panel 15% */
.left-panel {
  width: 15%;
  min-width: 220px;
  max-width: 320px;
  box-sizing: border-box;
  border-right: 1px solid rgba(0,0,0,0.04);
  padding: 12px;
  background: #fbfcfd;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* card look without big margin, rounded */
.left-card {
  background: #fff;
  border-radius: 12px;
  padding: 10px;
  box-shadow: 0 6px 18px rgba(15,23,42,0.06);
  display: flex;
  flex-direction: column;
  height: calc(100vh - 30px);
  overflow: hidden;
}

.left-card-header {
  padding: 6px 6px 12px;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.left-card-header .title {
  font-weight: 700;
  color: #0f172a;
}
.left-card-header .sub { color:#6b7280; font-size:12px; margin-top:4px; }

/* list scroll area */
.left-list {
  overflow-y: auto;
  padding: 8px 2px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* each item: default light gray translucent, selected dark gray translucent */
.left-item {
  display:flex;
  align-items:center;
  cursor:pointer;
  padding:10px;
  border-radius:10px;
  background: rgba(15,23,42,0.02);
  border: 1px solid rgba(15,23,42,0.03);
  transition: all .12s ease;
}
.left-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(15,23,42,0.04);
}
.left-item.active {
  background: linear-gradient(90deg, rgba(31,111,235,0.06), rgba(255,255,255,0));
  border-color: rgba(31,111,235,0.12);
}

.left-item-main { display:flex; flex-direction:column; gap:4px; }
.left-item-title { font-weight:600; color:#0f172a; font-size:14px; }
.left-item-sub { font-size:12px; color:#6b7280; }

/* empty */
.left-empty { color:#9ca3af; text-align:center; margin-top:18px; }

/* right panel takes rest */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* -------------- 复用你原样式 -------------- */
.markdown-body {
  font-size: 13px;
  line-height: 1.6;
  word-wrap: break-word;
  text-align: left;
}
.markdown-body pre { background-color: #f6f8fa; border-radius: 6px; padding: 16px; overflow: auto; }
.markdown-body code { background-color: rgba(175, 184, 193, 0.2); border-radius: 4px; padding: 0.2em 0.4em; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, "Roboto Mono", "Courier New", monospace; }
.markdown-body pre code { background-color: transparent; padding: 0; }
.markdown-body blockquote { border-left: 0.25em solid #d0d7de; padding: 0 1em; color: #656d76; margin: 0; }
.markdown-body ul { padding-left: 2em; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  background: #ffffff;
  padding: 0;
  width: 100%;
}
.messages-wrapper {
  padding: 20px 5%;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100%;
}
.message {
  display: flex;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.message-content { flex: 1; }
.user-content { margin-left: auto; display: flex; flex-direction: column; align-items: flex-end; }
.ai-content { margin-right: auto; text-align: left; width: 100%; }
.message-text { font-size: 15px; line-height: 2; }
.user-message .message-text {
  background: #f0f0f0; color: #1a1a1a; padding: 12px 16px; border-radius: 8px; box-shadow: 0 1px 2px rgba(0,0,0,0.1); display: inline-block; max-width: 100%;
}
.ai-message .message-text { color: #1a1a1a; padding: 0; }
.message-time { font-size: 11px; color: #999; margin-top: 4px; }
.chat-input-container { padding: 16px 5%; max-width: 1200px; margin: 0 auto; background: #ffffff; border-top: 1px solid #e6e6e6; }
.input-wrapper { display: flex; align-items: flex-end; background: #ffffff; border: 1px solid #e6e6e6; border-radius: 12px; padding: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); }
textarea { flex: 1; border: none; outline: none; resize: none; padding: 8px; font-size: 14px; line-height: 1.6; width: 450px; height: 56px; max-height: 200px; min-height: 56px; font-family: inherit; overflow-y: hidden; }
.send-button, .stop-button { background: #10a37f; color: white; border: none; border-radius: 6px; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background-color 0.2s; flex-shrink: 0; }
.send-button:hover:not(:disabled) { background: #0d8c6d; }
.send-button:disabled { background: #ccc; cursor: not-allowed; }
.stop-button { background: #f56c6c; }
.stop-button:hover:not(:disabled) { background: #ea7a7a; }
.stop-button:disabled { background: #fab6b6; cursor: not-allowed; }
.input-tips { font-size: 11px; color: #999; margin-top: 8px; text-align: center; }
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #888; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #555; }

/* AI Status shimmer */
.ai-status-message { position: relative; display: inline-block; padding: 8px 15px; background-color: rgba(128,128,128,0.1); color: rgba(128,128,128,0.7); border-radius: 15px; overflow: hidden; font-size: 0.9em; font-style: italic; min-width: 100px; text-align: center; }
.ai-status-message .shimmer { position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(to right, transparent, rgba(255,255,255,0.3), transparent); animation: shimmer 1.5s infinite; }
@keyframes shimmer { 0% { left: -100%; } 100% { left: 100%; } }
/* 新增：确保 chat-container 是 column 布局，便于 sticky 生效 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 保持消息区可滚动，填满剩余高度 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  background: #ffffff;
  padding: 0;
  width: 100%;
}

/* 给 messages-wrapper 底部留出足够空间，避免被输入框遮挡 */
.messages-wrapper {
  padding: 20px 5% 140px; /* 这里把底部 padding 增大为 140px，根据输入框高度调整 */
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100%;
}

/* 关键：把输入区域固定在容器底部（sticky 在父容器为 column 时生效） */
.chat-input-container {
  position: sticky;
  bottom: 0;
  z-index: 50;             /* 提到最上层，防止被消息遮挡 */
  padding: 12px 5%;       /* 与 messages-wrapper 两侧对齐 */
  max-width: 1200px;
  margin: 0 auto;
  background: #ffffff;    /* 确保覆盖在消息上，可根据主题调整 */
  border-top: 1px solid #e6e6e6;
  box-shadow: 0 -6px 18px rgba(15,23,42,0.03); /* 轻微上方阴影，视觉分隔 */
}

/* 可选：收紧 input-wrapper 的垂直空间，使输入区更紧凑 */
.input-wrapper {
  display: flex;
  align-items: flex-end;
  background: #ffffff;
  border: 1px solid #e6e6e6;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}

html, body, #app { height: 100%; box-sizing: border-box; }
</style>
