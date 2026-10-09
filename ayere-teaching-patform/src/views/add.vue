<template>
  <div class="chat-page">
    <!-- 左侧栏 -->
    <div class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <button class="new-chat-btn" @click="createNewChat">
          <span class="plus-icon">+</span> 新建对话
        </button>
        <button class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" title="收起/展开">
          {{ sidebarCollapsed ? '▶' : '◀' }}
        </button>
      </div>

      <div class="chat-history">
        <div 
          v-for="chat in chatHistory" 
          :key="chat.id"
          :class="['chat-item', { active: chat.id === currentChatId }]"
          @click="switchChat(chat.id)"
        >
          <span class="chat-icon">💬</span>
          <span class="chat-title">{{ chat.title }}</span>
          <button class="chat-delete" @click.stop="deleteChat(chat.id)" title="删除">×</button>
        </div>
        <div v-if="chatHistory.length === 0" class="no-history">暂无历史对话</div>
      </div>

      <div class="sidebar-footer">
        <div class="setting-item" @click="showSettings = true">
          <span>⚙️</span> 设置
        </div>
        <div class="user-info">
          <!-- 用 label 原生触发文件选择 -->
          <label class="avatar-wrapper" title="点击更换头像">
            <img v-if="userAvatar" :src="userAvatar" class="avatar-img" />
            <span v-else class="avatar-text">{{ username.charAt(0) }}</span>
            <div class="avatar-mask">📷</div>
            <input 
              type="file" 
              id="avatarInput"
              class="hidden-file-input"
              accept="image/*" 
              @change="handleAvatarUpload" 
            />
          </label>
          <div class="user-detail">
            <div class="user-name">{{ username }}</div>
            <div class="user-role">在线</div>
          </div>
          <button class="logout-icon-btn" @click="handleLogout" title="退出登录">🚪</button>
        </div>
      </div>
    </div>

    <!-- 中间主区域 -->
    <div class="main-area">
      <div class="top-bar">
        <span class="current-chat-title">{{ currentChatTitle }}</span>
        <div class="top-bar-right">
          <span class="online-status">● 在线</span>
          <button class="logout-btn" @click="handleLogout">退出登录</button>
        </div>
      </div>

      <div class="message-area" ref="messageArea">
        <div v-if="currentMessages.length === 0" class="empty-state">
          <div class="empty-icon">🤖</div>
          <div class="empty-text">你好，{{ username }}！有什么可以帮你的？</div>
          <div class="empty-suggestions">
            <div class="suggestion" @click="quickAsk('帮我写一段Java代码')">💻 帮我写一段Java代码</div>
            <div class="suggestion" @click="quickAsk('解释一下SpringBoot原理')">📚 解释一下SpringBoot原理</div>
            <div class="suggestion" @click="quickAsk('翻译一段英文')">🌍 翻译一段英文</div>
            <div class="suggestion" @click="quickAsk('帮我做一个学习计划')">📅 帮我做一个学习计划</div>
          </div>
        </div>

        <div 
          v-for="(msg, index) in currentMessages" 
          :key="index"
          :class="['message-row', msg.role]"
        >
          <div class="message-avatar">
            <template v-if="msg.role === 'user'">
              <img v-if="userAvatar" :src="userAvatar" class="msg-avatar-img" />
              <span v-else>{{ username.charAt(0) }}</span>
            </template>
            <template v-else>AI</template>
          </div>
          <div class="message-content">
            <div class="message-name">{{ msg.role === 'user' ? username : 'AI 助手' }}</div>
            
            <div v-if="msg.content" class="message-bubble">{{ msg.content }}</div>
            
            <div v-if="msg.images && msg.images.length" class="message-images">
              <img 
                v-for="(img, i) in msg.images" 
                :key="i" 
                :src="img" 
                class="msg-image"
                @click="previewImage(img)"
              />
            </div>
            
            <div v-if="msg.files && msg.files.length" class="message-files">
              <div v-for="(file, i) in msg.files" :key="i" class="msg-file">
                <span class="file-icon">📄</span>
                <span class="file-name">{{ file.name }}</span>
                <span class="file-size">{{ file.size }}</span>
              </div>
            </div>

            <div v-if="msg.role === 'ai' && msg.content" class="message-actions">
              <button @click="copyMessage(msg.content)" title="复制">📋 复制</button>
              <button @click="regenerate(index)" title="重新生成">🔄 重新生成</button>
            </div>
          </div>
        </div>

        <div v-if="aiLoading" class="message-row ai">
          <div class="message-avatar">AI</div>
          <div class="message-content">
            <div class="message-name">AI 助手</div>
            <div class="message-bubble typing">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div v-if="pendingFiles.length || pendingImages.length" class="pending-preview">
          <div v-for="(file, i) in pendingFiles" :key="'f'+i" class="pending-item">
            <span class="file-icon">📄</span>
            <span class="file-name">{{ file.name }}</span>
            <button class="remove-btn" @click="removePending('file', i)">×</button>
          </div>
          <div v-for="(img, i) in pendingImages" :key="'i'+i" class="pending-item image-item">
            <img :src="img.url" class="pending-thumb" />
            <button class="remove-btn" @click="removePending('image', i)">×</button>
          </div>
        </div>

        <div class="input-row">
          <label class="upload-btn" title="上传文件">
            📎
            <input type="file" ref="fileInput" class="hidden-file-input" @change="handleFileUpload" multiple />
          </label>
          <label class="upload-btn" title="上传图片">
            🖼️
            <input type="file" ref="imageInput" class="hidden-file-input" accept="image/*" @change="handleImageUpload" multiple />
          </label>
          <textarea
            v-model="inputText"
            class="chat-input"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行..."
            @keydown="handleKeydown"
            :disabled="aiLoading"
            rows="1"
            ref="chatInput"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="!canSend || aiLoading">
            {{ aiLoading ? '...' : '发送' }}
          </button>
        </div>
        <div class="input-tip">AI 生成内容仅供参考，请核实重要信息</div>
      </div>
    </div>

    <!-- 设置弹窗 -->
    <div v-if="showSettings" class="settings-modal" @click.self="showSettings = false">
      <div class="settings-panel">
        <div class="settings-header">
          <span>设置</span>
          <button class="close-btn" @click="showSettings = false">×</button>
        </div>
        <div class="settings-body">
          <!-- 头像设置：用 for 关联同一个 input -->
          <div class="setting-row avatar-setting">
            <label>头像</label>
            <div class="avatar-setting-right">
              <label class="setting-avatar" for="avatarInput">
                <img v-if="userAvatar" :src="userAvatar" class="setting-avatar-img" />
                <span v-else class="setting-avatar-text">{{ username.charAt(0) }}</span>
                <div class="setting-avatar-mask">更换</div>
              </label>
              <button v-if="userAvatar" class="text-btn danger" @click="removeAvatar">移除头像</button>
            </div>
          </div>

          <div class="setting-row">
            <label>用户名</label>
            <input 
              v-model="settings.username" 
              class="setting-input" 
              placeholder="请输入用户名"
              @blur="updateUsername"
            />
          </div>

          <div class="setting-row">
            <label>AI 模型</label>
            <select v-model="settings.model" class="setting-select">
              <option value="default">默认模型</option>
              <option value="fast">快速模式</option>
              <option value="deep">深度思考</option>
            </select>
          </div>
          <div class="setting-row">
            <label>温度 (创造性)</label>
            <input type="range" v-model="settings.temperature" min="0" max="1" step="0.1" class="setting-range" />
            <span class="range-value">{{ settings.temperature }}</span>
          </div>
          <div class="setting-row">
            <label>历史记录保存</label>
            <input type="checkbox" v-model="settings.saveHistory" />
          </div>
          <div class="setting-row">
            <label>清空所有历史</label>
            <button class="danger-btn" @click="clearAllHistory">清空</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <div v-if="previewImgUrl" class="image-preview-modal" @click="previewImgUrl = ''">
      <img :src="previewImgUrl" class="preview-full" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      username: '用户',
      userAvatar: '',
      sidebarCollapsed: false,
      showSettings: false,
      previewImgUrl: '',
      currentChatId: null,
      chatHistory: [],
      inputText: '',
      pendingFiles: [],
      pendingImages: [],
      aiLoading: false,
      settings: {
        username: '',
        model: 'default',
        temperature: 0.7,
        saveHistory: true
      }
    }
  },
  computed: {
    currentMessages() {
      const chat = this.chatHistory.find(c => c.id === this.currentChatId)
      return chat ? chat.messages : []
    },
    currentChatTitle() {
      const chat = this.chatHistory.find(c => c.id === this.currentChatId)
      return chat ? chat.title : '新对话'
    },
    canSend() {
      return this.inputText.trim() || this.pendingFiles.length || this.pendingImages.length
    }
  },
  mounted() {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      try {
        const info = JSON.parse(userInfo)
        this.username = info.username || '用户'
        this.userAvatar = info.avatar || ''
        this.settings.username = this.username
      } catch (e) {}
    }
    const savedSettings = localStorage.getItem('ai_settings')
    if (savedSettings) {
      try {
        Object.assign(this.settings, JSON.parse(savedSettings))
      } catch (e) {}
    }
    this.loadHistoryFromStorage()
    if (this.chatHistory.length === 0) {
      this.createNewChat()
    } else {
      this.currentChatId = this.chatHistory[0].id
    }
  },
  methods: {
    handleLogout() {
      if (!confirm('确定要退出登录吗？')) return
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      this.$router.push('/login')
    },

    // 头像上传（label 原生触发，不需要 triggerAvatarUpload）
    handleAvatarUpload(e) {
      const file = e.target.files[0]
      if (!file) return
      if (file.size > 2 * 1024 * 1024) {
        alert('头像图片不能超过 2MB')
        e.target.value = ''
        return
      }
      const reader = new FileReader()
      reader.onload = (ev) => {
        this.userAvatar = ev.target.result
        this.saveUserInfo()
      }
      reader.readAsDataURL(file)
      e.target.value = ''
    },

    removeAvatar() {
      if (!confirm('确定移除头像吗？')) return
      this.userAvatar = ''
      this.saveUserInfo()
    },

    saveUserInfo() {
      let userInfo = {}
      try {
        userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      } catch (e) {}
      userInfo.username = this.username
      userInfo.avatar = this.userAvatar
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    updateUsername() {
      const name = this.settings.username.trim()
      if (name && name !== this.username) {
        this.username = name
        this.saveUserInfo()
      } else {
        this.settings.username = this.username
      }
    },

    saveSettings() {
      localStorage.setItem('ai_settings', JSON.stringify(this.settings))
    },

    createNewChat() {
      const id = Date.now().toString()
      const newChat = {
        id,
        title: '新对话 ' + (this.chatHistory.length + 1),
        messages: [],
        createTime: new Date().toLocaleString()
      }
      this.chatHistory.unshift(newChat)
      this.currentChatId = id
      this.saveHistoryToStorage()
    },

    switchChat(id) {
      this.currentChatId = id
    },

    deleteChat(id) {
      if (!confirm('确定删除这个对话吗？')) return
      this.chatHistory = this.chatHistory.filter(c => c.id !== id)
      if (this.currentChatId === id) {
        this.currentChatId = this.chatHistory.length > 0 ? this.chatHistory[0].id : null
        if (!this.currentChatId) this.createNewChat()
      }
      this.saveHistoryToStorage()
    },

    clearAllHistory() {
      if (!confirm('确定清空所有历史对话吗？此操作不可恢复！')) return
      this.chatHistory = []
      this.createNewChat()
      this.showSettings = false
    },

    quickAsk(text) {
      this.inputText = text
      this.sendMessage()
    },

    handleFileUpload(e) {
      Array.from(e.target.files).forEach(file => {
        this.pendingFiles.push({
          name: file.name,
          size: this.formatFileSize(file.size),
          file: file
        })
      })
      e.target.value = ''
    },

    handleImageUpload(e) {
      Array.from(e.target.files).forEach(file => {
        const reader = new FileReader()
        reader.onload = (ev) => {
          this.pendingImages.push({ url: ev.target.result, name: file.name, file: file })
        }
        reader.readAsDataURL(file)
      })
      e.target.value = ''
    },

    removePending(type, index) {
      type === 'file' ? this.pendingFiles.splice(index, 1) : this.pendingImages.splice(index, 1)
    },

    formatFileSize(bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },

    handleKeydown(e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        this.sendMessage()
      }
    },

    async sendMessage() {
      if (!this.canSend || this.aiLoading) return
      if (!this.currentChatId) this.createNewChat()

      const chat = this.chatHistory.find(c => c.id === this.currentChatId)
      if (!chat) return

      const userMsg = {
        role: 'user',
        content: this.inputText.trim(),
        images: this.pendingImages.map(img => img.url),
        files: this.pendingFiles.map(f => ({ name: f.name, size: f.size }))
      }
      chat.messages.push(userMsg)

      if (chat.messages.length === 1 && userMsg.content) {
        chat.title = userMsg.content.slice(0, 20) + (userMsg.content.length > 20 ? '...' : '')
      }

      const textToSend = this.inputText.trim()
      const filesToSend = [...this.pendingFiles]
      const imagesToSend = [...this.pendingImages]
      this.inputText = ''
      this.pendingFiles = []
      this.pendingImages = []
      this.aiLoading = true
      this.scrollToBottom()

      try {
        // TODO: 接入 AI 接口
        await new Promise(resolve => setTimeout(resolve, 1000))
        let reply = '（AI接口待接入）已收到你的消息'
        if (textToSend) reply += '\n文本：' + textToSend
        if (filesToSend.length) reply += '\n文件：' + filesToSend.map(f => f.name).join('、')
        if (imagesToSend.length) reply += '\n图片：' + imagesToSend.length + '张'
        chat.messages.push({ role: 'ai', content: reply })
      } catch (err) {
        chat.messages.push({ role: 'ai', content: '抱歉，服务暂时不可用，请稍后重试。' })
      } finally {
        this.aiLoading = false
        this.saveHistoryToStorage()
        this.scrollToBottom()
      }
    },

    regenerate(index) {
      const chat = this.chatHistory.find(c => c.id === this.currentChatId)
      if (!chat) return
      chat.messages.splice(index)
      this.saveHistoryToStorage()
    },

    copyMessage(text) {
      navigator.clipboard.writeText(text).then(() => alert('已复制到剪贴板'))
    },

    previewImage(url) {
      this.previewImgUrl = url
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.messageArea
        if (el) el.scrollTop = el.scrollHeight
      })
    },

    saveHistoryToStorage() {
      if (!this.settings.saveHistory) return
      try {
        localStorage.setItem('ai_chat_history', JSON.stringify(this.chatHistory))
      } catch (e) { console.warn('历史记录保存失败', e) }
    },

    loadHistoryFromStorage() {
      try {
        const data = localStorage.getItem('ai_chat_history')
        if (data) this.chatHistory = JSON.parse(data)
      } catch (e) { console.warn('历史记录加载失败', e) }
    }
  },
  watch: {
    'settings.model'() { this.saveSettings() },
    'settings.temperature'() { this.saveSettings() },
    'settings.saveHistory'() { this.saveSettings() }
  }
}
</script>

<style scoped>
.chat-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
}

/* 隐藏的文件 input：用这个替代 display:none */
.hidden-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}

/* 左侧栏 */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: #202123;
  display: flex;
  flex-direction: column;
  transition: all 0.3s;
  overflow: hidden;
}
.sidebar.collapsed {
  width: 0;
  min-width: 0;
}
.sidebar-header {
  padding: 14px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.new-chat-btn {
  flex: 1;
  padding: 10px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}
.new-chat-btn:hover { background: rgba(255,255,255,0.18); }
.plus-icon { font-weight: bold; margin-right: 4px; }
.collapse-btn {
  width: 32px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.chat-history::-webkit-scrollbar { width: 4px; }
.chat-history::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.2); border-radius: 2px; }

.chat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #ccc;
  font-size: 13px;
  margin-bottom: 2px;
  transition: background 0.15s;
}
.chat-item:hover { background: rgba(255,255,255,0.08); }
.chat-item.active { background: rgba(255,255,255,0.15); color: #fff; }
.chat-icon { font-size: 14px; }
.chat-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-delete {
  display: none;
  background: none;
  border: none;
  color: #999;
  font-size: 16px;
  cursor: pointer;
  padding: 0 4px;
}
.chat-item:hover .chat-delete { display: block; }
.chat-delete:hover { color: #ff4d4f; }
.no-history {
  text-align: center;
  color: #666;
  font-size: 13px;
  padding: 30px 10px;
}

.sidebar-footer {
  border-top: 1px solid rgba(255,255,255,0.1);
  padding: 10px;
}
.setting-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  color: #ccc;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
}
.setting-item:hover { background: rgba(255,255,255,0.08); color: #fff; }
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  color: #fff;
}

/* 头像：label 代替 div，加 cursor */
.avatar-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-text {
  font-size: 15px;
  font-weight: bold;
}
.avatar-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-wrapper:hover .avatar-mask { opacity: 1; }

.user-detail { flex: 1; }
.user-name { font-size: 14px; font-weight: 500; }
.user-role { font-size: 11px; color: #4ade80; }
.logout-icon-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}
.logout-icon-btn:hover { background: rgba(255,255,255,0.1); }

/* 中间主区域 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-width: 0;
}

.top-bar {
  height: 56px;
  min-height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.current-chat-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.online-status {
  font-size: 12px;
  color: #4ade80;
}
.logout-btn {
  padding: 7px 16px;
  font-size: 13px;
  color: #fff;
  background: #ff4d4f;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.logout-btn:hover { background: #e64547; }

/* 消息区 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 0;
}
.message-area::-webkit-scrollbar { width: 6px; }
.message-area::-webkit-scrollbar-thumb { background: #ddd; border-radius: 3px; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}
.empty-icon { font-size: 56px; margin-bottom: 16px; }
.empty-text { font-size: 20px; margin-bottom: 24px; color: #666; }
.empty-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  max-width: 600px;
  padding: 0 20px;
}
.suggestion {
  padding: 10px 18px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
}
.suggestion:hover {
  background: #667eea;
  color: #fff;
  border-color: #667eea;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding: 0 24px;
}
.message-row.user { flex-direction: row-reverse; }
.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
  color: #fff;
  flex-shrink: 0;
  overflow: hidden;
}
.message-row.ai .message-avatar {
  background: linear-gradient(135deg, #10a37f, #0d8a6a);
}
.message-row.user .message-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
}
.msg-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.message-content { max-width: 75%; }
.message-row.user .message-content { text-align: right; }
.message-name {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.message-bubble {
  display: inline-block;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-row.ai .message-bubble {
  background: #f7f7f8;
  color: #333;
  border: 1px solid #eee;
}
.message-row.user .message-bubble {
  background: #667eea;
  color: #fff;
}
.message-bubble.typing {
  display: flex;
  gap: 4px;
  padding: 16px;
}
.message-bubble.typing .dot {
  width: 8px;
  height: 8px;
  background: #999;
  border-radius: 50%;
  animation: bounce 1.4s infinite;
}
.message-bubble.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.message-bubble.typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.message-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.message-row.user .message-images { justify-content: flex-end; }
.msg-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  object-fit: cover;
  border: 1px solid #eee;
}
.message-files { margin-top: 8px; }
.msg-file {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #f0f0f0;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
}
.file-name { font-weight: 500; }
.file-size { color: #999; font-size: 12px; }
.message-actions {
  margin-top: 8px;
  display: flex;
  gap: 12px;
}
.message-actions button {
  background: none;
  border: none;
  color: #999;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.message-actions button:hover { background: #f0f0f0; color: #333; }

/* 底部输入区 */
.input-area {
  border-top: 1px solid #eee;
  padding: 16px 24px 20px;
  background: #fff;
}
.pending-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.pending-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 12px;
  position: relative;
}
.pending-item.image-item { padding: 4px; }
.pending-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}
.remove-btn {
  background: none;
  border: none;
  color: #999;
  font-size: 14px;
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
}
.remove-btn:hover { color: #ff4d4f; }

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: #f7f7f8;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 10px 12px;
  transition: border-color 0.2s;
  max-width: 800px;
  margin: 0 auto;
}
.input-row:focus-within { border-color: #667eea; }
.upload-btn {
  cursor: pointer;
  font-size: 20px;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.2s;
  user-select: none;
  position: relative;
}
.upload-btn:hover { background: rgba(0,0,0,0.06); }
.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  max-height: 120px;
  font-family: inherit;
  color: #333;
}
.chat-input::placeholder { color: #aaa; }
.send-btn {
  padding: 8px 20px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) { background: #5568d3; }
.send-btn:disabled { background: #ccc; cursor: not-allowed; }
.input-tip {
  text-align: center;
  font-size: 11px;
  color: #bbb;
  margin-top: 8px;
}

/* 设置弹窗 */
.settings-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.settings-panel {
  width: 440px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.settings-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}
.close-btn {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #999;
  line-height: 1;
}
.settings-body { padding: 20px; }
.setting-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  font-size: 14px;
  color: #333;
}
.setting-row label { min-width: 100px; }
.setting-select {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}
.setting-input {
  flex: 1;
  padding: 7px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
}
.setting-input:focus { border-color: #667eea; }
.setting-range { flex: 1; }
.range-value { min-width: 30px; text-align: center; color: #666; }
.danger-btn {
  padding: 6px 14px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.danger-btn:hover { background: #e64547; }
.text-btn {
  background: none;
  border: none;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
}
.text-btn.danger { color: #ff4d4f; }
.text-btn.danger:hover { text-decoration: underline; }

/* 头像设置行 */
.avatar-setting {
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}
.avatar-setting-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.setting-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.setting-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.setting-avatar-text {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
}
.setting-avatar-mask {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.55);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.2s;
}
.setting-avatar:hover .setting-avatar-mask { opacity: 1; }

/* 图片预览 */
.image-preview-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  cursor: zoom-out;
}
.preview-full {
  max-width: 90%;
  max-height: 90%;
  border-radius: 8px;
}
</style>