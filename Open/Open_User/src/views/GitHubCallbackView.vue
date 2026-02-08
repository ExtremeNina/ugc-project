<template>
  <div class="github-callback-container">
    <div class="loading-container">
      <div class="loading-spinner"></div>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import WebSocketService from '../services/websocketService.js'

export default {
  name: 'GitHubCallbackView',
  data() {
    return {
      message: '正在处理GitHub登录...'
    }
  },
  async mounted() {
    try {
      // 解析URL中的查询参数
      const urlParams = new URLSearchParams(window.location.search)
      const tokenFromUrl = urlParams.get('token')
      
      let token
      
      // 如果URL中没有token参数，可能是后端直接返回了JSON响应
      // 我们需要检查响应是否是JSON格式
      if (tokenFromUrl) {
        token = tokenFromUrl
        console.log('从URL获取到token:', token)
      } else {
        // 检查当前页面内容是否为JSON
        const responseText = document.body.textContent
        try {
          const responseJson = JSON.parse(responseText)
          if (responseJson.code === 200 && responseJson.token) {
            token = responseJson.token
            console.log('从页面响应获取到token:', token)
          } else {
            throw new Error('无效的响应格式')
          }
        } catch (e) {
          // 如果页面内容不是JSON，可能是后端重定向到了其他页面
          // 尝试从URL哈希参数中获取token
          const hashParams = new URLSearchParams(window.location.hash.substring(1))
          const tokenFromHash = hashParams.get('token')
          
          if (tokenFromHash) {
            token = tokenFromHash
            console.log('从URL哈希获取到token:', token)
          } else {
            throw new Error('无法解析响应内容，可能是网络错误或后端配置问题')
          }
        }
      }
      
      if (!token) {
        throw new Error('未找到有效的JWT令牌')
      }
      
      // 保存登录状态和令牌到localStorage（默认记住登录）
      localStorage.setItem('isLoggedIn', 'true')
      localStorage.setItem('token', token)
      
      // 设置axios默认请求头，用于后续请求携带token
      const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
      axios.defaults.headers.common['Authorization'] = `Bearer ${cleanToken}`
      console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`)
      
      // 建立WebSocket连接
      WebSocketService.connect(token)
      
      // 更新状态信息
      this.message = '登录成功，正在跳转...'
      
      // 延迟跳转，让用户看到成功信息
      setTimeout(() => {
        this.$router.push('/community')
      }, 1000)
      
    } catch (error) {
      console.error('GitHub登录回调处理失败:', error)
      this.message = `登录失败: ${error.message}`
      
      // 延迟返回登录页
      setTimeout(() => {
        this.$router.push('/login')
      }, 2000)
    }
  }
}
</script>

<style scoped>
.github-callback-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
  font-family: 'Inter', sans-serif;
}

.loading-container {
  text-align: center;
  color: var(--text-primary);
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-left-color: var(--accent-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-container p {
  font-size: 18px;
  color: var(--text-secondary);
}
</style>