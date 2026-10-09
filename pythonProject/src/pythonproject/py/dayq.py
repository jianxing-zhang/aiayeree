import os
from dotenv import load_dotenv
from langchain.chat_models import init_chat_model

# 项目根目录 .env 绝对路径
env_path = r"/.env"
load_dotenv(env_path)

base_url = os.getenv("DASHSCOPE_BASE_URL")
api_key = os.getenv("DEEPSEEK_API_KEY")

model = init_chat_model(
    model="deepseek-chat",
    model_provider="openai",
    base_url=base_url,
    api_key=api_key,
    temperature=0.7
)

print(type(model))

response = model.invoke([
    {
        "role": "system",
        "content":"你扮演是是五藏"
    },
    {
        "role": "user",
        "content": "你是谁？"
    }

])
print(response.content)
stream = model.stream([
    {
        "role": "system",
        "content":"你扮演是是五藏"
    },
    {
        "role": "user",
        "content": "你是谁？"
    }

])
for chunk in stream:
    print(chunk.content,end="",flush=True)