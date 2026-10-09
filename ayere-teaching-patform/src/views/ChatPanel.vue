<template>
  <div class="chat">
    <div class="chat-messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="empty">
        <div class="empty-icon">🤖</div>
        <div class="empty-title">题目拆解 Agent</div>
        <div class="empty-desc">输入一道题，或上传题目图片，我会帮你拆解为「简单 / 中等 / 困难」三级子题目</div>
      </div>

      <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
        <div class="avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble">
          <template v-if="m.role === 'user'">
            <img v-if="m.image" :src="m.image" class="user-img" />
            <div v-if="m.content" class="user-text">{{ m.content }}</div>
          </template>
          <template v-else>
            <div v-if="m.loading" class="typing">正在拆解题目…</div>
            <div v-else-if="m.error" class="err">{{ m.error }}</div>
            <div v-else-if="m.detail" class="result">
              <div v-if="m.detail.knowledgePoints && m.detail.knowledgePoints.length" class="kp">
                <el-tag
                  v-for="kp in m.detail.knowledgePoints"
                  :key="kp.id"
                  class="kp-tag"
                  size="small"
                  :type="tagType(kp.difficulty)"
                >
                  {{ diffLabel(kp.difficulty) }} · {{ kp.name }}
                </el-tag>
              </div>
              <div v-if="m.detail.subQuestions && m.detail.subQuestions.length" class="levels">
                <div v-for="sq in m.detail.subQuestions.filter(s => s.content)" :key="sq.id" class="level">
                  <div class="level-title">{{ diffLabel(sq.difficulty) }}题</div>
                  <div class="level-row"><b>题目：</b><span class="math" v-html="renderMath(sq.content)"></span></div>
                  <div v-if="sq.solution" class="level-row"><b>解法：</b><span class="math" v-html="renderMath(sq.solution)"></span></div>
                  <div v-if="sq.answer" class="level-row"><b>答案：</b><span class="math" v-html="renderMath(sq.answer)"></span></div>
                </div>
              </div>
              <div v-if="extractOutlineTopics(m.detail.question?.aiResult).length" class="outline-section">
                <div class="block-title">知识点大纲</div>
                <div class="outline-topics">
                  <el-tag
                    v-for="(t, i) in extractOutlineTopics(m.detail.question?.aiResult)"
                    :key="i"
                    class="outline-tag"
                    :type="outlineLoading[t] ? 'info' : 'warning'"
                    effect="plain"
                    @click="loadOutline(m.detail.question.id, t)"
                  >
                    {{ outlineLoading[t] ? t + '...' : t }}
                  </el-tag>
                </div>
                <div v-for="(t, i) in outlineData" :key="'o'+i" class="outline-detail">
                  <div class="outline-detail-title">{{ t.topic }}</div>
                  <div class="outline-detail-body" v-html="renderMath(t.content)"></div>
                </div>
              </div>
            </div>
            <div v-else class="err">无返回结果</div>
          </template>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <input
        ref="fileRef"
        type="file"
        accept="image/*"
        style="display: none"
        @change="onFileChange"
      />
      <el-input
        v-model="subject"
        placeholder="科目"
        class="subject-input"
        clearable
      />
      <el-button circle title="上传题目图片" @click="pickImage">
        <el-icon><Picture /></el-icon>
      </el-button>
      <el-input
        v-model="input"
        placeholder="输入题目，回车发送"
        :disabled="loading"
        @keyup.enter="send"
      />
      <el-button type="primary" :loading="loading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { currentUser, uploadText, uploadImage, fetchOutline } from '../api/index'
import { renderMath, extractOutlineTopics } from '../utils/math'

const user = currentUser() || {}

const messages = ref([])
const outlineLoading = ref({})  // { topic: true/false }
const outlineData = ref([])      // [{ topic, content }]

const loadOutline = async (questionId, topic) => {
  if (outlineData.value.find((o) => o.topic === topic)) return
  outlineLoading.value[topic] = true
  try {
    const content = await fetchOutline(questionId, topic)
    outlineData.value.push({ topic, content: content || '无内容' })
  } catch (e) {
    ElMessage.error(e.message || '知识点生成失败')
  } finally {
    outlineLoading.value[topic] = false
  }
}
const input = ref('')
const subject = ref('')
const loading = ref(false)
const fileRef = ref(null)
const messagesRef = ref(null)
let pendingFile = null

const diffLabel = (d) => ({ 基础: '简单', 进阶: '中等', 挑战: '困难' }[d] || d || '未知难度')
const tagType = (d) => ({ 基础: 'success', 进阶: 'warning', 挑战: 'danger' }[d] || 'info')

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}

const pickImage = () => fileRef.value && fileRef.value.click()

const onFileChange = (e) => {
  const f = e.target.files && e.target.files[0]
  if (f) pendingFile = f
  e.target.value = ''
}

const send = async () => {
  const text = input.value.trim()
  const hasFile = !!pendingFile
  if (!text && !hasFile) {
    ElMessage.warning('请输入题目或上传图片')
    return
  }
  if (loading.value) return

  const imageUrl = hasFile ? URL.createObjectURL(pendingFile) : ''
  messages.value.push({ role: 'user', content: text, image: imageUrl })
  input.value = ''

  const assistant = { role: 'assistant', loading: true }
  messages.value.push(assistant)
  loading.value = true
  scrollToBottom()

  try {
    let detail
    if (hasFile) {
      const fd = new FormData()
      fd.append('file', pendingFile)
      fd.append('uploaderId', user.id || '')
      fd.append('uploaderName', user.username || '匿名')
      if (subject.value.trim()) fd.append('subject', subject.value.trim())
      detail = await uploadImage(fd)
    } else {
      detail = await uploadText({
        uploaderId: user.id || null,
        uploaderName: user.username || '匿名',
        subject: subject.value.trim(),
        sourceType: 'TEXT',
        text,
        originalAnswer: ''
      })
    }
    assistant.loading = false
    assistant.detail = detail
  } catch (e) {
    assistant.loading = false
    assistant.error = e.message || '出题失败，请稍后重试'
  } finally {
    loading.value = false
    pendingFile = null
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.empty {
  margin-top: 80px;
  text-align: center;
  color: #909399;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #555;
  margin-bottom: 8px;
}
.empty-desc {
  font-size: 13px;
}
.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.msg.user {
  flex-direction: row-reverse;
}
.avatar {
  flex: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #667eea;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.msg.user .avatar {
  background: #67c23a;
}
.bubble {
  max-width: 72%;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 12px 16px;
  line-height: 1.7;
  color: #333;
}
.msg.user .bubble {
  background: #ecf5ff;
}
.user-img {
  max-width: 260px;
  max-height: 200px;
  border-radius: 6px;
  display: block;
  margin-bottom: 8px;
}
.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.typing {
  color: #909399;
  font-size: 14px;
}
.err {
  color: #f56c6c;
  font-size: 14px;
}
.kp {
  margin-bottom: 10px;
}
.kp-tag {
  margin: 0 6px 6px 0;
}
.level {
  padding: 10px 0;
  border-top: 1px dashed #eee;
}
.level:first-child {
  border-top: none;
}
.level-title {
  font-weight: 600;
  color: #667eea;
  margin-bottom: 4px;
}
.level-row {
  margin-bottom: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.outline-section {
  margin-top: 12px;
}
.outline-topics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.outline-tag {
  cursor: pointer;
}
.outline-detail {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f9f9f9;
  border-radius: 6px;
}
.outline-detail-title {
  font-weight: 600;
  color: #667eea;
  margin-bottom: 6px;
}
.outline-detail-body {
  white-space: pre-wrap;
  word-break: break-word;
}
.chat-input {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #eee;
}
.subject-input {
  width: 110px;
}
</style>