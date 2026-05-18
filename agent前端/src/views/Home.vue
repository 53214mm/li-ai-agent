<template>
  <div class="home">
    <h1 class="title">🤖 Li AI Agent</h1>
    <p class="subtitle">选择一个 AI 应用开始对话</p>
    <div class="cards">
      <router-link to="/love" class="card card-love">
        <span class="card-icon">💕</span>
        <h2>AI 恋爱大师</h2>
        <p>倾诉恋爱难题，获取专属建议</p>
      </router-link>
      <router-link to="/manus" class="card card-manus">
        <span class="card-icon">🧠</span>
        <h2>AI 超级智能体</h2>
        <p>联网搜索、文件操作、PDF 生成，全能助手</p>
      </router-link>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}
.title {
  font-size: 36px;
  color: #fff;
  margin-bottom: 8px;
}
.subtitle {
  font-size: 16px;
  color: rgba(255,255,255,0.75);
  margin-bottom: 48px;
}
.cards {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
  justify-content: center;
}
.card {
  width: 280px;
  padding: 40px 28px;
  border-radius: 16px;
  text-decoration: none;
  color: #333;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}
.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.2);
}
.card-love {
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
}
.card-manus {
  background: linear-gradient(135deg, #a1c4fd, #c2e9fb);
}
.card-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}
.card h2 {
  font-size: 20px;
  margin-bottom: 8px;
}
.card p {
  font-size: 14px;
  color: #666;
}
</style>
