<template>
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
</template>

<script>
import { marked } from 'marked'
import TestCaseCard from './TestCaseCard.vue'

marked.setOptions({
  breaks: true,
  gfm: true,
})

export default {
  name: 'Chat',
  components: { TestCaseCard },
  data() {
    return {
      messages: [
        {
          role: 'ai',
          content:
              '你好！我是您的AI助手，请问有什么可以帮您？\n\n我可以帮您：\n- 回答问题\n- 编写代码\n- 解释概念\n- 提供建议',
          time: this.getCurrentTime(),
          status_message: '', // 为初始 AI 消息添加 status_message
        },
      ],
      userInput: '',
      isSending: false,
      abortController: null,
    }
  },
  methods: {
    getCurrentTime() {
      const now = new Date()
      return `${now.getHours().toString().padStart(2, '0')}:${now
          .getMinutes()
          .toString()
          .padStart(2, '0')}`
    },
    isTestCaseJson(text) {
      try {
        const obj = JSON.parse(text)
        if (Array.isArray(obj)) {
          return obj.every(item =>
              typeof item.case_id === 'string' &&
              typeof item.title === 'string' &&
              Array.isArray(item.steps) &&
              typeof item.expected_result === 'string'
          )
        } else {
          return (
              typeof obj.case_id === 'string' &&
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
    // 新增：模拟打字机效果的方法
    async typewriterEffect(aiMessage, content) {
      const delay = 10; // 每个字的延迟，可调整
      let index = 0;
      return new Promise((resolve) => {
        const timer = setInterval(() => {
          if (index < content.length) {
            aiMessage.content += content[index];
            this.$forceUpdate();
            this.$nextTick(this.scrollToBottom);
            index++;
          } else {
            clearInterval(timer);
            resolve();
          }
        }, delay);
      });
    },

    async sendMessage() {
      const content = this.userInput.trim()
      if (!content || this.isSending) return

      // 推送用户消息
      this.messages.push({
        role: 'user',
        content,
        time: this.getCurrentTime(),
      })
      this.userInput = ''
      this.$nextTick(() => {
        this.scrollToBottom()
      })

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
      const token = localStorage.getItem("auth.token")

      try {
        const response = await fetch('http://localhost:9090/api/chat/normal', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json',
                     'Authorization': 'Bearer ' + token},
          body: JSON.stringify({
            messages: this.messages.map(msg => ({
              role: msg.role,
              content: msg.role === 'user' ? msg.content + '/no_think' : msg.content
            })),
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
            // 完整流结束
            streamDone = true
            break
          }

          buffer += decoder.decode(value, { stream: true })
          // 按行分割。事件之间通常有一个空行分隔（SSE）
          const lines = buffer.split(/\r?\n/)
          // 最后一行可能是未完的片段，保存在 buffer 里
          buffer = lines.pop()

          for (const line of lines) {
            const trimmed = line.trim()
            if (!trimmed) {
              // 空行 -> SSE 事件结束（但我们已经按行处理 data: 行了）
              continue
            }
            if (!trimmed.startsWith('data:')) {
              // 忽略不是 data: 开头的行（例如: event:, id: 等）
              continue
            }

            // 移除所有前缀的 data:（有时服务端会多次写 data: data: {...}）
            const jsonStr = trimmed.replace(/^(?:data:\s*)+/, '').trim()
            if (!jsonStr) continue

            if (jsonStr === '[DONE]') {
              streamDone = true
              break
            }

            try {
              const data = JSON.parse(jsonStr)
              // 优先处理 status_message
              if (data.status_message !== undefined) {
                aiMessage.status_message = data.status_message
                this.$forceUpdate()
              } else if (data.choices && data.choices[0] && data.choices[0].delta && data.choices[0].delta.content) {
                // 内容片段
                aiMessage.status_message = ''
                aiMessage.content += data.choices[0].delta.content
                this.$forceUpdate()
                this.$nextTick(this.scrollToBottom)
              }
            } catch (e) {
              console.error("Error parsing SSE data:", e, "raw:", jsonStr)
              // 如果解析失败，忽略该片段继续处理后面的
            }
          }
          // 继续外层循环，直到 reader.read() 的 done 为 true 或遇到 [DONE]
        }

        // 确保最终 status_message 清空（流正常结束）
        if (aiMessage) aiMessage.status_message = ''
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


    // 新增：中断消息发送的方法
    stopMessage() {
      if (this.abortController) {
        this.abortController.abort(); // 调用 abort 方法中断请求
        console.log('消息发送已中断');
      }
    },

    scrollToBottom() {
      const container = this.$refs.messagesContainer
      if (container) {
        container.scrollTop = container.scrollHeight
      }
    },

    autoResize() {
      if (!this.isSending) { // 仅在非发送状态时调整大小
        const textarea = this.$refs.inputArea;
        textarea.style.height = 'auto';
        textarea.style.height = `${textarea.scrollHeight}px`;
      }
    },

    renderMarkdown(text) {
      return marked(text)
    },
  },
  watch: {
    isSending(newValue) {
      // 当 isSending 从 true 变为 false 时，重新调整输入框大小
      if (!newValue) {
        this.$nextTick(this.autoResize);
      }
    }
  },
  mounted() {
    this.$nextTick(this.autoResize);
    // 检查路由中是否有 initialMessage
    if (this.$route.query.initialMessage) {
      const initialMessageContent = this.$route.query.initialMessage;
      // 将初始消息添加到消息列表
      this.messages.push({
        role: 'user',
        content: initialMessageContent,
        time: this.getCurrentTime(),
      });
      // 清除路由参数，避免重复添加
      this.$router.replace({ query: null });
      // 触发 AI 回复
      this.userInput = initialMessageContent; // 临时设置 userInput 来触发 sendMessage 的逻辑
      this.sendMessage();
      this.userInput = ''; // 清除 userInput
    }
  }
}
</script>

<style>

.markdown-body {
  font-size: 13px;
  line-height: 1.6;
  word-wrap: break-word;
  text-align: left;
}

.markdown-body pre {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}

.markdown-body code {
  background-color: rgba(175, 184, 193, 0.2);
  border-radius: 4px;
  padding: 0.2em 0.4em;
  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo,
  Consolas, Liberation Mono, monospace;
}

.markdown-body pre code {
  background-color: transparent;
  padding: 0;
}

.markdown-body blockquote {
  border-left: 0.25em solid #d0d7de;
  padding: 0 1em;
  color: #656d76;
  margin: 0;
}

.markdown-body ul {
  padding-left: 2em;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  background: #ffffff;
  padding: 0;
  width: 100%;
}

/* ✅ 修改后的样式：更宽的聊天内容区域 */
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

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-content {
  flex: 1;
}

.user-content {
  margin-left: auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.ai-content {
  margin-right: auto;
  text-align: left;
  width: 100%;
}

.message-text {
  font-size: 15px;
  line-height: 2;
}

.user-message .message-text {
  background: #f0f0f0;
  color: #1a1a1a;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  display: inline-block;
  max-width: 100%;
}

.ai-message .message-text {
  color: #1a1a1a;
  padding: 0;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.chat-input-container {
  padding: 16px 5%;
  max-width: 1200px;
  margin: 0 auto;
  background: #ffffff;
  border-top: 1px solid #e6e6e6;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  background: #ffffff;
  border: 1px solid #e6e6e6;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: 8px;
  font-size: 14px;
  line-height: 1.6;
  width: 450px;
  height: 56px;
  max-height: 200px;
  min-height: 56px;
  font-family: inherit;
  overflow-y: hidden;
}

.send-button, .stop-button {
  background: #10a37f;
  color: white;
  border: none;
  border-radius: 6px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.send-button:hover:not(:disabled) {
  background: #0d8c6d;
}

.send-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.stop-button {
  background: #f56c6c;
}

.stop-button:hover:not(:disabled) {
  background: #ea7a7a;
}

.stop-button:disabled {
  background: #fab6b6;
  cursor: not-allowed;
}

.input-tips {
  font-size: 11px;
  color: #999;
  margin-top: 8px;
  text-align: center;
}

::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* AI Status Message Styles */
.ai-status-message {
  position: relative;
  display: inline-block;
  padding: 8px 15px;
  background-color: rgba(128, 128, 128, 0.1);
  color: rgba(128, 128, 128, 0.7);
  border-radius: 15px;
  overflow: hidden; /* For shimmer effect */
  font-size: 0.9em;
  font-style: italic;
  min-width: 100px;
  text-align: center;
}

.ai-status-message .shimmer {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
      to right,
      transparent,
      rgba(255, 255, 255, 0.3),
      transparent
  );
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

html, body, #app {
  height: 100%;
  box-sizing: border-box;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 95vh;
  overflow: hidden;
  background: #ffffff;
}

.messages-wrapper {
  padding: 20px 5%;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 0;
}

.chat-input-container {
  z-index: 2;

}


</style>
