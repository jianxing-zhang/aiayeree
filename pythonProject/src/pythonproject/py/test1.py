import os
from dotenv import load_dotenv
from langchain.chat_models import init_chat_model

# 打印当前脚本路径，确认位置
print("当前py文件路径：", __file__)

# 显式指定.env绝对路径！绕开相对路径坑
env_path = r"/.env"
print("尝试读取env路径：", env_path)
load_dotenv(env_path)

base_url = os.getenv("DASHSCOPE_BASE_URL")
api_key = os.getenv("DEEPSEEK_API_KEY")

print("base_url =", repr(base_url))
print("api_key =", repr(api_key))

# 兜底，如果读不到就手动赋值
if base_url is None:
    base_url = "https://api.deepseek.com/v1"
if api_key is None:
    raise Exception("DEEPSEEK_API_KEY读取失败，请检查.env")

model = init_chat_model(
    model="deepseek-chat",
    model_provider="openai",
    base_url=base_url,
    api_key=api_key
)

print(type(model))
resp = model.invoke("你好")
print(resp.content)
