# Li AI Agent 前端

基于 Vue3 + Vite + Axios 构建的 AI 对话前端，对接后端 Spring Boot 服务。

## 功能

| 页面 | 路由 | 说明 |
|------|------|------|
| 主页 | `/` | 选择 AI 应用 |
| AI 恋爱大师 | `/love` | 流式对话，通过 SSE（Flux）实时输出 |
| AI 超级智能体 | `/manus` | 流式对话，通过 SSE（SseEmitter）实时输出 |

## 技术栈

- Vue 3.5 + Composition API
- Vue Router 4.5
- Axios 1.7
- Vite 6.0

## 项目结构

```
agent前端/
├── index.html
├── package.json
├── vite.config.js              # 开发服务器 + /api 代理 → localhost:8123
├── README.md
└── src/
    ├── main.js                 # 入口：路由注册
    ├── App.vue                 # 根组件
    ├── api/
    │   └── chat.js             # SSE 接口封装（Fetch + EventSource）
    ├── components/
    │   └── ChatWindow.vue      # 聊天室共享组件
    └── views/
        ├── Home.vue            # 主页：选择应用
        ├── LoveAppChat.vue     # AI 恋爱大师
        └── ManusAgentChat.vue  # AI 超级智能体
```

## 启动

```bash
# 安装依赖
npm install

# 开发模式（默认 http://localhost:3000）
npm run dev

# 生产构建
npm run build
```

## 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/love_app/chat/sse?message=&chatId=` | GET | Flux SSE 流，实时返回文本块 |
| `/api/ai/manus/chat?message=` | GET | SseEmitter，标准 SSE 事件流 |

## 代理配置

开发模式下 Vite 将 `/api` 请求代理到 `http://localhost:8123`（Spring Boot 后端端口）。
