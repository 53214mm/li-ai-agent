import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Home from './views/Home.vue'
import LoveAppChat from './views/LoveAppChat.vue'
import ManusAgentChat from './views/ManusAgentChat.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/love', name: 'LoveApp', component: LoveAppChat },
  { path: '/manus', name: 'ManusAgent', component: ManusAgentChat }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const app = createApp(App)
app.use(router)
app.mount('#app')
