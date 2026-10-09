<template>
  <div class="page">
    <div class="header">
      <h2>智能出题</h2>
      <el-button text @click="goBack">← 返回主页</el-button>
    </div>

    <el-card class="form-card">
      <el-form :model="form" label-width="90px">
        <el-form-item label="科目">
          <el-input v-model="form.subject" placeholder="如：数学 / 物理 / 英语" style="width: 260px" />
        </el-form-item>

        <el-form-item label="出题方式">
          <el-radio-group v-model="mode">
            <el-radio value="TEXT">文本出题</el-radio>
            <el-radio value="IMAGE">图片出题</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="mode === 'TEXT'">
          <el-form-item label="题目内容">
            <el-input
              v-model="form.text"
              type="textarea"
              :rows="4"
              placeholder="粘贴或输入原题，AI 会拆分为简单 / 中等 / 困难三个层级"
            />
          </el-form-item>
          <el-form-item label="参考答案">
            <el-input
              v-model="form.originalAnswer"
              type="textarea"
              :rows="2"
              placeholder="可选：原题参考答案"
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="题目图片">
            <el-upload
              :auto-upload="false"
              :limit="1"
              accept="image/*"
              list-type="picture-card"
              v-model:file-list="fileList"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
            >
              <div class="upload-trigger">+ 上传</div>
            </el-upload>
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">开始出题</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="detail" class="result-card">
      <template #header>
        <div class="result-header">
          <span>出题结果</span>
          <span class="result-meta">科目：{{ detail.question.subject || '未填写' }}</span>
        </div>
      </template>

      <div v-if="detail.knowledgePoints && detail.knowledgePoints.length" class="kp-block">
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

      <div v-if="detail.subQuestions && detail.subQuestions.length" class="sq-block">
        <div class="block-title">分级子题目</div>
        <el-collapse>
          <el-collapse-item
            v-for="sq in detail.subQuestions"
            :key="sq.id"
            :name="sq.id"
          >
            <template #title>
              <span class="sq-title">{{ diffLabel(sq.difficulty) }}：{{ shortText(sq.content) }}</span>
            </template>
            <div class="sq-body">
              <div class="sq-row"><b>题干：</b>{{ sq.content }}</div>
              <div v-if="sq.solution" class="sq-row"><b>解法：</b>{{ sq.solution }}</div>
              <div v-if="sq.answer" class="sq-row"><b>答案：</b>{{ sq.answer }}</div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { currentUser, uploadText, uploadImage } from '../api/index'

const router = useRouter()
const user = currentUser() || {}

const mode = ref('TEXT')
const loading = ref(false)
const detail = ref(null)
const form = reactive({
  subject: '',
  text: '',
  originalAnswer: ''
})

const fileList = ref([])
let rawFile = null
const onFileChange = (file) => { rawFile = file.raw }
const onFileRemove = () => { rawFile = null }

const diffLabel = (d) => ({ 基础: '简单', 进阶: '中等', 挑战: '困难' }[d] || d || '未知难度')
const tagType = (d) => ({ 基础: 'success', 进阶: 'warning', 挑战: 'danger' }[d] || 'info')
const shortText = (t) => (t && t.length > 30 ? t.slice(0, 30) + '...' : t)
const goBack = () => router.push('/main')

const submit = async () => {
  if (!form.subject.trim()) {
    ElMessage.warning('请先填写科目')
    return
  }
  if (mode.value === 'TEXT' && !form.text.trim()) {
    ElMessage.warning('请输入题目内容')
    return
  }
  if (mode.value === 'IMAGE' && !rawFile) {
    ElMessage.warning('请先选择题目图片')
    return
  }

  loading.value = true
  detail.value = null
  try {
    if (mode.value === 'TEXT') {
      detail.value = await uploadText({
        uploaderId: user.id || null,
        uploaderName: user.username || '匿名',
        subject: form.subject.trim(),
        sourceType: 'TEXT',
        text: form.text.trim(),
        originalAnswer: form.originalAnswer.trim()
      })
    } else {
      const fd = new FormData()
      fd.append('file', rawFile)
      fd.append('uploaderId', user.id || '')
      fd.append('uploaderName', user.username || '匿名')
      fd.append('subject', form.subject.trim())
      detail.value = await uploadImage(fd)
    }
    ElMessage.success('出题完成')
  } catch (e) {
    ElMessage.error(e.message || '出题失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.header h2 {
  margin: 0;
  font-size: 22px;
  color: #333;
}
.form-card {
  margin-bottom: 16px;
}
.upload-trigger {
  color: #8c939d;
  font-size: 13px;
}
.result-card {
  margin-bottom: 24px;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.result-meta {
  font-weight: 400;
  color: #909399;
  font-size: 13px;
}
.block-title {
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
.kp-block {
  margin-bottom: 20px;
}
.kp-tag {
  margin: 0 8px 8px 0;
}
.sq-title {
  font-weight: 500;
}
.sq-body {
  padding: 4px 0;
  line-height: 1.7;
  color: #555;
}
.sq-row {
  margin-bottom: 8px;
}
</style>