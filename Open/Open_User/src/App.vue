<template>
  <div class="app-container">
    <!-- 使用router-view显示当前路由对应的组件 -->
    <router-view />
  </div>
</template>

<script>
import WebSocketService from './services/websocketService.js'

export default {
  name: 'App',
  methods: {
    // 全局方法：检查登录状态
    checkLoginStatus() {
      const isLoggedIn = localStorage.getItem('isLoggedIn') || sessionStorage.getItem('isLoggedIn')
      console.log('登录状态:', isLoggedIn ? '已登录' : '未登录')
      return !!isLoggedIn
    },
    // 初始化WebSocket连接
    initWebSocket() {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token')
      if (token) {
        console.log('建立全局WebSocket连接')
        WebSocketService.connect(token)
      }
    },
    // 关闭WebSocket连接
    closeWebSocket() {
      WebSocketService.disconnect()
    }
  },
  mounted() {
    // 初始化时检查登录状态
    this.checkLoginStatus()
    // 建立WebSocket连接
    this.initWebSocket()
    console.log('应用已启动')
  },
  beforeUnmount() {
    // 应用卸载时关闭WebSocket连接
    this.closeWebSocket()
  }
}
</script>

<style>
/* 全局容器样式 */
.app-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}
</style>