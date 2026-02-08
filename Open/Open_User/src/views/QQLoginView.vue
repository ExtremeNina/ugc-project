<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h2 class="login-title">QQ邮箱登录</h2>
        <p class="login-subtitle">使用您的QQ邮箱账号登录</p>
      </div>
      
      <div class="login-form">
        <div class="form-group">
          <label for="email" class="form-label">QQ邮箱</label>
          <div class="input-wrapper">
            <i class="fa fa-envelope"></i>
            <input 
              type="email" 
              id="email" 
              v-model="email" 
              placeholder="请输入您的QQ邮箱" 
              class="form-input"
              @keyup.enter="handleQQLogin"
            >
          </div>
        </div>
        
        <div class="form-group">
          <label for="verificationCode" class="form-label">验证码</label>
          <div class="input-wrapper code-wrapper">
            <i class="fa fa-shield-alt"></i>
            <input 
              type="text" 
              id="verificationCode" 
              v-model="verificationCode" 
              placeholder="请输入验证码" 
              class="form-input code-input"
              @keyup.enter="handleQQLogin"
            >
            <button 
              class="send-code-btn" 
              @click="sendVerificationCode"
              :disabled="countdown > 0 || isLoading"
            >
              {{ countdown > 0 ? `${countdown}s后重新发送` : '获取验证码' }}
            </button>
          </div>
        </div>
        
        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe">
            <span>记住我</span>
          </label>
        </div>
        
        <button 
          class="login-button anime-button primary" 
          @click="handleQQLogin"
          :disabled="isLoading"
        >
          <span v-if="!isLoading">登录</span>
          <span v-else>登录中...</span>
        </button>
        
        <div class="login-footer">
          <p>返回<a href="/login" class="register-link">普通登录</a></p>
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
  name: 'QQLoginView',
  data() {
    return {
      email: '',
      verificationCode: '',
      countdown: 0,
      rememberMe: false,
      isLoading: false
    }
  },
  methods: {
    // 发送验证码
    async sendVerificationCode() {
      // 验证邮箱格式
      if (!this.email) {
        alert('请先输入QQ邮箱')
        return
      }
      
      if (!this.email.endsWith('@qq.com')) {
        alert('请输入有效的QQ邮箱地址')
        return
      }
      
      try {
        // 调用发送验证码API
        const response = await axios.post('http://localhost:8081/api/auth/QQ/sendCode', {
          email: this.email
        })
        
        // 检查返回值
        if (response.data === false) {
          alert('邮箱格式不正确')
        } else {
          alert('验证码已发送到您的QQ邮箱，请查收')
          // 开始倒计时
          this.startCountdown()
        }
      } catch (error) {
        console.error('发送验证码失败:', error)
        const errorMessage = error.response?.data?.message || '发送验证码失败，请稍后重试'
        alert(errorMessage)
      }
    },
    
    // 倒计时功能
    startCountdown() {
      this.countdown = 60
      const timer = setInterval(() => {
        this.countdown--
        if (this.countdown <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    },
    
    async handleQQLogin() {
      // 简单的表单验证
      if (!this.email || !this.verificationCode) {
        alert('请输入QQ邮箱和验证码')
        return
      }
      
      // 验证是否为QQ邮箱
      if (!this.email.endsWith('@qq.com')) {
        alert('请输入有效的QQ邮箱地址')
        return
      }
      
      this.isLoading = true
      
      try {
        // 调用QQ邮箱验证码登录API接口
        const response = await axios.post('http://localhost:8081/api/auth/QQ/login', {
          mail: this.email,
          code: this.verificationCode
        })
        
        // 打印完整响应信息用于调试
        console.log('QQ邮箱登录响应完整信息:', response)
        
        // 从response.data.data中提取JWT令牌（根据后端Result对象格式）
        const token = response.data.data
        
        // 打印最终提取的token
        console.log('从response.data.data中提取的JWT令牌:', token)
        
        // 保存登录状态和令牌到localStorage或sessionStorage
        if (this.rememberMe) {
          localStorage.setItem('isLoggedIn', 'true')
          localStorage.setItem('token', token)
        } else {
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
        console.error('QQ邮箱登录失败:', error)
        const errorMessage = error.response?.data?.message || '登录失败，请检查邮箱和验证码'
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

/* 验证码输入框和按钮样式 */
.code-wrapper {
  display: flex;
  align-items: center;
}

.code-input {
  flex: 1;
  padding-right: 110px;
}

.send-code-btn {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  padding: 8px 12px;
  background-color: var(--accent-color);
  color: var(--bg-primary);
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.send-code-btn:hover:not(:disabled) {
  background-color: rgba(242, 163, 58, 0.9);
  box-shadow: 0 2px 8px rgba(242, 163, 58, 0.3);
}

.send-code-btn:disabled {
  background-color: var(--text-secondary);
  cursor: not-allowed;
  opacity: 0.7;
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

/* 背景装饰 */
.login-bg-decor {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  z-index: -1;
}

.decor-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  animation: float 20s infinite ease-in-out;
}

.decor-circle-1 {
  width: 400px;
  height: 400px;
  background-color: var(--accent-color);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.decor-circle-2 {
  width: 300px;
  height: 300px;
  background-color: rgba(255, 255, 255, 0.5);
  bottom: -150px;
  right: -150px;
  animation-delay: 5s;
}

.decor-circle-3 {
  width: 200px;
  height: 200px;
  background-color: var(--accent-color);
  top: 50%;
  right: -100px;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-50px) rotate(180deg);
  }
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