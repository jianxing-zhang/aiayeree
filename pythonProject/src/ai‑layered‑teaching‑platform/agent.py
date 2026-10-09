import os
from dotenv import load_dotenv
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langchain.agents import create_agent
from langchain.tools import tool
from langchain.chat_models import init_chat_model

env_path = r"E:\studeat\python\pythonProject\.env"
load_dotenv(env_path)
api_key = os.getenv("QIANWEN_API_KEY")
base_url = os.getenv("DASH_SCOPE_BASE_API_URL")

@tool
def getWeather(location: str) -> str:
    """查询指定地点天气
    Args:
        location: 城市名称
    """
    return f"Current weather in {location} is sunny"

# 工具调用推荐 qwen-plus，不要用 qwen-vl-plus
model = init_chat_model(
    model="qwen-plus",
    model_provider="openai",
    base_url=base_url,
    api_key=api_key,
    temperature=0.7
)

agent = create_agent(
    model=model,
    tools=[getWeather]
)

stream_response = agent.stream(
    {
        "messages": [
            SystemMessage(content="请使用工具查询天气消息"),
            HumanMessage("你好我是胡歌"),
            AIMessage("你好胡歌很高兴认识你"),
            HumanMessage("今天北京天气怎么样")
        ]
    }
)

# messages模式返回元组 (msg_chunk, metadata)
for chunk in stream_response:
    msg = chunk[0]  # 取出消息块
    if hasattr(msg, "content") and msg.content:
        print(msg.content, end="", flush=True)
print("\n")
