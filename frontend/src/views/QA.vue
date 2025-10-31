<template>
  <div class="chat-layout">
    <aside class="left-panel">
      <div class="left-card">
        <div class="left-card-header">
          <div class="title-and-actions">
            <div class="title">会话列表</div>
            <div class="sub">user: {{ authUser.username || authUser.id }}</div>
          </div>

          <div class="header-actions">
            <button class="new-conv-btn" @click="createNewConversation" :disabled="isCreating">
              {{ isCreating ? '创建中...' : '新对话' }}
            </button>
          </div>
        </div>

        <div class="left-list" ref="leftList">
          <div
              v-for="(item, idx) in conversationNames"
              :key="item.id || item.conversationUuid || item.conversation_uuid || idx"
              :class="['left-item', { active: currentConversationUuid === (item.conversationUuid || item.conversation_uuid) }]"
              @click="onSelectConversation(item)"
              @mouseenter="hoverItemId = item.id"
              @mouseleave="hoverItemId = null"
              ref="leftItems"
          >
            <div class="left-item-main">
              <!-- 行内重命名：如果当前在编辑则显示 input -->
              <div v-if="editingId === item.id" class="left-item-title-edit">
                <input
                    ref="renameInput"
                    v-model="editingName"
                    @keydown.enter.prevent="confirmRename(item)"
                    @keydown.esc.prevent="cancelRename()"
                    @blur="confirmRename(item)"
                    class="rename-input"
                    :placeholder="item.name || '未命名会话'"
                />
              </div>
              <div v-else class="left-item-title">{{ item.name || '未命名会话' }}</div>
              <div class="left-item-sub">{{ formatShortDate(item.createdAt || item.created_at) }}</div>
            </div>

            <!-- 三点按钮 -->
            <div
                class="ellipsis-btn"
                :class="{ visible: hoverItemId === item.id || openMenuId === item.id }"
                @click.stop="toggleMenu(item, $event)"
                :data-item-id="item.id"
            >
              <i class="fas fa-ellipsis-h"></i>
            </div>
          </div>

          <div v-if="conversationNames.length === 0" class="left-empty">暂无会话</div>
        </div>
      </div>
    </aside>

    <!-- 菜单：fixed 放在根部（不在 left-item 内部） -->
    <transition name="slide-from-right-up">
      <div
          v-if="openMenuId"
          class="menu-popup"
          :style="menuStyle"
          @click.stop
          ref="floatingMenu"
      >
        <div
            class="menu-item"
            :class="{ selected: activeAction && activeAction.id === openMenuId && activeAction.action === 'rename' }"
            @click="onRenameClick(findById(openMenuId))"
        >
          <i class="fas fa-pen icon"></i>
          <span class="label">重命名</span>
        </div>

        <div class="menu-divider"></div>

        <div
            class="menu-item delete"
            :class="{ selected: activeAction && activeAction.id === openMenuId && activeAction.action === 'delete' }"
            @click="onDeleteClick(findById(openMenuId))"
        >
          <i class="fas fa-trash icon"></i>
          <span class="label">删除</span>
        </div>
      </div>
    </transition>

    <!-- Delete Confirm Modal (centered) -->
    <transition name="fade">
      <div v-if="deleteModal.show" class="overlay" @click.self="closeDeleteModal">
        <div class="confirm-modal" role="dialog" aria-modal="true" @click.stop>
          <div class="modal-body">
            <div class="modal-title">删除会话</div>
            <div class="modal-desc">确定要删除会话 "{{ deleteModal.item?.name || '未命名会话' }}" 吗？此操作无法撤销。</div>
          </div>
          <div class="modal-actions">
            <button class="btn btn-cancel" @click="closeDeleteModal">取消</button>
            <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 右侧聊天区（保持不变） -->
    <main class="right-panel">
      <div class="chat-container">
        <div class="chat-messages" ref="messagesContainer">
          <div class="messages-wrapper">
            <div
                v-for="(message, index) in messages"
                :key="index"
                :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']"
            >
              <template v-if="message.role === 'user'">
                <div class="message-content user-content">
                  <div class="message-text">{{ message.content }}</div>
                  <div class="message-time">{{ message.time }}</div>
                </div>
              </template>

              <template v-else>
                <div class="message-content ai-content">
                  <div v-if="message.status_message" class="ai-status-message">
                    <p>{{ message.status_message }}</p>
                    <div v-if="message.status_message !== '用户中止'" class="shimmer"></div>
                  </div>

                  <template v-if="isTestCaseJson(message.content)">
                    <template v-if="Array.isArray(parseTestCase(message.content))">
                      <TestCaseCard
                          v-for="(test, i) in parseTestCase(message.content)"
                          :key="i"
                          :testCase="test"
                      />
                    </template>
                    <template v-else>
                      <TestCaseCard :testCase="parseTestCase(message.content)" />
                    </template>

                    <!-- 新增：导入用例库 按钮（空心纯文字，显示在 TestCaseCard 块的右下方） -->
                    <div class="import-button-container">
                      <button class="import-button" @click="openImport(message)" title="将该 AI 生成的用例导入到用例库">
                        导入用例库
                      </button>
                    </div>
                  </template>

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

        <!-- IMPORT FORM OVERLAY -->
        <transition name="fade">
          <div v-if="isImportVisible" class="import-overlay" @click.self="closeImport">
            <div class="import-card" @click.stop>
              <div class="import-header">
                <div class="import-title">导入用例到用例库</div>
                <div class="import-sub">请选择目标项目与模块；创建者自动填充为当前用户</div>
              </div>

              <div class="import-body">
                <div class="field">
                  <label>项目 <span class="field-required">*</span></label>
                  <select v-model="importForm.projectId" @change="onImportProjectChange">
                    <option value="">-- 请选择项目 --</option>
                    <option v-for="p in projects" :key="p.id || p.projectId || p" :value="p.id ?? p.projectId">
                      {{ p.name ?? p.projectName ?? p.title ?? ('项目 ' + (p.id ?? '')) }}
                    </option>
                  </select>
                </div>

                <div class="field">
                  <label>模块 <span class="field-required">*</span></label>
                  <select v-model="importForm.moduleId" :disabled="!importForm.projectId || modulesLoading">
                    <option value="">-- 请选择模块 --</option>
                    <option v-for="m in modules" :key="m.id || m.moduleId || m._id" :value="m.id ?? m.moduleId">
                      {{ m.name ?? m.moduleName ?? ('模块 ' + (m.id ?? '')) }}
                    </option>
                  </select>
                </div>

                <div class="field">
                  <label>创建者</label>
                  <input type="text" :value="authUser?.id ? (authUser.username || ('user:' + authUser.id)) : '未知用户'" disabled />
                </div>

                <div class="import-preview">
                  <div class="preview-title">预览（将导入的用例）：</div>
                  <div class="preview-list">
                    <div v-for="(t, idx) in importPreview" :key="idx" class="preview-item">
                      <div class="preview-item-title">{{ t.title || t.name || ('用例 ' + (idx+1)) }}</div>
                      <div class="preview-item-meta">期望结果：{{ t.expectedResult || t.expected_result || '-' }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="import-footer">
                <button class="btn btn-cancel import-btn" @click="closeImport" :disabled="importLoading">取消</button>
                <button class="btn btn-confirm import-btn" @click="confirmImport" :disabled="importLoading || !canConfirmImport">
                  {{ importLoading ? '导入中...' : '确认导入' }}
                </button>
              </div>
            </div>
          </div>
        </transition>

        <div class="chat-input-container">
          <!-- 输入框区域：加上 has-switch 类以配合开关样式 -->
          <div class="input-wrapper has-switch">
            <!-- 丝滑开关：左下角 -->
            <div class="usecase-switch" title="开启后每次发送会在请求体内加入 tool_choice='testCase'">
              <label class="switch" aria-label="用例生成开关">
                <input type="checkbox" v-model="useCaseGen" />
                <span class="slider"></span>
              </label>
              <div class="switch-label">
                <div class="label-title">用例生成</div>
                <div class="label-sub" v-text="useCaseGen ? '已启用' : '已关闭'"></div>
              </div>
            </div>

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
          <div class="input-tips">按 Enter 发送，Shift + Enter 换行</div>
        </div>
      </div>
      <div id="toast-container" aria-live="polite" aria-atomic="true"></div>
    </main>
  </div>
</template>

<script>
import { marked } from 'marked'
import TestCaseCard from './TestCaseCard.vue'
import '@/assets/style.css'

marked.setOptions({ breaks: true, gfm: true })

export default {
  name: 'ChatWithSidebar',
  components: { TestCaseCard },

  data() {
    return {
      // messages / chat state
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
      messageCounter: 0,

      // 新增：用例生成开关（false 默认）
      useCaseGen: false,

      // 左侧数据
      conversationNames: [],
      currentConversationUuid: null,
      authUser: {},
      authToken: null,

      // 菜单与悬浮控制
      hoverItemId: null,
      openMenuId: null,
      menuStyle: {}, // { top: 'px', left: 'px', zIndex: 99999 }
      activeAction: null,

      // 创建会话状态
      isCreating: false,

      // 重命名内联编辑状态
      editingId: null,
      editingName: '',

      // 删除模态状态
      deleteModal: {
        show: false,
        item: null
      },

      /* ========== 新增导入相关 state ========== */
      isImportVisible: false,      // 是否显示导入表单
      importLoading: false,        // 提交中
      importForm: {                // 表单字段
        projectId: '',
        moduleId: ''
      },
      projects: [],                // 项目选项
      modules: [],                 // 当前 project 的模块选项
      modulesLoading: false,
      importSourceMessage: null,   // 被导入的 message 对象（包含 content）
    }
  },

  computed: {
    // 供预览使用：把 parseTestCase 的结果标准化为数组
    importPreview() {
      const src = this.importSourceMessage?.content
      if (!src) return []
      try {
        const parsed = JSON.parse(src)
        return Array.isArray(parsed) ? parsed.map(p=>this.normalizeAiCase(p)) : [this.normalizeAiCase(parsed)]
      } catch { return [] }
    },
    canConfirmImport() {
      return !!this.importForm.projectId && !!this.importForm.moduleId && this.importPreview.length > 0
    }
  },

  methods: {
    // ------------------------------
    // 基础帮助函数
    // ------------------------------
    getCurrentTime() {
      const now = new Date()
      return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
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
    // 加载 authUser & names
    // ------------------------------
    async loadAuthUser() {
      try {
        const raw = localStorage.getItem('auth.user')
        if (raw) {
          this.authUser = JSON.parse(raw)
        } else if (window && window.auth && window.auth.user) {
          this.authUser = window.auth.user
        } else {
          this.authUser = { id: 13, username: '23222' }
        }
      } catch (e) {
        console.warn('解析 auth.user 失败，使用默认：', e)
        this.authUser = { id: 13, username: '23222' }
      }
      this.authToken = localStorage.getItem('auth.token') || null
    },

    async loadConversationNames() {
      if (!this.authUser || !this.authUser.id) return
      const uid = this.authUser.id
      try {
        const resp = await fetch(`http://localhost:9090/conversation-names/user/${uid}`, {
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {})
          }
        })
        if (!resp.ok) {
          console.error('获取 conversation-names 失败', resp.status)
          this.conversationNames = []
          return
        }
        const body = await resp.json()
        const data = (body && (body.data ?? body)) || []
        this.conversationNames = Array.isArray(data) ? data : []
        // 如果没有选中会话，则默认选第一个
        if (this.conversationNames.length > 0 && !this.currentConversationUuid) {
          this.onSelectConversation(this.conversationNames[0])
        }
      } catch (err) {
        console.error('loadConversationNames error', err)
        this.conversationNames = []
      }
    },

    // 查找 item
    findById(id) {
      return this.conversationNames.find(c => c.id === id) || null
    },

    onSelectConversation(item) {
      if (!item) return
      const uuid = item.conversationUuid || item.conversation_uuid
      if (!uuid) {
        console.warn('selected conversation item missing conversationUuid', item)
        return
      }
      this.currentConversationUuid = uuid
      this.loadConversationByUuid(uuid)
      this.closeMenu()
    },

    async loadConversationByUuid(uuid) {
      if (!uuid) return
      try {
        const resp = await fetch(`http://localhost:9090/conversations/by-uuid/${encodeURIComponent(uuid)}`, {
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {})
          }
        })
        if (!resp.ok) {
          console.error('加载 conversation 失败', resp.status)
          return
        }
        const body = await resp.json()
        const payload = (body && (body.data ?? body)) || {}
        let msgs = payload.messages ?? payload.messages
        if (typeof msgs === 'string') {
          try { msgs = JSON.parse(msgs) } catch { msgs = [] }
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
            status_message: ''
          }
        })

        this.messages = mapped.length > 0 ? mapped : [{
          role: 'ai',
          content: '您好！我是TFBots!请问有什么可以帮您？\n\n我可以帮您：\n- 生成用例\n- 编写代码\n- 回答问题\n- 提供建议',
          time: this.getCurrentTime(),
          status_message: ''
        }]
        this.$nextTick(this.scrollToBottom)
      } catch (err) {
        console.error('loadConversationByUuid error', err)
      }
    },

    // ------------------------------
    // 新对话：向后端创建，会后刷新列表并选中后端创建的会话
    // ------------------------------
    async createNewConversation() {
      if (this.isCreating) return
      this.isCreating = true
      try {
        const convResp = await fetch('http://localhost:9090/conversations', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {})
          },
          body: JSON.stringify({
            userId: this.authUser.id,
            assistantId: 'gpt-5',
          })
        })
        if (!convResp.ok) {
          const t = await convResp.text().catch(()=>'')
          console.error('create conversation failed', convResp.status, t)
          this.showToast('创建会话失败', 'error')
          return
        }

        // 解析创建结果，尽量从后端响应里取到 conversationUuid 或 id
        let created = null
        try {
          const body = await convResp.json().catch(()=>null)
          created = (body && (body.data ?? body)) || body || null
        } catch (e) {
          console.warn('解析 create 会话响应失败', e)
        }

        // 重新加载会话名列表（后端已在 server 插入 conversation_name）
        await this.loadConversationNames()

        // 尝试选中刚创建的会话
        let target = null
        const createdUuid = created?.conversationUuid ?? created?.conversation_uuid ?? created?.conversationUuid
        if (createdUuid) {
          target = this.conversationNames.find(c => (c.conversationUuid === createdUuid) || (c.conversation_uuid === createdUuid))
        }
        if (!target && created?.id) {
          target = this.conversationNames.find(c => c.id === created.id)
        }

        // 如果上面没找到，尝试按最近 created_at（后端返回的列表一般按时间排序，选第一个通常是刚创建的）
        if (!target && this.conversationNames.length > 0) {
          target = this.conversationNames[0]
        }

        if (target) {
          this.onSelectConversation(target)
        } else {
          console.warn('创建成功但未能在拉取的列表中找到对应项')
        }
      } catch (err) {
        console.error('createNewConversation error', err)
        this.showToast('创建新对话出错', 'error')
      } finally {
        this.isCreating = false
      }
    },

    // ------------------------------
    // 重命名（从 menu 点击触发，进入内联编辑）
    // ------------------------------
    onRenameClick(item) {
      if (!item) return
      this.closeMenu()
      this.editingId = item.id
      this.editingName = item.name || ''
      this.$nextTick(() => {
        // focus input if available
        const el = this.$refs.renameInput
        if (el && el.focus) {
          // $refs.renameInput may be an array when multiple inputs exist; prefer last
          if (Array.isArray(el)) el[el.length - 1].focus()
          else el.focus()
          // move cursor to end
          const val = this.editingName
          el.setSelectionRange && el.setSelectionRange(val.length, val.length)
        }
      })
    },

    async confirmRename(item) {
      if (!item || this.editingId !== item.id) {
        this.editingId = null
        this.editingName = ''
        return
      }
      const trimmed = String(this.editingName || '').trim()
      if (trimmed.length === 0) {
        this.showToast('名称不能为空', 'warning')
        return
      }
      const payload = Object.assign({}, item, { name: trimmed })
      try {
        const resp = await fetch(`http://localhost:9090/conversation-names/${encodeURIComponent(item.id)}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json', ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}) },
          body: JSON.stringify(payload)
        })
        if (!resp.ok) {
          const txt = await resp.text().catch(()=>'')
          console.error('rename failed', resp.status, txt)
          this.showToast('重命名失败', 'error')
          return
        }
        const found = this.conversationNames.find(c => c.id === item.id)
        if (found) found.name = trimmed
      } catch (err) {
        console.error('rename error', err)
        this.showToast('重命名出错', 'error')
      } finally {
        this.editingId = null
        this.editingName = ''
      }
    },

    cancelRename() {
      this.editingId = null
      this.editingName = ''
    },

    // ------------------------------
    // 删除：打开美观模态 -> 确认删除
    // ------------------------------
    onDeleteClick(item) {
      if (!item) return
      this.closeMenu()
      this.activeAction = { id: item.id, action: 'delete' }
      this.deleteModal = { show: true, item }
    },

    closeDeleteModal() {
      this.deleteModal = { show: false, item: null }
      this.activeAction = null
    },

    async confirmDelete() {
      const item = this.deleteModal.item
      if (!item) { this.closeDeleteModal(); return }
      try {
        const resp = await fetch(`http://localhost:9090/conversation-names/${encodeURIComponent(item.id)}`, {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json', ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}) }
        })
        if (!resp.ok) {
          const txt = await resp.text().catch(()=>'')
          console.error('delete failed', resp.status, txt)
          this.showToast('删除失败', 'error'); return
        }
        const idx = this.conversationNames.findIndex(c => c.id === item.id)
        if (idx !== -1) this.conversationNames.splice(idx, 1)

        if (this.currentConversationUuid === (item.conversationUuid || item.conversation_uuid)) {
          if (this.conversationNames.length > 0) {
            const next = this.conversationNames[Math.max(0, idx - 1)]
            if (next) this.onSelectConversation(next)
            else this.currentConversationUuid = null
          } else {
            this.currentConversationUuid = null
            this.messages = [{
              role: 'ai',
              content: '您好！我是TFBots!请问有什么可以帮您？\n\n我可以帮您：\n- 生成用例\n- 编写代码\n- 回答问题\n- 提供建议',
              time: this.getCurrentTime(),
              status_message: ''
            }]
          }
        }
        this.closeDeleteModal()
      } catch (err) {
        console.error('delete error', err)
        this.showToast('删除出错', 'error')
      }
    },

    // ------------------------------
    // 右侧聊天相关（保持你原来逻辑）
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
      try { return JSON.parse(text) } catch { return null }
    },
    generateMessageId(prefix = 'm') {
      this.messageCounter = (this.messageCounter || 0) + 1
      return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2,6)}-${this.messageCounter}`
    },
    async appendMessageToServer(messageObj) {
      if (!this.currentConversationUuid) { console.warn('appendMessageToServer: no currentConversationUuid, skip append'); return }
      try {
        await fetch(`http://localhost:9090/conversations/${encodeURIComponent(this.currentConversationUuid)}/append`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}) },
          body: JSON.stringify(messageObj)
        })
      } catch (err) { console.error('appendMessageToServer error', err) }
    },

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
        // 这里：如果 useCaseGen 为 true，就在 body 中额外传入 tool_choice: 'testCase'
        const requestBody = {
          messages: this.messages.map(msg => ({
            role: msg.role,
            content: msg.role === 'user' ? msg.content + '/no_think' : msg.content
          })),
          ...(this.currentConversationUuid ? { conversationUuid: this.currentConversationUuid } : {}),
          // 当 useCaseGen 为 true 时，添加 tool_choice 字段；为 false 则不添加。
          ...(this.useCaseGen ? { tool_choice: 'testCase' } : {})
        }

        const response = await fetch('http://localhost:9090/api/chat/normal', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
          body: JSON.stringify(requestBody),
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

    // ------------------------------
    // 菜单控制：计算 fixed 位置并打开
    // ------------------------------
    toggleMenu(item, ev) {
      if (!item) return
      // 如果同一个 id 切换，则关闭
      if (this.openMenuId === item.id) {
        this.closeMenu()
        return
      }

      // 计算目标按钮位置（ev.currentTarget 或 ev.target 的父级）
      const btn = ev.currentTarget || ev.target
      const rect = btn.getBoundingClientRect()

      // 设定 menu 固定位置：在按钮右侧并略上移（从右侧弹上来）
      const offsetX = 8
      const offsetY = -8
      const top = Math.max(8, rect.top + offsetY)
      const left = Math.min(window.innerWidth - 180, rect.right + offsetX)

      this.menuStyle = {
        position: 'fixed',
        top: `${top}px`,
        left: `${left}px`,
        zIndex: 99999
      }
      this.openMenuId = item.id
    },

    closeMenu() {
      this.openMenuId = null
      this.menuStyle = {}
    },

    onDocumentClick(e) {
      const el = e.target
      const isInsidePopup = !!el.closest('.menu-popup')
      const isEllipsis = !!el.closest('.ellipsis-btn')
      if (!isInsidePopup && !isEllipsis) {
        this.closeMenu()
      }
    },

    // 其他 UI helpers
    formatTime(v) {
      try {
        const d = new Date(v)
        if (isNaN(d.getTime())) return String(v)
        const hh = String(d.getHours()).padStart(2, '0')
        const mm = String(d.getMinutes()).padStart(2, '0')
        return `${hh}:${mm}`
      } catch { return String(v) }
    },
    renderMarkdown(text) { return marked(text) },
    autoResize() {
      if (!this.isSending) {
        const textarea = this.$refs.inputArea
        if (!textarea) return
        textarea.style.height = 'auto'
        textarea.style.height = `${textarea.scrollHeight}px`
      }
    },
    scrollToBottom() {
      const container = this.$refs.messagesContainer
      if (container) container.scrollTop = container.scrollHeight
    },

    // 键盘处理：Esc 取消模态或取消重命名
    onKeyDown(e) {
      if (e.key === 'Escape') {
        if (this.deleteModal.show) this.closeDeleteModal()
        if (this.editingId) this.cancelRename()
        if (this.isImportVisible) this.closeImport()
      }
    },

    // 打开导入弹窗（传入当前 message），并加载项目列表（如尚未加载）
    openImport(message) {
      if (!message || !message.content) {
        this.showToast('无可导入的数据', 'error')
        return
      }
      // 解析并校验
      const parsed = this.parseTestCase(message.content)
      if (!parsed) {
        this.showToast('解析用例失败，数据格式不正确', 'error')
        return
      }
      // 保存源消息，打开弹窗
      this.importSourceMessage = message
      this.isImportVisible = true
      this.importForm = { projectId: '', moduleId: '' }
      // load projects if empty
      if (!this.projects || this.projects.length === 0) {
        this.loadProjects().catch(err => console.warn('loadProjects err', err))
      }
      // clear modules
      this.modules = []
    },

    closeImport() {
      if (this.importLoading) return
      this.isImportVisible = false
      this.importForm = { projectId: '', moduleId: '' }
      this.importSourceMessage = null
    },


    showToast(message, type = 'info', duration = 3000) {
      try {
        const container = document.getElementById('toast-container')
        if (!container) return
        const toast = document.createElement('div')
        toast.className = `toast toast-${type}`
        toast.setAttribute('role', 'this.showToast')
        toast.setAttribute('aria-live', 'assertive')

        // inner content
        const icon = document.createElement('span')
        icon.className = 'toast-icon'
        // simple icon by type (you can replace with SVGs)
        const icons = { success: '✔', info: 'ℹ', warning: '⚠', error: '✖' }
        icon.textContent = icons[type] || ''
        const msg = document.createElement('div')
        msg.className = 'toast-message'
        msg.textContent = message

        toast.appendChild(icon)
        toast.appendChild(msg)

        // append and animate
        container.appendChild(toast)
        // force reflow to trigger CSS animations if needed
        void toast.offsetWidth

        // auto remove
        const t = setTimeout(() => {
          toast.classList.add('toast-hide')
          // remove after animation
          setTimeout(() => { try { container.removeChild(toast) } catch(e){} }, 400)
        }, duration)

        // allow click to dismiss early
        toast.addEventListener('click', () => {
          clearTimeout(t)
          toast.classList.add('toast-hide')
          setTimeout(() => { try { container.removeChild(toast) } catch(e){} }, 200)
        })
      } catch (e) {
        // fallback
        console.warn('showToast error', e)
      }
    },

    async loadProjects() {
      try {
        const resp = await fetch(`http://localhost:9090/projects?pageNum=1&pageSize=1000`, {
          headers: { 'Content-Type': 'application/json', ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}) }
        })
        if (!resp.ok) throw new Error(`项目加载失败 ${resp.status}`)
        const body = await resp.json()
        const data = (body && (body.data ?? body)) || {}
        // 支持 data.list 或直接 list
        const list = data.list || body.list || []
        this.projects = Array.isArray(list) ? list : []
      } catch (err) {
        console.error('loadProjects error', err)
        this.projects = []
      }
    },

    // 选中 project 后加载 module
    async onImportProjectChange() {
      const pid = this.importForm.projectId
      this.importForm.moduleId = ''
      this.modules = []
      if (!pid) return
      await this.loadModulesForProject(pid)
    },

    async loadModulesForProject(projectId) {
      this.modulesLoading = true
      try {
        // 使用你给出的接口，尽量请求更多分页以获取全部模块
        const resp = await fetch(`http://localhost:9090/modules?projectId=${encodeURIComponent(projectId)}&pageNum=1&pageSize=1000`, {
          headers: { 'Content-Type': 'application/json', ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {}) }
        })
        if (!resp.ok) throw new Error(`模块加载失败 ${resp.status}`)
        const body = await resp.json()
        const data = (body && (body.data ?? body)) || {}
        const list = data.list || body.list || []
        this.modules = Array.isArray(list) ? list : []
      } catch (err) {
        console.error('loadModulesForProject error', err)
        this.modules = []
      } finally {
        this.modulesLoading = false
      }
    },

    // 把 AI case（任意风格）标准化为方便预览的对象
    normalizeAiCase(item) {
      if (!item) return {}
      return {
        title: item.title || item.name || item.case_title || '',
        expectedResult: item.expected_result || item.expectedResult || '',
        steps: item.steps || [],
        preconditions: item.preconditions || item.pre_conditions || []
      }
    },

    // 构造后端期望的批量 payload，然后 POST 到 /testcases/batch
    async confirmImport() {
      if (!this.canConfirmImport) {
        this.showToast('请先选择项目与模块，并确保数据可用', 'warning')
        return
      }
      if (this.importLoading) return
      // prepare payload
      let parsed = null
      try {
        parsed = this.parseTestCase(this.importSourceMessage.content)
      } catch (e) {
        console.error('parse fail', e)
        this.showToast('解析用例失败', 'error')
        return
      }
      if (!parsed) {
        this.showToast('解析用例失败', 'error')
        return
      }
      const arr = Array.isArray(parsed) ? parsed : [parsed]
      const now = new Date().toISOString()
      const payload = arr.map(item => {
        // preconditions -> string
        let pre = ''
        try {
          const pcs = item.preconditions || item.pre_conditions || []
          pre = Array.isArray(pcs) ? pcs.map(p=>{
            if (typeof p === 'string') return p
            if (typeof p === 'object' && p !== null) {
              return Object.values(p).join('; ')
            }
            return String(p)
          }).join('; ') : (typeof pcs === 'string' ? pcs : '')
        } catch { pre = '' }

        // steps -> ordered text
        let stepsText = ''
        try {
          const sarr = Array.isArray(item.steps) ? item.steps.slice() : []
          sarr.sort((a,b) => (a.step_number || a.step || 0) - (b.step_number || b.step || 0))
          stepsText = sarr.map(s => {
            const num = s.step_number ?? s.step ?? ''
            const act = s.action ?? s.content ?? s.description ?? ''
            return `${num ? num + '. ' : ''}${act}`
          }).join('\n')
        } catch { stepsText = '' }

        return {
          projectId: Number(this.importForm.projectId),
          moduleId: Number(this.importForm.moduleId),
          title: item.title || item.name || ('用例 ' + (item.case_id || item.caseId || '')),
          description: item.description || null,
          preCondition: pre,
          steps: stepsText,
          expectedResult: item.expected_result || item.expectedResult || '',
          status: '新建',
          priority: '中',
          createdBy: this.authUser?.id ?? null,
          createdAt: now,
          updatedAt: now,
          externalKey: item.case_id || item.caseId || null
        }
      })

      // POST payload
      this.importLoading = true
      try {
        const resp = await fetch('http://localhost:9090/testcases/batch', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(this.authToken ? { Authorization: 'Bearer ' + this.authToken } : {})
          },
          body: JSON.stringify(payload)
        })
        if (!resp.ok) {
          const text = await resp.text().catch(()=> '')
          console.error('import failed', resp.status, text)
          this.showToast('导入失败: ' + (text || resp.status), 'error')
          return
        }
        // success
        this.showToast('导入成功', 'success')
        this.closeImport()
        // 可选：如果需要刷新某些数据表或 UI，可在这里调用对应加载函数
      } catch (err) {
        console.error('confirmImport error', err)
        this.showToast('导入异常，请查看控制台', 'error')
      } finally {
        this.importLoading = false
      }
    },

    /* =======================
       = 新增导入功能方法区 END =
       ======================= */

  },

  mounted() {
    document.addEventListener('click', this.onDocumentClick)
    window.addEventListener('resize', this.closeMenu)
    window.addEventListener('scroll', this.closeMenu, true) // capture scrolling in ancestors
    document.addEventListener('keydown', this.onKeyDown)
    this.$nextTick(this.autoResize)
    this.loadAuthUser().then(() => this.loadConversationNames())
  },

  beforeUnmount() {
    document.removeEventListener('click', this.onDocumentClick)
    window.removeEventListener('resize', this.closeMenu)
    window.removeEventListener('scroll', this.closeMenu, true)
    document.removeEventListener('keydown', this.onKeyDown)
  }
}
</script>

<style scoped>
.left-item-title-edit { width: 100%; }
.rename-input {
  width: 100%;
  font-weight: 600;
  font-size: 14px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid rgba(15,23,42,0.08);
  background: #fff;
  outline: none;
  box-shadow: 0 6px 18px rgba(15,23,42,0.04);
  transition: box-shadow .12s ease, border-color .12s ease;
}
.rename-input:focus {
  border-color: rgba(31,111,235,0.18);
  box-shadow: 0 10px 30px rgba(31,111,235,0.06);
}

/* Modal overlay + center modal */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(24,24,27,0.18);   /* 更透明 */
  backdrop-filter: blur(2px) saturate(110%);
  -webkit-backdrop-filter: blur(2px) saturate(110%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100000; /* 高于 menu-popup */
  padding: 20px;
}

/* 模态框卡片 */
.confirm-modal {
  width: 420px;
  max-width: calc(100% - 40px);
  background: linear-gradient(180deg, #ffffff, #fbfbfc);
  border-radius: 14px;
  box-shadow: 0 20px 50px rgba(10, 18, 40, 0.35);
  overflow: hidden;
  border: 1px solid rgba(15,23,42,0.06);
  transform: translateY(0);
}

/* modal body */
.modal-body {
  padding: 22px 24px;
  color: #111827;
}
.modal-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 8px;
  color: #0f172a;
}
.modal-desc {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

/* actions */
.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 16px 20px 20px;
  background: rgba(245,247,250,0.6);
}
.btn {
  min-width: 92px;
  padding: 10px 14px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  box-shadow: none;
}
.btn-cancel {
  background: #f3f4f6;
  color: #111827;
  border: 1px solid rgba(15,23,42,0.04);
}
.btn-cancel:hover { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(15,23,42,0.06); }
.btn-danger {
  background: #ef4444;
  color: white;
  border: none;
}
.btn-danger:hover { filter: brightness(0.95); transform: translateY(-1px); box-shadow: 0 8px 26px rgba(239,68,68,0.16); }

/* 基本布局（保持你原有样式） */
.chat-layout { display:flex; height:100vh; background:#ffffff; overflow:hidden; }
.left-panel { width:13%; min-width:220px; max-width:320px; box-sizing:border-box; border-right:1px solid rgba(0,0,0,0.04); padding:12px; background:#fbfcfd; display:flex; flex-direction:column; }
.left-card { background:#fff; border-radius:12px; padding:10px; box-shadow:0 6px 18px rgba(15,23,42,0.06); display:flex; flex-direction:column; height:calc(100vh - 30px); overflow:hidden; }
.left-card-header { display:flex; align-items:center; justify-content:space-between; padding:6px 6px 12px; border-bottom:1px solid rgba(0,0,0,0.04); }

/* 标题与新对话按钮排布 */
.title-and-actions { display:flex; flex-direction:column; }
.header-actions { display:flex; align-items:center; gap:8px; }

/* 新对话文字按钮 */
.new-conv-btn {
  background: transparent;
  border: none;
  color: #1f6feb;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background-color .12s ease, color .12s ease;
}
.new-conv-btn:hover { background: rgba(31,111,235,0.06); }

.left-list { overflow-y:auto; padding:8px 2px; display:flex; flex-direction:column; gap:8px; }
.left-item { display:flex; align-items:center; cursor:pointer; padding:10px; border-radius:10px; background: rgba(15,23,42,0.02); border:1px solid rgba(15,23,42,0.03); transition: all .12s ease; position:relative; }
.left-item:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15,23,42,0.04); }
.left-item.active { background: linear-gradient(90deg, rgba(31,111,235,0.06), rgba(255,255,255,0)); border-color: rgba(31,111,235,0.12); }
.left-item-main { display:flex; flex-direction:column; gap:4px; }
.left-item-title { font-weight:600; color:#0f172a; font-size:14px; }
.left-item-sub { font-size:12px; color:#6b7280; }
.left-empty { color:#9ca3af; text-align:center; margin-top:18px; }

/* 三点按钮 */
.ellipsis-btn { position:absolute; right:10px; top:50%; transform:translateY(-50%); width:28px; height:28px; border-radius:6px; display:flex; align-items:center; justify-content:center; color:#6b7280; font-size:14px; cursor:pointer; opacity:0; transition: opacity 160ms ease, background-color 120ms ease, transform 120ms ease; z-index:30; }
.ellipsis-btn.visible { opacity:1; background: rgba(15,23,42,0.03); }
.ellipsis-btn:hover { transform:translateY(-50%) scale(1.03); background: rgba(15,23,42,0.06); }

/* 浮动菜单（fixed）样式 — 更高 z-index 确保最上层 */
.menu-popup {
  width: 176px;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 12px 36px rgba(15,23,42,0.08);
  border: 1px solid rgba(15,23,42,0.06);
  overflow: hidden;
  display:flex;
  flex-direction:column;
  padding:6px;
  will-change: transform, opacity;
  z-index: 99999;
}

/* 菜单项 */
.menu-item { display:flex; align-items:center; gap:10px; padding:8px 10px; border-radius:8px; cursor:pointer; user-select:none; transition: background-color 120ms ease, color 120ms ease; color:#111827; font-size:13px; }
.menu-item .icon { width:18px; text-align:center; font-size:14px; }
.menu-item:hover, .menu-item.selected { background: rgba(15,23,42,0.04); }
.menu-item.delete { color:#ef4444; }
.menu-item.delete:hover { background: rgba(239,68,68,0.06); }
.menu-divider { height:1px; background: rgba(15,23,42,0.06); margin:6px 4px; border-radius:2px; }

.slide-from-right-up-enter-active, .slide-from-right-up-leave-active {
  transition: all 180ms cubic-bezier(.2,.9,.3,1);
}
.slide-from-right-up-enter-from, .slide-from-right-up-leave-to {
  transform: translateX(12px) translateY(8px) scale(0.98);
  opacity: 0;
}
.slide-from-right-up-enter-to, .slide-from-right-up-leave-from {
  transform: translateX(0) translateY(0) scale(1);
  opacity: 1;
}
.chat-layout {
  display: flex;
  height: 100vh;
  background: #ffffff;
  overflow: hidden;
}

/* right panel takes rest */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

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

/* chat area and messages */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  background: #ffffff;
  padding: 0;
  width: 100%;
}
.messages-wrapper {
  padding: 20px 5% 140px; /* 这里把底部 padding 增大为 140px，根据输入框高度调整 */
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
.message-text { font-size: 15px; line-height: 2; }

/* 用户消息居右，AI居左，且限制宽度不超过容器的 80%（防止撑满） */
.user-content { margin-left: auto; display: flex; flex-direction: column; align-items: flex-end; }
.ai-content { margin-right: auto; text-align: left; width: 100%; }

/* 限制两侧气泡最大宽度为 messages-wrapper 宽度的 80%（从左右分别生效） */
.user-message .message-text {
  background: #f0f0f0;
  color: #1a1a1a;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
  display: inline-block;
  max-width: 80%;       /* 用户气泡不超过从右向左的 80% */
  word-break: break-word;
}
.ai-message .message-text {
  color: #1a1a1a;
  padding: 12px 16px;
  border-radius: 8px;
  display: inline-block;
  max-width: 80%;       /* AI 气泡不超过从左向右的 80% */
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15,23,42,0.02);
  word-break: break-word;
}
.message-time { font-size: 11px; color: #999; margin-top: 4px; }

/* ==== 新增：导入按钮样式（空心、纯文字） ==== */
.import-button-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
.import-button {
  border: 1px solid rgba(31,111,235,0.18);
  color: #1f6feb;
  background: transparent;
  padding: 6px 12px;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  font-size: 13px;
  transition: background 120ms ease, transform 120ms ease;
}
.import-button:hover { background: rgba(31,111,235,0.06); transform: translateY(-2px); }

/* ==== 新增：导入弹窗（卡片风格，四周阴影但无模糊） ==== */
.import-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 120000;
  background: rgba(0,0,0,0.06); /* 轻遮罩但无模糊 */
  padding: 20px;
}
.import-card {
  width: 560px;
  max-width: calc(100% - 40px);
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 30px 80px rgba(10,20,40,0.22);
  border: 1px solid rgba(15,23,42,0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.import-header { padding: 18px 20px; border-bottom: 1px solid rgba(15,23,42,0.04); }
.import-title { font-weight: 700; font-size: 16px; color: #0f172a; margin-bottom: 6px; }
.import-sub { color: #6b7280; font-size: 13px; }

.import-body { padding: 14px 18px; display:flex; flex-direction:column; gap:12px; }
.field { display:flex; flex-direction:column; gap:8px; }
.field label { font-size: 13px; color: #111827; font-weight: 600; }
.field input[type="text"], .field select {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(15,23,42,0.06);
  outline: none;
  font-size: 14px;
  background: #fff;
}
.field input[disabled] { background: #f6f7f9; color:#6b7280; }

.field-required { color: #ef4444; margin-left: 6px; font-weight:600; }

.import-preview { margin-top: 6px; border-radius: 8px; padding: 10px; background: rgba(15,23,42,0.02); border: 1px solid rgba(15,23,42,0.03); }
.preview-title { font-weight:600; font-size: 13px; margin-bottom: 8px; color:#0f172a; }
.preview-list { display:flex; flex-direction:column; gap:8px; max-height: 180px; overflow:auto; padding-right:8px; }
.preview-item { padding:8px; border-radius:8px; background: #fff; border: 1px solid rgba(15,23,42,0.03); display:flex; flex-direction:column; gap:6px; }
.preview-item-title { font-weight:600; }
.preview-item-meta { font-size:12px; color:#6b7280 }

/* footer 按钮 */
.import-footer { display:flex; justify-content:flex-end; gap:12px; padding: 14px 18px; border-top: 1px solid rgba(15,23,42,0.04); background: rgba(255,255,255,0.98); }
.import-btn { min-width:110px; padding:8px 12px; border-radius:10px; font-weight:600; cursor:pointer; border:1px solid rgba(15,23,42,0.06); }
.btn-cancel.import-btn { background: #fff; color:#111827; border-color: rgba(15,23,42,0.06); }
.btn-cancel.import-btn:hover { box-shadow: 0 8px 26px rgba(15,23,42,0.06); transform: translateY(-2px); }
.btn-confirm.import-btn { background: transparent; color:#1f6feb; border: 1px solid rgba(31,111,235,0.18); }
.btn-confirm.import-btn:hover { background: rgba(31,111,235,0.06); transform: translateY(-2px); }

/* -------------- 复用你原样式 -------------- */
/* chat input */
.chat-input-container { padding: 16px 5%; max-width: 1200px; margin: 0 auto; background: #ffffff; border-top: 1px solid #e6e6e6; }
.input-wrapper { display: flex; align-items: flex-end; background: #ffffff; border: 1px solid #e6e6e6; border-radius: 12px; padding: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.05); position: relative; }
/* 当有开关时，textarea 留出左侧空间，避免被覆盖 */
.input-wrapper.has-switch textarea { padding-left: 56px; }

textarea { flex: 1; border: none; outline: none; resize: none; padding: 8px; font-size: 14px; line-height: 1.6; width: 450px; height: 56px; max-height: 200px; min-height: 56px; font-family: inherit; overflow-y: hidden; transition: height .12s ease; }

/* 发送 / 停止按钮 */
.send-button, .stop-button { background: #10a37f; color: white; border: none; border-radius: 6px; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background-color 0.2s; flex-shrink: 0; margin-left: 12px; }
.send-button:hover:not(:disabled) { background: #0d8c6d; }
.send-button:disabled { background: #ccc; cursor: not-allowed; }
.stop-button { background: #f56c6c; }
.stop-button:hover:not(:disabled) { background: #ea7a7a; }
.stop-button:disabled { background: #fab6b6; cursor: not-allowed; }
.input-tips { font-size: 11px; color: #999; margin-top: 8px; text-align: center; }

.usecase-switch {
  position: absolute;
  left: 12px;
  bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  user-select: none;
  z-index: 20;
}

/* switch 本体 */
.switch {
  display: inline-block;
  width: 44px;
  height: 26px;
  position: relative;
}
.switch input {
  opacity: 0;
  width: 0;
  height: 0;
  position: absolute;
}
.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(0,0,0,0.06), rgba(255,255,255,0.02));
  box-shadow: 0 6px 18px rgba(15,23,42,0.06), inset 0 1px 0 rgba(255,255,255,0.2);
  transition: background 220ms cubic-bezier(.2,.9,.3,1), transform 220ms cubic-bezier(.2,.9,.3,1);
}
.slider::after {
  content: "";
  position: absolute;
  left: 4px;
  top: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 6px 16px rgba(12,20,44,0.12);
  transition: transform 220ms cubic-bezier(.2,.9,.3,1);
}

/* checked 状态 */
.switch input:checked + .slider {
  background: linear-gradient(90deg, rgba(16,163,127,0.96), rgba(0,160,122,0.95));
  box-shadow: 0 10px 26px rgba(16,163,127,0.14), inset 0 1px 0 rgba(255,255,255,0.06);
}
.switch input:checked + .slider::after {
  transform: translateX(18px);
}

/* 标签文字 */
.switch-label { display:flex; flex-direction:column; line-height:1; }
.switch-label .label-title { font-size:12px; font-weight:600; color:#0f172a; }
.switch-label .label-sub { font-size:11px; color:#6b7280; }

/* scrollbars */
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

.chat-messages {
  flex: 1;
  overflow-y: auto;
  background: #ffffff;
  padding: 0;
  width: 100%;
}

.messages-wrapper {
  padding: 20px 5% 140px; /* 这里把底部 padding 增大为 140px，根据输入框高度调整 */
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100%;
}

.chat-input-container {
  position: sticky;
  bottom: 0;
  z-index: 50;             /* 提到最上层，防止被消息遮挡 */
  padding: 12px 5%;       /* 与 messages-wrapper 两侧对齐 */
  max-width: 12100px;
  margin: 0 auto;
  background: #ffffff;    /* 确保覆盖在消息上，可根据主题调整 */
  border-top: 1px solid #e6e6e6;
  box-shadow: 0 -6px 18px rgba(15,23,42,0.03); /* 轻微上方阴影，视觉分隔 */
}

html, body, #app { height: 100%; box-sizing: border-box; }

.fade-enter-active, .fade-leave-active { transition: opacity 160ms ease, transform 160ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(6px); }
.fade-enter-to, .fade-leave-from { opacity: 1; transform: translateY(0); }
</style>
