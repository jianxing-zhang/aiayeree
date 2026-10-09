<template>
  <div class="bank">
    <!-- 左侧：题目大纲 -->
    <aside class="left">
      <div class="left-head">
        <el-radio-group v-model="mode" size="small" @change="switchMode">
          <el-radio-button value="all">全部题库</el-radio-button>
          <el-radio-button value="fav">我的收藏</el-radio-button>
        </el-radio-group>
      </div>

      <el-input
        v-model="query.keyword"
        placeholder="搜索题目…"
        clearable
        size="small"
        class="search"
        @keyup.enter="load"
      />

      <div v-if="mode === 'fav'" class="cats">
        <div
          v-for="c in ['', ...categories]"
          :key="c || '__all'"
          class="cat"
          :class="{ active: favCategory === c }"
          @click="setCategory(c)"
        >
          {{ c || '全部分类' }}
        </div>
      </div>

      <div class="list" v-loading="loading">
        <div
          v-for="row in list"
          :key="row.id"
          class="item"
          :class="{ active: row.id === currentId }"
          @click="select(row)"
        >
          <div class="item-title">
            <span class="star" v-if="favoriteIds.has(row.id)">★</span>
            <span class="t">{{ brief(row) }}</span>
          </div>
          <div class="item-meta">
            <span class="subj">{{ row.subject || '未分类' }}</span>
            <span>{{ sourceLabel(row.sourceType) }}</span>
          </div>
        </div>
        <div v-if="!loading && list.length === 0" class="empty">暂无题目</div>
      </div>
    </aside>

    <!-- 右侧：题目详情 + 图片 -->
    <section class="right">
      <div v-if="!detail" class="placeholder">
        <div class="ph-icon">◀</div>
        <div>点击左侧大纲查看题目</div>
      </div>

      <template v-else>
        <div class="detail-head">
          <div class="dh-title">{{ brief(detail.question) }}</div>
          <div class="dh-actions">
            <el-button size="small" :type="isFav ? 'warning' : 'default'" @click="toggleFav">
              <el-icon><StarFilled v-if="isFav" /><Star v-else /></el-icon>
              <span class="btn-txt">{{ isFav ? '已收藏' : '收藏' }}</span>
            </el-button>
            <el-button size="small" type="danger" @click="onDelete">
              <el-icon><Delete /></el-icon>
              <span class="btn-txt">删除</span>
            </el-button>
          </div>
        </div>

        <div class="detail-body">
          <div v-if="detail.question.sourceImage" class="img-wrap">
            <el-image
              :src="imgSrc(detail.question.sourceImage)"
              fit="contain"
              class="qimg"
              :preview-src-list="[imgSrc(detail.question.sourceImage)]"
            />
          </div>

          <div v-if="qText" class="qtext" v-html="renderMath(qText)"></div>

          <div v-if="detail.knowledgePoints && detail.knowledgePoints.length" class="block">
            <div class="block-title">知识点拆解</div>
            <el-tag
              v-for="kp in detail.knowledgePoints"
              :key="kp.id"
              class="kp-tag"
              :type="tagType(kp.difficulty)"
            >
              {{ diffLabel(kp.difficulty) }} · {{ kp.name }}
            </el-tag>
          </div>

          <div v-if="detail.subQuestions && detail.subQuestions.length" class="block">
            <div class="block-title">分级子题目</div>
            <el-collapse>
              <el-collapse-item v-for="sq in detail.subQuestions.filter(s => s.content)" :key="sq.id" :name="sq.id">
                <template #title>
                  <span class="sq-title">{{ diffLabel(sq.difficulty) }}：{{ shortText(sq.content) }}</span>
                </template>
                <div class="sq-body">
                  <div class="sq-row"><b>题干：</b><span class="math" v-html="renderMath(sq.content)"></span></div>
                  <div v-if="sq.solution" class="sq-row"><b>解法：</b><span class="math" v-html="renderMath(sq.solution)"></span></div>
                  <div v-if="sq.answer" class="sq-row"><b>答案：</b><span class="math" v-html="renderMath(sq.answer)"></span></div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
          <div v-if="extractOutlineTopics(detail.question?.aiResult).length" class="detail-block">
            <div class="block-title">知识点大纲</div>
            <div class="outline-topics">
              <el-tag
                v-for="(t, i) in extractOutlineTopics(detail.question?.aiResult)"
                :key="i"
                class="outline-tag"
                :type="outlineLoading[t] ? 'info' : 'warning'"
                effect="plain"
                @click="loadOutline(detail.question.id, t)"
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
      </template>
    </section>

    <!-- 收藏分类弹窗 -->
    <el-dialog v-model="favDialog" title="收藏到分类" width="380px">
      <el-select
        v-model="favCategoryInput"
        placeholder="选择或输入新分类名"
        filterable
        allow-create
        default-first-option
        style="width: 100%"
      >
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <template #footer>
        <el-button @click="favDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmFav">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Delete } from '@element-plus/icons-vue'
import {
  currentUser,
  listQuestions,
  questionDetail,
  deleteQuestion,
  addFavorite,
  removeFavorite,
  listFavorites,
  listFavoriteCategories,
  fetchOutline
} from '../api/index'
import { renderMath, stripMath, extractOutlineTopics } from '../utils/math'

const user = currentUser() || {}

const mode = ref('all') // all | fav
const query = reactive({ keyword: '' })
const list = ref([])
const loading = ref(false)
const currentId = ref(null)
const detail = ref(null)
const outlineLoading = ref({})
const outlineData = ref([])

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

const categories = ref([])       // 该用户已有收藏分类
const favCategory = ref('')      // 当前筛选分类（空=全部）
const favoriteIds = ref(new Set())
const favDialog = ref(false)
const favCategoryInput = ref('')

const sourceLabel = (s) => ({ TEXT: '文本', IMAGE: '图片', DOC: '文档' }[s] || s)
const diffLabel = (d) => ({ 基础: '简单', 进阶: '中等', 挑战: '困难' }[d] || d || '未知难度')
const tagType = (d) => ({ 基础: 'success', 进阶: 'warning', 挑战: 'danger' }[d] || 'info')
const shortText = (t) => {
  const p = stripMath(t)
  return p.length > 30 ? p.slice(0, 30) + '…' : p
}

// 图片 base64 前缀补全
const imgSrc = (img) => {
  if (!img) return ''
  if (img.startsWith('data:image')) return img
  if (img.startsWith('iVBOR')) return 'data:image/png;base64,' + img
  if (img.startsWith('/9j')) return 'data:image/jpeg;base64,' + img
  return 'data:image/png;base64,' + img
}

// 题干文本（优先原文/OCR，木有就用 AI 结果里的困难题原文）
const qText = computed(() => {
  const q = detail.value && detail.value.question
  if (!q) return ''
  let t = ((q.sourceText || '') + '\n' + (q.recognizedText || '')).replace(/\s+/g, ' ').trim()
  if (!t && q.aiResult) {
    const m = q.aiResult.match(/三、困难题[\s\S]*?题目[:：]\s*([^\n]+)/)
    t = m ? m[1].trim() : ''
  }
  return t
})

// 题目简介
const brief = (row) => {
  if (!row) return ''
  let t = ((row.sourceText || '') + '\n' + (row.recognizedText || '')).replace(/\s+/g, ' ').trim()
  if (!t && row.aiResult) {
    const m = row.aiResult.match(/三、困难题[\s\S]*?题目[:：]\s*([^\n]+)/)
    t = m ? m[1].trim() : ''
  }
  if (!t) return '暂无简介'
  const p = stripMath(t)
  return p.length > 28 ? p.slice(0, 28) + '…' : p
}

const isFav = computed(() => detail.value && favoriteIds.value.has(detail.value.question.id))

const load = async () => {
  loading.value = true
  try {
    if (mode.value === 'fav') {
      list.value = await listFavorites(user.id, favCategory.value || undefined)
    } else {
      list.value = await listQuestions({ keyword: query.keyword, uploaderId: user.id })
    }
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const loadFavorites = async () => {
  try {
    const favs = await listFavorites(user.id)
    favoriteIds.value = new Set((favs || []).map((q) => q.id))
  } catch (e) {
    favoriteIds.value = new Set()
  }
}

const loadCategories = async () => {
  try {
    categories.value = await listFavoriteCategories(user.id)
  } catch (e) {
    categories.value = []
  }
}

const switchMode = () => {
  currentId.value = null
  detail.value = null
  favCategory.value = ''
  load()
}

const setCategory = (c) => {
  favCategory.value = c
  currentId.value = null
  detail.value = null
  load()
}

const select = async (row) => {
  currentId.value = row.id
  outlineData.value = []
  outlineLoading.value = {}
  try {
    detail.value = await questionDetail(row.id)
  } catch (e) {
    detail.value = null
  }
}

const toggleFav = () => {
  const q = detail.value.question
  if (!isFav.value) {
    favCategoryInput.value = ''
    favDialog.value = true
    _pendingFavQuestion = q.id
  } else {
    removeFavorite(user.id, q.id)
      .then(() => {
        favoriteIds.value.delete(q.id)
        ElMessage.success('已取消收藏')
        if (mode.value === 'fav') load()
      })
      .catch(() => ElMessage.error('操作失败'))
  }
}

let _pendingFavQuestion = null

const confirmFav = async () => {
  try {
    const cat = favCategoryInput.value && favCategoryInput.value.trim()
    await addFavorite(user.id, _pendingFavQuestion, cat || undefined)
    favoriteIds.value.add(_pendingFavQuestion)
    ElMessage.success('已收藏')
    favDialog.value = false
    if (cat && !categories.value.includes(cat)) categories.value.push(cat)
  } catch (e) {
    ElMessage.error('收藏失败')
  }
}

const onDelete = () => {
  const q = detail.value.question
  ElMessageBox.confirm('确定删除这道题目吗？删除后不可恢复。', '删除提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await deleteQuestion(q.id)
      ElMessage.success('已删除')
      // 从列表移除（收藏列表也移除）
      list.value = list.value.filter((it) => it.id !== q.id)
      favoriteIds.value.delete(q.id)
      currentId.value = null
      detail.value = null
    })
    .catch(() => {})
}

onMounted(() => {
  load()
  loadFavorites()
  loadCategories()
})
</script>

<style scoped>
.bank {
  display: flex;
  height: 100%;
  gap: 12px;
  padding: 16px;
  box-sizing: border-box;
}

/* 左栏 */
.left {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  padding: 12px;
}
.left-head {
  margin-bottom: 10px;
}
.left-head :deep(.el-radio-group) {
  width: 100%;
}
.left-head :deep(.el-radio-button) {
  flex: 1;
}
.search {
  margin-bottom: 10px;
}
.cats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.cat {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  cursor: pointer;
  background: #f2f3f5;
  color: #555;
}
.cat.active {
  background: #409eff;
  color: #fff;
}
.list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid transparent;
  background: #fafafa;
}
.item:hover {
  background: #f0f6ff;
}
.item.active {
  background: #ecf5ff;
  border-color: #409eff;
}
.item-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #303133;
  line-height: 1.4;
}
.item-title .star {
  color: #f7ba2a;
  flex-shrink: 0;
}
.item-title .t {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-meta {
  display: flex;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.item-meta .subj {
  color: #409eff;
}
.empty {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 13px;
}

/* 右栏 */
.right {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}
.placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 15px;
  gap: 8px;
}
.ph-icon {
  font-size: 28px;
}
.detail-head {
  padding: 14px 18px;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.dh-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dh-actions {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}
.btn-txt {
  margin-left: 2px;
}
.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  line-height: 1.7;
}
.img-wrap {
  margin-bottom: 16px;
  text-align: center;
}
.qimg {
  max-width: 100%;
  max-height: 320px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}
.qtext {
  background: #fafafa;
  border-left: 3px solid #409eff;
  padding: 12px;
  border-radius: 4px;
  color: #303133;
  margin-bottom: 16px;
  white-space: pre-wrap;
}
.block {
  margin-bottom: 20px;
}
.block-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
.kp-tag {
  margin: 0 8px 8px 0;
}
.sq-title {
  font-weight: 500;
}
.sq-body {
  padding: 4px 0;
  color: #555;
}
.sq-row {
  margin-bottom: 8px;
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
</style>