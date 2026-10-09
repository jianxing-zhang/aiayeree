// 后端服务地址
export const USER_BASE = 'http://localhost:8081'
export const QS_BASE = 'http://localhost:8082'

// 统一请求：后端统一返回 {code, msg, data}，成功 code===200 时返回 data，否则抛错
export async function request(url, options = {}) {
  const res = await fetch(url, options)
  const data = await res.json()
  if (data.code === 200) return data.data
  throw new Error(data.msg || '请求失败')
}

// 获取当前登录用户（登录成功后存入 localStorage）
export function currentUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (e) {
    return null
  }
}

// ===== 用户接口（8081） =====

// 发送注册验证码
export function sendRegisterCode(phone) {
  return request(`${USER_BASE}/login/sendRegisterCode?phone=${encodeURIComponent(phone)}`)
}

// 注册
export function register(payload) {
  const qs = new URLSearchParams()
  Object.keys(payload).forEach((k) => {
    const v = payload[k]
    if (v !== null && v !== undefined && v !== '') qs.append(k, v)
  })
  return request(`${USER_BASE}/login/register?${qs.toString()}`, { method: 'POST' })
}

// 登录
export function login(name, password) {
  return request(`${USER_BASE}/login?name=${encodeURIComponent(name)}&password=${encodeURIComponent(password)}`)
}

// 忘记密码（重置密码）
export function forgetPwd(payload) {
  const qs = new URLSearchParams()
  Object.keys(payload).forEach((k) => {
    const v = payload[k]
    if (v !== null && v !== undefined && v !== '') qs.append(k, v)
  })
  return request(`${USER_BASE}/login/forgetPwd?${qs.toString()}`, { method: 'POST' })
}

// ===== 题库接口（8082） =====

// 文本录入智能出题
export function uploadText(payload) {
  return request(`${QS_BASE}/api/question/upload-text`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

// 图片上传智能出题
export function uploadImage(formData) {
  return request(`${QS_BASE}/api/question/upload-image`, {
    method: 'POST',
    body: formData
  })
}

// 题库列表 / 搜索
export function listQuestions(params = {}) {
  const qs = new URLSearchParams()
  Object.keys(params).forEach((k) => {
    const v = params[k]
    if (v !== null && v !== undefined && v !== '') qs.append(k, v)
  })
  return request(`${QS_BASE}/api/question/list?${qs.toString()}`)
}

// 题目详情（含知识点与小问题）
export function questionDetail(id) {
  return request(`${QS_BASE}/api/question/${id}`)
}

// 按需生成知识点大纲某主题的详细内容
export function fetchOutline(questionId, topic) {
  return request(`${QS_BASE}/api/question/${questionId}/outline?topic=${encodeURIComponent(topic)}`)
}

// 删除题目
export function deleteQuestion(id) {
  return request(`${QS_BASE}/api/question/${id}`, { method: 'DELETE' })
}

// ===== 收藏 =====
export function addFavorite(userId, questionId, category) {
  const qs = new URLSearchParams({ userId, questionId })
  if (category) qs.append('category', category)
  return request(`${QS_BASE}/api/question/favorite?${qs.toString()}`, { method: 'POST' })
}

export function removeFavorite(userId, questionId) {
  return request(`${QS_BASE}/api/question/favorite?userId=${userId}&questionId=${questionId}`, { method: 'DELETE' })
}

export function listFavorites(userId, category) {
  const qs = new URLSearchParams({ userId })
  if (category) qs.append('category', category)
  return request(`${QS_BASE}/api/question/favorite/list?${qs.toString()}`)
}

export function listFavoriteCategories(userId) {
  return request(`${QS_BASE}/api/question/favorite/categories?userId=${userId}`)
}