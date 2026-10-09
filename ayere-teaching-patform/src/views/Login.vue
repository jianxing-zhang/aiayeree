<template>
  <!-- 全屏渐变背景容器 -->
  <div class="login-wrap">
    <!-- 浮动装饰球 -->
    <div class="orb orb1"></div>
    <div class="orb orb2"></div>
    <div class="orb orb3"></div>

    <!-- 居中卡片 -->
    <div class="login-card">
      <!-- 品牌区 -->
      <div class="brand">
        <div class="brand-logo">AI</div>
        <div class="brand-title">智能题库系统</div>
        <div class="brand-subtitle">AI 驱动的智能出题与知识拆解平台</div>
      </div>
      <!-- Tab标签页 -->
      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 登录 -->
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-width="0"
            class="form-container"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入账号"
                prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                :type="loginPwdType"
                placeholder="请输入密码"
                prefix-icon="Lock"
              >
                <template #suffix>
                  <el-icon @click="toggleLoginPwd">
                    <View v-if="loginPwdType === 'password'" />
                    <Hide v-else />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="submit-btn"
                @click="handleLogin"
                :loading="loginLoading"
              >
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 注册 -->
        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-width="0"
            class="form-container"
          >
            <el-form-item prop="name">
              <el-input
                v-model="registerForm.name"
                placeholder="请设置账号"
                prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                prefix-icon="Phone"
              />
            </el-form-item>
            <el-form-item prop="subject">
              <el-select
                v-model="registerForm.subject"
                placeholder="请选择科目"
                style="width: 100%"
              >
                <el-option label="物理" value="物理" />
                <el-option label="数学" value="数学" />
                <el-option label="化学" value="化学" />
                <el-option label="生物" value="生物" />
                <el-option label="语文" value="语文" />
                <el-option label="英语" value="英语" />
              </el-select>
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请设置密码"
                prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item prop="confirmPwd">
              <el-input
                v-model="registerForm.confirmPwd"
                type="password"
                placeholder="请再次输入密码"
                prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item prop="verifyCode">
              <div style="display: flex; gap: 8px; width: 100%">
                <el-input
                  v-model="registerForm.verifyCode"
                  placeholder="请输入验证码"
                  prefix-icon="Key"
                />
                <el-button
                  type="info"
                  :disabled="codeCooldown > 0"
                  @click="sendCode"
                  style="flex-shrink: 0"
                >
                  {{ codeCooldown > 0 ? `${codeCooldown}s` : '发送' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="submit-btn"
                @click="handleRegister"
                :loading="registerLoading"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 忘记密码 -->
        <el-tab-pane label="忘记密码" name="forget">
          <el-form
            ref="forgetFormRef"
            :model="forgetForm"
            :rules="forgetRules"
            label-width="0"
            class="form-container"
          >
            <el-form-item prop="name">
              <el-input
                v-model="forgetForm.name"
                placeholder="请输入用户名"
                prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input
                v-model="forgetForm.phone"
                placeholder="请输入注册手机号"
                prefix-icon="Phone"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="forgetForm.password"
                type="password"
                placeholder="请设置新密码"
                prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item prop="confirmPwd">
              <el-input
                v-model="forgetForm.confirmPwd"
                type="password"
                placeholder="请再次输入新密码"
                prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item prop="verifyCode">
              <div style="display: flex; gap: 8px; width: 100%">
                <el-input
                  v-model="forgetForm.verifyCode"
                  placeholder="请输入验证码"
                  prefix-icon="Key"
                />
                <el-button
                  type="info"
                  :disabled="forgetCooldown > 0"
                  @click="sendForgetCode"
                  style="flex-shrink: 0"
                >
                  {{ forgetCooldown > 0 ? `${forgetCooldown}s` : '发送' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                class="submit-btn"
                @click="handleForgetPwd"
                :loading="forgetLoading"
              >
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide, Phone, Key } from '@element-plus/icons-vue'
import { sendRegisterCode, register, forgetPwd } from '../api/index'

// 当前激活tab
const activeTab = ref('login')
const router = useRouter()

// ================== 登录表单 ==================
const loginFormRef = ref(null)
const loginLoading = ref(false)
const loginPwdType = ref('password')
const loginForm = ref({
  username: '',
  password: '',
  remember: false
})
const loginRules = ref({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})
// 密码显隐切换
const toggleLoginPwd = () => {
  loginPwdType.value = loginPwdType.value === 'password' ? '' : 'password'
}
// 登录提交
const handleLogin = async () => {
  await loginFormRef.value.validate(async valid => {
    if (!valid) return
    loginLoading.value = true
    try {
      const params = new URLSearchParams({
        name: loginForm.value.username,
        password: loginForm.value.password
      })
      const res = await fetch(`http://localhost:8081/login?${params.toString()}`, {
        method: 'POST'
      })
      const data = await res.json()
      if (data.code === 200) {
        localStorage.setItem('user', JSON.stringify(data.data))
        ElMessage.success('登录成功！')
        router.push('/main')
      } else {
        ElMessage.error(data.msg || '登录失败')
      }
    } catch (e) {
      ElMessage.error('无法连接后端，请确认 user-service(8081) 已启动')
    } finally {
      loginLoading.value = false
    }
  }).catch(() => {
    ElMessage.warning('表单校验失败，请检查输入')
  })
}

// ================== 注册表单 ==================
const registerFormRef = ref(null)
const registerLoading = ref(false)
const codeCooldown = ref(0)
const registerForm = ref({
  name: '',
  phone: '',
  subject: '',
  password: '',
  confirmPwd: '',
  verifyCode: ''
})
// 密码一致性自定义校验
const validateConfirmPwd = (rule, value, callback) => {
  if (value !== registerForm.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}
const registerRules = ref({
  name: [{ required: true, message: '请设置账号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  subject: [{ required: true, message: '请选择科目', trigger: 'change' }],
  password: [{ required: true, message: '请设置密码', trigger: 'blur' }],
  confirmPwd: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ],
  verifyCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
})
// 发送验证码
const sendCode = async () => {
  const phone = registerForm.value.phone
  if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  try {
    const msg = await sendRegisterCode(phone)
    ElMessage.success(msg || '验证码已发送')
    codeCooldown.value = 60
    const timer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  }
}
// 注册提交
const handleRegister = async () => {
  await registerFormRef.value.validate(async valid => {
    if (!valid) return
    registerLoading.value = true
    try {
      const msg = await register({
        name: registerForm.value.name,
        phone: registerForm.value.phone,
        password: registerForm.value.password,
        subject: registerForm.value.subject,
        verifyCode: registerForm.value.verifyCode
      })
      ElMessage.success(msg || '注册成功，请登录！')
      activeTab.value = 'login'
    } catch (e) {
      ElMessage.error(e.message || '注册失败')
    } finally {
      registerLoading.value = false
    }
  }).catch(() => {
    ElMessage.warning('表单校验失败，请检查输入')
  })
}

// ================== 忘记密码表单 ==================
const forgetFormRef = ref(null)
const forgetLoading = ref(false)
const forgetCooldown = ref(0)
const forgetForm = ref({
  name: '',
  phone: '',
  password: '',
  confirmPwd: '',
  verifyCode: ''
})
const validateForgetConfirmPwd = (rule, value, callback) => {
  if (value !== forgetForm.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}
const forgetRules = ref({
  name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请设置新密码', trigger: 'blur' }],
  confirmPwd: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateForgetConfirmPwd, trigger: 'blur' }
  ],
  verifyCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
})
// 发送验证码（忘记密码）
const sendForgetCode = async () => {
  const phone = forgetForm.value.phone
  if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  try {
    const msg = await sendRegisterCode(phone)
    ElMessage.success(msg || '验证码已发送')
    forgetCooldown.value = 60
    const timer = setInterval(() => {
      forgetCooldown.value--
      if (forgetCooldown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    ElMessage.error(e.message || '发送失败')
  }
}
// 重置密码
const handleForgetPwd = async () => {
  await forgetFormRef.value.validate(async valid => {
    if (!valid) return
    forgetLoading.value = true
    try {
      const msg = await forgetPwd({
        name: forgetForm.value.name,
        phone: forgetForm.value.phone,
        password: forgetForm.value.password,
        verifyCode: forgetForm.value.verifyCode
      })
      ElMessage.success(msg || '密码重置成功，请登录！')
      activeTab.value = 'login'
    } catch (e) {
      ElMessage.error(e.message || '重置失败')
    } finally {
      forgetLoading.value = false
    }
  }).catch(() => {
    ElMessage.warning('表单校验失败，请检查输入')
  })
}
</script>

<style scoped>
.login-wrap {
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #0c1e3d 0%, #16407a 50%, #0c1e3d 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 20px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

/* 浮动装饰球 */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.25;
  animation: float 12s ease-in-out infinite;
}
.orb1 { width: 300px; height: 300px; background: #06b6d4; top: -60px; left: -60px; }
.orb2 { width: 400px; height: 400px; background: #3b82f6; bottom: -100px; right: -80px; animation-delay: -4s; }
.orb3 { width: 250px; height: 250px; background: #22d3ee; top: 40%; right: 10%; animation-delay: -8s; }

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(0.95); }
}

.login-card {
  width: 100%;
  max-width: 440px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1);
  padding: 36px 32px;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

/* 品牌区 */
.brand {
  text-align: center;
  margin-bottom: 24px;
}
.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 1px;
  box-shadow: 0 8px 20px rgba(6, 182, 212, 0.35);
  margin-bottom: 12px;
}
.brand-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 4px;
}
.brand-subtitle {
  font-size: 13px;
  color: #999;
  letter-spacing: 0.5px;
}

.login-tabs {
  --el-tabs-header-height: 44px;
}
:deep(.el-tabs__header) {
  margin-bottom: 8px;
}
:deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s;
}
:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #06b6d4, #3b82f6);
  height: 3px;
  border-radius: 3px;
}
:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #eee;
}

.form-container {
  margin-top: 20px;
}

/* 输入框 */
:deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  transition: all 0.3s ease;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--el-color-primary-light-5) inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(6, 182, 212, 0.3) inset;
}
:deep(.el-input__inner) {
  height: 42px;
  font-size: 14px;
}

/* 按钮 */
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  border: none;
  transition: all 0.3s ease;
}
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(6, 182, 212, 0.4);
}
.submit-btn:active {
  transform: translateY(0);
}

/* 表单项间距 */
:deep(.el-form-item) {
  margin-bottom: 18px;
}

/* 验证码按钮 */
:deep(.el-button--info) {
  border-radius: 10px;
  height: 42px;
}

/* select */
:deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
}

/* 移动端适配 */
@media screen and (max-width: 480px) {
  .login-card {
    padding: 28px 20px;
  }
  .brand-title {
    font-size: 20px;
  }
}
</style>
