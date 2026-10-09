<template>
  <div class="app">
    <div class="topbar">
      <span class="brand">智能题库系统</span>
      <div class="right">
        <el-radio-group v-model="mode" size="small">
          <el-radio-button value="chat">与 Agent 聊天</el-radio-button>
          <el-radio-button value="bank">我的题库</el-radio-button>
        </el-radio-group>
        <span class="user">你好，{{ username }}（{{ roleLabel }}）</span>
        <el-button size="small" @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="content">
      <keep-alive>
        <ChatPanel v-if="mode === 'chat'" />
      </keep-alive>
      <QuestionList v-if="mode === 'bank'" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { currentUser } from '../api/index'
import ChatPanel from './ChatPanel.vue'
import QuestionList from './QuestionList.vue'

const router = useRouter()
const mode = ref('chat')
const username = ref('用户')
const roleLabel = ref('')

const labels = { student: '学生', teacher: '老师', admin: '管理员' }

onMounted(() => {
  const user = currentUser()
  if (user) {
    username.value = user.username || '用户'
    roleLabel.value = labels[user.role] || ''
  }
})

const logout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}
.topbar {
  height: 56px;
  flex: none;
  background: #fff;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.brand {
  font-size: 18px;
  font-weight: 700;
  color: #667eea;
}
.right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user {
  font-size: 14px;
  color: #333;
}
.content {
  flex: 1;
  overflow: hidden;
}
</style>