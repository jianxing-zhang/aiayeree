import katex from 'katex'
import 'katex/dist/katex.min.css'

// 转义 HTML 特殊字符，避免 XSS 注入
const escapeHtml = (text) =>
  String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

// 渲染前还原转义，避免破坏公式里的 < > &
const unescape = (t) =>
  t.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&')

function renderOne(formula, displayMode) {
  try {
    return katex.renderToString(unescape(formula).trim(), {
      displayMode,
      throwOnError: false
    })
  } catch (e) {
    return `<code>${formula}</code>`
  }
}

// 把文本中的 $...$（行内）与 $$...$$（块级）渲染成数学公式，返回 HTML 字符串
export function renderMath(text) {
  if (text == null) return ''
  let s = escapeHtml(String(text))
  // 先处理块级 $$...$$
  s = s.replace(/\$\$([\s\S]+?)\$\$/g, (_, f) => renderOne(f, true))
  // 再处理行内 $...$
  s = s.replace(/\$([^$\n]+?)\$/g, (_, f) => renderOne(f, false))
  return s
}

// 去掉 LaTeX 标记，用于简短的标题预览（不做完整渲染）
export function stripMath(text) {
  if (text == null) return ''
  return String(text)
    .replace(/\$\$([\s\S]+?)\$\$/g, '$1')
    .replace(/\$([^$\n]+?)\$/g, '$1')
    .replace(/\\frac|\\dfrac|\\sqrt|\\times|\\cdot|\\left|\\right|\\begin|\\end/g, ' ')
    .replace(/[\\{}]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * 从 aiResult 原文中提取"### 四、知识点大纲"部分的主题名列表。
 * 返回: ['椭圆相关', '斜率相关', ...]
 */
export function extractOutlineTopics(aiResult) {
  if (!aiResult) return []
  const m = aiResult.match(/#{2,4}\s*[四4][、.]\s*知识点大纲[\s\S]*/)
  if (!m) return []
  let raw = m[0]
  const idx = raw.indexOf('\n')
  if (idx >= 0) raw = raw.substring(idx + 1)
  return raw
    .split('\n')
    .map((l) => l.replace(/^[-•]\s*/, '').trim())
    .filter((l) => l && !l.startsWith('#'))
}