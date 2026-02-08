import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'  // 导入全局样式

// 导入Font Awesome图标库
import '@fortawesome/fontawesome-free/css/all.css'

// 导入AOS库及其样式
import AOS from 'aos'
import 'aos/dist/aos.css'

// 创建Vue应用实例
const app = createApp(App)

// 使用路由
app.use(router)

// 挂载应用
app.mount('#app')

// 初始化AOS
AOS.init({
  duration: 500, // 默认动画持续时间
  easing: 'ease-in-out', // 默认缓动效果
  once: true, // 动画只触发一次
  mirror: false // 元素滚动回视口时不触发动画
})