<template>
  <div class="chat-wrapper">
    <div class="chat-container">
      <!-- 聊天内容区 -->
      <div class="messages-area">
        <div
            v-for="(msg, index) in displayedMessages"
            :key="index"
            class="message-bubble"
            :class="msg.role"
        >
          <!-- 普通助手 -->
          <div
              v-if="msg.role === 'assistant'"
              class="bubble-content"
              v-html="msg.content"
          ></div>

          <!-- 用户输入 -->
          <div v-else-if="msg.role === 'user'" class="bubble-content">
            {{ msg.content }}
          </div>

          <!-- 思考气泡 -->
          <div
              v-else-if="msg.role === 'thinking'"
              class="bubble-content thinking-content"
              v-html="msg.content"
          ></div>
        </div>

      </div>

      <!-- 输入区 -->
      <div class="input-bubble">
        <div class="bubble-form">
          <div class="db-search-switch">
            <el-switch v-model="dbSearch" />
            <span class="db-search-label">数据库检索</span>
          </div>
          <div class="db-search-switch" style="margin-left: 126px">
            <el-switch v-model="isThinking" />
            <span class="db-search-label">深度思考🧠</span>
          </div>

          <el-input
              v-model="inputText"
              type="textarea"
              placeholder="✏️ 请输入内容..."
              :autosize="{ minRows: 2, maxRows: 4 }"
              class="cute-input"
              @keydown.enter.prevent="sendMessage"
          ></el-input>
          <el-button
              :disabled="isLoading || !inputText.trim()"
              class="send-button"
              :class="{ active: inputText.trim() && !isLoading }"
              @click="sendMessage"
          >
            <el-icon :size="20" class="send-icon">
              <svg v-if="isLoading" viewBox="0 0 1024 1024" class="loading-icon">
                <path
                    fill="currentColor"
                    d="M512 1024c-69.1 0-136.2-13.5-199.3-40.2C251.7 958 197 921 150 874c-47-47-84-101.7-109.8-162.7C13.5 648.2 0 581.1 0 512c0-19.9 16.1-36 36-36s36 16.1 36 36c0 59.4 11.6 117 34.6 171.3 22.2 52.4 53.9 98.8 94.3 136.3 40.4 37.5 88.7 66.9 142.7 87.5 50.4 19.5 104 29.5 158.4 29.5 19.9 0 36 16.1 36 36s-16.1 36-36 36z"
                />
              </svg>
              <svg v-else-if="!inputText.trim()" viewBox="0 0 1024 1024">
                <path
                    fill="currentColor"
                    d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm-38.4 409.6H409.6v307.2h64V473.6h64v-64h-64zM512 320a51.2 51.2 0 1 0 0-102.4 51.2 51.2 0 0 0 0 102.4z"
                />
              </svg>
              <svg v-else viewBox="0 0 1024 1024">
                <path
                    fill="currentColor"
                    d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm-32 232.704h-64V512h64V296.704zm0 196.864v158.72h64v-158.72h-64z"
                />
              </svg>
            </el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>

</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import { computed } from 'vue'

const displayedMessages = computed(() =>
    messages.value.filter(msg => msg.role !== 'system')
)
const isThinking = ref(false)
let thinkingMsgIndex = null

/* ---------------------- Markdown 渲染器 ---------------------- */
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  breaks: true
})
const renderMarkdown = text => md.render(text || '')

/* ---------------------- 响应式状态 ---------------------- */
const messages = ref([
  {
    role: 'system',
    content:
        '你是一位职场云小助手，回答要自然,热情，可适当加入表情。'
  },
  {
    role: 'assistant',
    content: '你好！我是职场云☁小助手，有什么可以帮您的吗？😊'
  }
])

const inputText = ref('')
const dbSearch  = ref(false)
const isLoading = ref(false)

/* ---------------------- 发送按钮逻辑 ---------------------- */
const sendMessage = async () => {
  scrollToBottom()
  const userQuestion = inputText.value.trim()
  if (!userQuestion || isLoading.value) return

  // 1) 推入用户消息
  messages.value.push({ role: 'user', content: userQuestion })
  inputText.value = ''

  // 2) 预留 assistant 节点占位
  const assistantIndex = messages.value.push({ role: 'assistant', content: '' }) - 1
  isLoading.value = true
  const processedQuestion = isThinking.value ? `/think ${userQuestion}` : `/nothink ${userQuestion}`
  try {
    /* —— txt2sql 分支 —— */
    if (dbSearch.value) {
      const sqlMd  = await generateSQL(userQuestion)   // SQL 片段 (Markdown)
      messages.value[assistantIndex].content = sqlMd   // 先渲染 SQL 结果

      const queryResult = await generateData(userQuestion)
      const prompt = `这是数据库的查询结果:\\n${queryResult}。现在请你整理后给出用户问题的答案，请记住这个数据来源是根据数据库查询结果而不是用户提供！如果在返回数据库查询结果时涉及到dept_no，这是对照关系：dept_no(对应 dept_name:1监察，2财务，3后勤，4运维，5开发，6产品，7测试，8人力，9市场，10销售 等等部门)，如果涉及到金额记住单位是万.以上所有不要在对话中提到！！用户的问题：${processedQuestion}。`

      // ►► 继续流式对话
      await sendStreamRequest(buildMessagesArray(prompt), assistantIndex)
    }
    /* —— 普通聊天 / 非 txt2sql —— */
    else {
      await sendStreamRequest(buildMessagesArray(processedQuestion), assistantIndex)
    }

    scrollToBottom()
  } catch (err) {
    console.error(err)
    ElMessage.error('发生错误，请稍后重试')
  } finally {
    isLoading.value = false
  }
}

/* ---------------------- 构造消息数组 ---------------------- */
const buildMessagesArray = (lastQuestion) => {
  const history = messages.value.map(m => ({ role: m.role, content: m.content }))
  history.pop()
  history.push({ role: 'user', content: lastQuestion })
  return history
}

/* ---------------------- 生成 SQL、查询数据 ---------------------- */
const generateSQL = async (question) => {
  try {
    const response = await fetch('http://localhost:9090/qa/genSql', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: localStorage.getItem('jwt') || ''
      },
      body: JSON.stringify({ question, txt2sql: true })
    });
    if (!response.ok) {
      throw new Error('无法获取SQL查询');
    }
    const json = await response.json();
    if (json.code !== '200') {
      throw new Error(json.msg || '生成SQL失败');
    }
    const sql = json.data;
    console.log("sql为："+sql)
    return `<div style="background:#f5f5f5;border-radius:8px;padding:10px;font-family:sans-serif;font-size:12px;color:#666;box-shadow:0 4px 8px rgba(0,0,0,0.1);transition:background 0.3s ease;margin:10px 0;"><div style="display:flex;justify-content:space-between;align-items:center;cursor:pointer;transition:color 0.3s ease;" onclick="const pre=this.nextElementSibling;const arrow=this.querySelector('.arrow');const collapsed=pre.style.display==='none';pre.style.display=collapsed?'block':'none';arrow.style.transform=collapsed?'rotate(120deg)':'rotate(-120deg)';arrow.textContent=collapsed?'▼':'▶';"><span style='transition:color 0.3s ease;' onmouseover='this.style.color=\"#007BFF\"' onmouseout='this.style.color=\"#666\"'>已进行SQL语句的转换：</span><span class='arrow' style='margin-left:8px;transition:transform 0.3s ease;position:relative;top:1px;'>▶</span></div><pre style="background:#000;color:#fff;padding:8px;border-radius:6px;overflow-x:auto;margin-top:8px;font-family:monospace;white-space:pre-wrap;transition:max-height 0.5s ease-in-out;max-height:1000px;" v-show="!collapsed">${sql}</pre></div>`;
  } catch (error) {
    console.error('生成SQL失败:', error);
    return '生成 SQL 时出错，请稍后重试。';
  }
};


const generateData = async (question) => {
  const res = await fetch('http://localhost:9090/qa/ask/sql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: localStorage.getItem('jwt') || '' },
    body: JSON.stringify({ question, txt2sql: true })
  });

  const json = await res.json();
  console.log(json)
  if (json.code !== '200') {
    return '查询失败'; // 查询失败时返回错误信息
  }

  // 如果没有返回数据，返回"无返回结果"信息
  if (!json.data) {
    return '无返回结果';
  }
  console.log((json.data))
  return json.data;
};


/* ---------------------- 发送流式请求 ---------------------- */
const sendStreamRequest = async (messagesArray, assistantIndex) => {
  const response = await fetch('http://localhost:9090/qa/askStream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: localStorage.getItem('jwt') || '' },
    body: JSON.stringify(messagesArray)       // ★ 直接发送数组！
  })

  if (!response.ok || !response.body) throw new Error('无法获取流响应')
  let isThinking = false
  const reader   = response.body.getReader()
  const decoder  = new TextDecoder('utf-8')
  let   buffer   = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop()      // 保留未完整一行

    for (let line of lines) {
      line = line.trim()
      if (!line || line === 'data: [DONE]') continue

      // 去掉前缀
      line = line.replace(/^data:\s*/, '').replace(/^data:\s*/, '')
      try {
        const json  = JSON.parse(line)
        let delta = json.choices?.[0]?.delta?.content
        if (delta) {
          // —— 检测 <think> 开始 ——
          if (delta.includes('<think>')) {
            isThinking = true
            // 在 assistantIndex 之后插入一个新的 thinking 消息
            thinkingMsgIndex = messages.value.length
            messages.value.push({
              role: 'thinking',
              content: ''
            })
            // 去掉标签
            delta = delta.replace('<think>', '')
          }
          if (delta.includes('</think>')) {
            delta = delta.replace('</think>', '')
            if (delta) {
              await typeWriterEffect(delta, thinkingMsgIndex)
            }
            isThinking = false
            thinkingMsgIndex = null
            continue
          }
          if (isThinking) {
            await typeWriterEffect(delta, thinkingMsgIndex)
          } else {
            await typeWriterEffect(delta, assistantIndex)
          }
        }

      } catch { /* 忽略解析失败的心跳包 */ }
    }
  }

  /* 结束后把纯文本转换为 Markdown HTML */
  const raw = messages.value[assistantIndex].content
  messages.value[assistantIndex].content = renderMarkdown(raw)
}

/* ---------------------- 打字机效果 ---------------------- */
const typeWriterEffect = (text, msgIndex) => {
  return new Promise(resolve => {
    let i = 0
    const interval = setInterval(() => {
      if (i >= text.length) {
        clearInterval(interval)
        resolve()
        return
      }
      messages.value[msgIndex].content += text[i]
      i++
      nextTick(scrollToBottom)
    }, 30)
  })
}




/* ---------------------- 工具：滚动到底部 ---------------------- */
const scrollToBottom = () => {
  nextTick(() => {
    const el = document.querySelector('.messages-area')
    if (el) el.scrollTop = el.scrollHeight
  })
}
</script>


<style scoped>
.chat-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 90vh;
  background: linear-gradient(135deg, #f5f0ff 0%, #e3f6ff 100%) fixed;
  padding: 1px;
}


.chat-container {
  width: 100%;
  max-width: 1300px;
  height: 660px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 30px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  backdrop-filter: blur(5px);
  border: 2px solid rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: column;
}

.input-bubble {
  margin: 0 30px 30px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 25px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.05);
  overflow: visible;
}

.bubble-form {
  position: relative;
  display: flex;
  align-items: center;
  padding-right: 60px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 25px;
  overflow: visible;
}

.bubble-form .db-search-switch {
  position: absolute;
  bottom: 10px;
  display: flex;
  align-items: center;
  background: #eef4ff;
  border: 1px solid #c1d4f7;
  border-radius: 20px;
  padding: 4px 8px;
  z-index: 3;
}

.bubble-form .db-search-switch:first-of-type {
  left: 10px;
}
.bubble-form .db-search-switch:nth-of-type(2) {
  left: 19px;
}


/* 提升输入框和按钮层级 */
.cute-input :deep(.el-textarea__inner) {
  width: 100%;
  min-height: 120px !important;
  border: none !important;
  background: transparent !important;
  padding: 15px 25px;
  color: #666;
  resize: none;
  line-height: 1.0 !important;
  font-size: 14px; /* 可选，调字体大小 */
  border-radius: 25px !important;
  position: relative;
  z-index: 1;
}

.send-button {
  position: absolute;
  right: 10px;
  width: 40px;
  height: 38px;
  padding: 0;
  border: none !important;
  background: #e0e7ff !important;
  border-radius: 50%;
  transition: all 0.3s;
  z-index: 2;
}
.send-button.active {
  background: #b3c1e1 !important;
}
.send-button:hover {
  background: #c1d4f7;
}
.send-icon {
  color: #707f8b;
  font-size: 18px;
}

/* 新增加载动画 */
.loading-icon {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.messages-area {
  flex: 1;
  padding: 30px;
  overflow-y: auto;
  background: linear-gradient(to bottom, #f8f9ff, #f0f7ff);
}

.message-bubble {
  margin: 15px 0;
  display: flex;
  animation: bubbleAppear 0.3s ease-out;
}

@keyframes bubbleAppear {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.bubble-content {
  max-width: 75%;
  padding: 15px 20px;
  font-size: 16px;
  line-height: 1.6;
  border-radius: 18px;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
  display: inline-block;
}

.user .bubble-content {
  background: linear-gradient(135deg, #6c8cff, #5b7cff);
  margin-left: auto;
  color: white;
  border-radius: 20px 20px 5px 20px;
}

.assistant .bubble-content {
  background: #ffffff;
  color: #666;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
  border-radius: 20px 20px 20px 5px;
}

/* Markdown渲染样式 */


.markdown-body h1,
.markdown-body h2,
.markdown-body h3 {
  margin-top: 1em;
  margin-bottom: 16px;
  font-weight: 600;
}

.markdown-body ul,
.markdown-body ol {
  padding-left: 2em;
}

.markdown-body pre {
  background-color: #f6f8fa;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}

.markdown-body code {
  background-color: rgba(27,31,35,0.05);
  border-radius: 3px;
  padding: 0.2em 0.4em;
}


/* Markdown样式 */
:deep(.markdown-body) {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  line-height: 1.6;
}

:deep(.markdown-body h1) {
  font-size: 2em;
  border-bottom: 1px solid #eaecef;
  padding-bottom: 0.3em;
}

:deep(.markdown-body pre) {
  background-color: #f6f8fa;
  border-radius: 3px;
  padding: 16px;
  overflow: auto;
}

:deep(.markdown-body p) {
  margin: 0;
}

/* 思考气泡 */
.thinking-content {
  background: rgba(240, 240, 240, 0.5);   /* 50% 透明度灰色 */
  color: #888;                             /* 更淡的文字颜色 */
  font-size: 13px;                         /* 略小字体 */
  font-style: normal;                      /* 取消斜体 */
  border-radius: 20px;                     /* 加大圆角 */
  padding: 6px 10px;                       /* 适当内边距 */
  margin: 8px 0;
  max-width: 75%;
  align-self: flex-start;
  line-height: 1.4;
}


.thinking .message-bubble {
  justify-content: flex-start;
}


</style>