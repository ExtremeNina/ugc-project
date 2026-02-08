<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2 class="login-title">登录</h2>
        <p class="login-subtitle">欢迎回来，继续你的技术之旅</p>
      </div>
      
      <div class="login-form">
        <div class="form-group">
          <label for="username" class="form-label">用户名</label>
          <div class="input-wrapper">
            <i class="fa fa-user-circle"></i>
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              placeholder="请输入用户名"
              class="form-input"
              @keyup.enter="handleLogin"
            >
          </div>
        </div>
        
        <div class="form-group">
          <label for="password" class="form-label">密码</label>
          <div class="input-wrapper">
            <i class="fa fa-lock"></i>
            <input 
              type="password" 
              id="password" 
              v-model="password" 
              placeholder="请输入密码"
              class="form-input"
              @keyup.enter="handleLogin"
            >
          </div>
        </div>
        
        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe">
            <span>记住我</span>
          </label>
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>
        
        <button 
          class="login-button anime-button primary" 
          @click="handleLogin"
          :disabled="isLoading"
        >
          <span v-if="!isLoading">登录</span>
          <span v-else>登录中...</span>
        </button>
        
        <div class="login-divider">
          <span class="divider-text">或使用以下方式登录</span>
        </div>
        
        <a href="#" class="login-button github-button" @click.prevent="loginWithGithub">
          <span class="github-icon">🔗</span>
          <span>使用GitHub登录</span>
        </a>
        
        <a href="#" class="login-button qq-button" @click.prevent="loginWithQQ">
          <span class="qq-icon">📧</span>
          <span>使用QQ邮箱登录</span>
        </a>
        
        <div class="login-footer">
        <p>还没有账号？<a href="#" class="register-link" @click.prevent="goToRegister">立即注册</a></p>
      </div>
      </div>
    </div>
    
    <!-- 背景装饰 -->
    <div class="login-bg-decor">
      <div class="decor-circle decor-circle-1"></div>
      <div class="decor-circle decor-circle-2"></div>
      <div class="decor-circle decor-circle-3"></div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import WebSocketService from '../services/websocketService.js'

export default {
  name: 'LoginView',
  data() {
    return {
      username: '',
      password: '',
      rememberMe: false,
      isLoading: false
    }
  },
  methods: {
    goToRegister() {
      window.open('/register', '_blank')
    },
    
    loginWithGithub() {
      // 打印日志到浏览器控制台
      console.log('用户点击了GitHub登录按钮，正在跳转到GitHub授权页面...')
      const backendOAuthUrl = 'http://localhost:8081/oauth2/authorization/github'
      console.log('跳转路径:', backendOAuthUrl)
      // 直接跳转到后端的GitHub OAuth地址
      window.location.href = backendOAuthUrl
    },
    
    loginWithQQ() {
      // 跳转到QQ邮箱登录页面
      this.$router.push('/login/qq')
    },
    
    async handleLogin() {
      // 简单的表单验证
      if (!this.username || !this.password) {
        alert('请输入用户名和密码')
        return
      }
      
      this.isLoading = true
      
      try {
        // 调用实际的登录API接口，使用新的RESTful路径
        const response = await axios.post('http://localhost:8081/api/auth/login', {
          username: this.username,
          password: this.password
        })
        
        // 打印完整响应信息用于调试
        console.log('登录响应完整信息:', response)
        
        const userInfo = response.data
        
        // 打印响应数据
        console.log('登录响应数据:', userInfo)
        
        // 打印响应头中的token信息
        console.log('响应头中的Authorization:', response.headers['authorization'])
        console.log('响应头中的authorization:', response.headers['authorization'])
        console.log('响应头中的token:', response.headers['token'])
        
        // 打印响应数据中的token信息
        console.log('userInfo中的token:', userInfo.token)
        
        // 直接从response.data中提取JWT令牌
        const token = response.data.data
        
        // 打印最终提取的token
        console.log('从response.data中提取的JWT令牌:', token)
        
        // 保存登录状态和令牌到localStorage或sessionStorage
        if (this.rememberMe) {
          localStorage.setItem('userInfo', JSON.stringify(userInfo))
          localStorage.setItem('isLoggedIn', 'true')
          localStorage.setItem('token', token)
        } else {
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
          sessionStorage.setItem('isLoggedIn', 'true')
          sessionStorage.setItem('token', token)
        }
        
        // 设置axios默认请求头，用于后续请求携带token
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          axios.defaults.headers.common['Authorization'] = `Bearer ${cleanToken}`
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`)
        } else {
          console.warn('未在response.data中找到token')
        }
        
        // 建立WebSocket连接
        WebSocketService.connect(token)
        
        // 登录成功后跳转到社区主页
        this.$router.push('/community')
      } catch (error) {
        // 处理登录失败
        console.error('登录失败:', error)
        const errorMessage = error.response?.data?.message || '登录失败，请检查用户名和密码'
        alert(errorMessage)
      } finally {
        this.isLoading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
  padding: 20px;
  font-family: 'Inter', sans-serif;
}

.login-card {
  background-color: var(--card-bg);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 420px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-title {
  font-size: 28px;
  color: var(--text-primary);
  margin-bottom: 10px;
  font-weight: 600;
}

.login-subtitle {
  color: var(--text-secondary);
  font-size: 16px;
}

.login-form .form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-primary);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper i {
  position: absolute;
  left: 15px;
  color: var(--accent-color);
  font-size: 18px;
  z-index: 1;
}

.form-input {
  width: 100%;
  padding: 12px 15px 12px 45px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  font-size: 16px;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}

.form-input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px rgba(242, 163, 58, 0.2);
  background-color: rgba(255, 255, 255, 0.1);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
}

.remember-me input[type="checkbox"] {
  accent-color: var(--accent-color);
  width: 16px;
  height: 16px;
}

.forgot-password {
  color: var(--accent-color);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.forgot-password:hover {
  color: rgba(242, 163, 58, 0.8);
}

.login-button {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: 500;
  border: none;
  border-radius: 4px;
  background-color: var(--accent-color);
  color: var(--bg-primary);
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 48px;
  line-height: 1;
  text-decoration: none;
}

.login-divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background-color: rgba(255, 255, 255, 0.2);
}

.divider-text {
  padding: 0 15px;
  color: var(--text-secondary);
  font-size: 14px;
}

.github-button {
  background-color: #24292e;
  color: white;
  gap: 8px;
}

.github-button:hover {
  background-color: #3a3f44;
  box-shadow: 0 4px 12px rgba(36, 41, 46, 0.3);
}

.github-icon {
  font-size: 18px;
}

.qq-button {
  background-color: #12b7f5;
  color: white;
  gap: 8px;
}

.qq-button:hover {
  background-color: #0d9bd8;
  box-shadow: 0 4px 12px rgba(18, 183, 245, 0.3);
}

.qq-icon {
  font-size: 18px;
}

.login-button:hover:not(:disabled) {
  background-color: rgba(242, 163, 58, 0.9);
  box-shadow: 0 4px 12px rgba(242, 163, 58, 0.3);
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.register-link {
  color: var(--accent-color);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.register-link:hover {
  color: rgba(242, 163, 58, 0.8);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    padding: 30px 20px;
  }
  
  .login-title {
    font-size: 24px;
  }
  
  .form-input {
    font-size: 14px;
  }
}
</style>