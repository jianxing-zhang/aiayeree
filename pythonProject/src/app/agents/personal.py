import os
import sys
import base64 as _b64
from dotenv import load_dotenv
from langchain.agents import create_agent
from langchain.tools import tool
from langchain_openai import ChatOpenAI
from langchain_core.messages import AIMessage, HumanMessage, BaseMessage
from langgraph.graph import StateGraph, START, END, MessagesState

# 诊断日志
def _dbg(msg: str):
    sys.stderr.write(f"\n[ADAPTER] {msg}\n")
    sys.stderr.flush()

from pathlib import Path
# 项目根目录为 src 的上一级（pythonProject），.env 位于该目录
env_path = Path(__file__).resolve().parents[3] / ".env"
load_dotenv(env_path)
api_key = os.getenv("QIANWEN_API_KEY")
base_url = os.getenv("DASH_SCOPE_BASE_API_URL")

# 工具
@tool
def getWeather(location: str) -> str:
    """查询指定地点天气
    Args:
        location: 城市名称，例如：北京、上海
    """
    return f"Current weather in {location} is sunny"

@tool
def math_calculate(expression: str) -> str:
    """
    计算数学表达式，支持四则运算、平方、开方等。
    Args:
        expression: 数学表达式字符串，例如 "(2+3)*4, sqrt(16), 2**3"
    """
    import math
    safe_namespace = {"__builtins__": None, "math": math}
    try:
        result = eval(expression, safe_namespace)
        return f"计算结果：{result}"
    except Exception as e:
        return f"计算失败，错误信息：{str(e)}"

tools = [getWeather, math_calculate]

# ===== 关键：模型换成 qwen-vl-plus，让 agent 自己看图片 =====
model = ChatOpenAI(
    model="qwen-vl-plus",
    base_url=base_url,
    api_key=api_key,
    temperature=0.2,
    timeout=60,
    max_retries=2
)

system_prompt = """你是一名专业的难题知识点分解师，擅长将复杂题目拆解为梯度化子题目，帮助用户循序渐进掌握解题逻辑。
【核心任务】
接收用户输入的一道题目（可能是文字，也可能是图片），将其拆解为「简单、中等、困难」三个难度层级的子题目，拆解必须严格围绕原题核心知识点，不得偏离考点。
当解题过程中需要数值计算，可调用math_calculate工具。非天气相关问题，不要调用getWeather工具。

【数学格式要求】
所有数学公式一律用纯文本表达，禁止使用 LaTeX 语法（不要输出 $、\\frac、\\dfrac 等符号）。
- 分数写作 a/b，例如 x^2/a^2 + y^2/b^2 = 1
- 乘方写作 ^，例如 x^2、2^3
- 乘法写作 ×，例如 2×5 = 10
- 开方写作 √，例如 √16 = 4
- 上下标用普通写法，例如 a1 或 x^2

【难度划分标准】
1. 简单题：仅考察原题中单个最基础的知识点，是解原题的前置基础；必须给出完整解题步骤和最终答案。
2. 中等题：考察原题中2~3个关联知识点的结合，是基础到综合的过渡；只需列出题目，无需解析，但题目题干必须完整写出（含具体条件与数值）。
3. 困难题：即原题本身，是多知识点综合应用的最终题目；只需列出题目，无需解析，但题目题干必须完整写出（含具体条件与数值）。

【输出格式要求】
严格按照以下格式输出，不要添加多余寒暄；每个标题下方「题目：」后面必须给出完整题干，禁止留空：
### 一、简单题（基础知识点列出）
题目：[简单题题干，公式用纯文本]
【解法】 [分步解题过程，步骤清晰，公式用纯文本]
答案：[最终结果]

### 二、中等题（知识点结合列出）
题目：[中等题题干，必须完整写出]

### 三、困难题（原题综合列出）
题目：[原题完整题干，必须完整写出]

### 四、知识点大纲
只列出知识点主题名称，不输出具体内容（具体内容会在用户点击时按需生成）。格式：
- 椭圆相关
- 斜率相关
- 距离公式相关

【约束规则】
1. 所有子题目必须与原题考点高度相关，禁止引入无关知识点
2. 难度梯度平滑递进，符合学习认知规律
3. 简单题解法必须准确易懂，适合基础薄弱的学习者
4. 严格遵守输出格式，标题层级清晰
5. 三个难度层级的「题目：」都不能为空，必须给出完整题干
6. 知识点大纲列出2~5个主题，每行用 - 开头，只写主题名，不写具体内容
"""

agent = create_agent(model, tools=tools, system_prompt=system_prompt)

class AgentState(MessagesState):
    pass

# ===== 图片格式适配节点：只做格式转换，不做 OCR =====
# Studio 上传图片 -> {"type": "image", "data": "纯base64"}
# DashScope compatible-mode 需要 -> {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}}
def adapt_image_format(state: AgentState):
    last_msg = state["messages"][-1]
    _dbg(f"===== adapt_image_format =====")
    _dbg(f"content type={type(last_msg.content).__name__}")

    if not isinstance(last_msg.content, list):
        _dbg("不是多模态消息，原样返回")
        return state

    new_chunks = []
    has_image = False
    for chunk in last_msg.content:
        if isinstance(chunk, dict) and chunk.get("type") == "image" and "data" in chunk:
            has_image = True
            raw_b64 = chunk["data"]
            if isinstance(raw_b64, str):
                if raw_b64.startswith("data:"):
                    data_url = raw_b64
                else:
                    mime = "image/png" if raw_b64.startswith("iVBOR") else "image/jpeg"
                    data_url = f"data:{mime};base64,{raw_b64}"
            else:
                raw_b64_str = _b64.b64encode(raw_b64).decode()
                data_url = f"data:image/png;base64,{raw_b64_str}"
            # 转成 DashScope compatible-mode 能认的格式
            new_chunks.append({"type": "image_url", "image_url": {"url": data_url}})
            _dbg(f"图片格式适配完成, data_url 前80字符: {data_url[:80]}")
        else:
            new_chunks.append(chunk)

    if has_image:
        _dbg(f"适配后 chunks: {[c.get('type') if isinstance(c, dict) else str(c)[:30] for c in new_chunks]}")
        # 追加一条适配后的 image_url 消息（run_agent_node 只会取最后一条消息发给模型，
        # 原始 {"type":"image","data":...} 不会被发送，避免 DashScope 400 报错）
        return {"messages": [HumanMessage(content=new_chunks)]}

    _dbg("无图片，原样返回")
    return state


def run_agent_node(state: AgentState):
    _dbg(f"===== run_agent_node =====")
    # 只把最后一条消息（图片场景下即 adapt_image_format 适配后的 image_url 消息）
    # 发给 agent，原始 {"type":"image","data":...} 消息不会进入模型调用
    messages = state["messages"]
    last = messages[-1] if messages else None
    res = agent.invoke({"messages": [last]} if last is not None else state)
    return {"messages": [res["messages"][-1]]}


graph_builder = StateGraph(AgentState)
graph_builder.add_node("adapt_image", adapt_image_format)
graph_builder.add_node("agent", run_agent_node)
graph_builder.add_edge(START, "adapt_image")
graph_builder.add_edge("adapt_image", "agent")
graph_builder.add_edge("agent", END)

graph = graph_builder.compile()
