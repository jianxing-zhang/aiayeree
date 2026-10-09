import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage

env_path = r"E:\studeat\python\pythonProject\.env"
load_dotenv(env_path)

base_url = os.getenv("DASH_SCOPE_BASE_API_URL")
api_key = os.getenv("QIANWEN_API_KEY")
print("base_url:", base_url)
print("api_key:", api_key[:20] + "...")

model = ChatOpenAI(
    model="qwen-vl-plus",
    openai_api_base=base_url,
    openai_api_key=api_key
)

message = HumanMessage([
    {"type":"text", "text":"描述以下这张图片的内容."},
    {
        "type": "image_url",
        "image_url": {
            "url":"https://tse3-mm.cn.bing.net/th/id/OIP-C.JnkPxWn-aPS-USFZn6Z1BQHaEF?w=329&h=181&c=7&r=0&o=7&dpr=1.3&pid=1.7&rm=3"
        }
    }
])

resp = model.invoke([message])
print(resp.content)
