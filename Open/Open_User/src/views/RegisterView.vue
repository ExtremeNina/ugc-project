<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1 class="register-title">欢迎注册 <span class="logo-text">Open</span> 社区</h1>
        <p class="register-subtitle">加入我们，分享知识，连接技术，共同成长</p>
      </div>
      
      <div class="register-form">
        <div class="form-group">
          <label for="username" class="form-label">用户名</label>
          <div class="input-wrapper">
            <i class="icon-user">👤</i>
            <input 
              type="text" 
              id="username" 
              v-model="username" 
              placeholder="输入用户名（1-10个字符）" 
              class="form-input"
              @keyup.enter="handleRegister"
            >
          </div>
          <span v-if="errors.username" class="error-message">{{ errors.username }}</span>
        </div>
        
        <div class="form-group">
          <label for="email" class="form-label">邮箱（选填）</label>
          <div class="input-wrapper">
            <i class="icon-email">✉️</i>
            <input 
              type="email" 
              id="email" 
              v-model="email" 
              placeholder="输入邮箱地址（选填）"
              class="form-input"
              @keyup.enter="handleRegister"
            >
          </div>
          <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
        </div>
        
        <div class="form-group">
          <label for="password" class="form-label">密码</label>
          <div class="input-wrapper">
            <i class="icon-password">🔒</i>
            <input 
              type="password" 
              id="password" 
              v-model="password" 
              placeholder="设置密码（6-10个字符）"
              class="form-input"
              @keyup.enter="handleRegister"
            >
          </div>
          <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
        </div>
        
        <div class="form-group">
          <label for="confirmPassword" class="form-label">确认密码</label>
          <div class="input-wrapper">
            <i class="icon-password">🔒</i>
            <input 
              type="password" 
              id="confirmPassword" 
              v-model="confirmPassword" 
              placeholder="再次输入密码"
              class="form-input"
              @keyup.enter="handleRegister"
            >
          </div>
          <span v-if="errors.confirmPassword" class="error-message">{{ errors.confirmPassword }}</span>
        </div>
        
        <div class="form-group">
          <label for="captcha" class="form-label">验证码</label>
          <div class="input-wrapper captcha-wrapper">
            <i class="icon-code">🔑</i>
            <input 
              type="text" 
              id="captcha" 
              v-model="captcha" 
              placeholder="输入验证码" 
              class="form-input captcha-input"
              @keyup.enter="handleRegister"
            >
            <button class="captcha-button" @click="refreshCaptcha">
              获取验证码
            </button>
          </div>
          <!-- 验证码显示区域 -->
          <div v-if="displayedCaptcha" class="captcha-display">
            <span class="captcha-label">验证码: </span>
            <span class="captcha-code">{{ displayedCaptcha }}</span>
          </div>
          <span v-if="errors.captcha" class="error-message">{{ errors.captcha }}</span>
        </div>
        
        <button 
          type="button"
          class="register-button anime-button primary" 
          @click="handleRegister"
          :disabled="isLoading || countdown > 0"
        >
          <span v-if="!isLoading">创建账号</span>
          <span v-else>注册中...</span>
        </button>
        
        <div class="register-footer">
          <p>已有账号？<a href="#" class="login-link" @click.prevent="goToLogin">返回登录</a></p>
        </div>
      </div>
    </div>
    
    <!-- 背景装饰 -->
    <div class="register-bg-decor">
      <div class="decor-circle decor-circle-1"></div>
      <div class="decor-circle decor-circle-2"></div>
      <div class="decor-circle decor-circle-3"></div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'RegisterView',
  data() {
      return {
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
        captcha: '',
        displayedCaptcha: '',
        captchaImage: '',
        isLoading: false,
        errors: {}
      }
    },
  
  mounted() {
    // 页面加载时不再自动获取验证码，等待用户点击
    // this.refreshCaptcha()
  },
  methods: {
    // 表单验证
    validateForm() {
      this.errors = {}
      let isValid = true
      
      // 验证用户名
      if (!this.username.trim()) {
        this.errors.username = '用户名不能为空'
        isValid = false
      } else if (this.username.length < 1 || this.username.length > 10) {
        this.errors.username = '用户名长度应在1-10个字符之间'
        isValid = false
      }
      
      // 验证邮箱（选填）
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (this.email.trim() && !emailRegex.test(this.email)) {
        this.errors.email = '请输入有效的邮箱地址'
        isValid = false
      }
      
      // 验证验证码
      if (!this.captcha.trim()) {
        this.errors.captcha = '验证码不能为空'
        isValid = false
      } else if (this.captcha.length !== 6) {
        this.errors.captcha = '验证码长度为6位'
        isValid = false
      }
      
      // 验证密码
      if (!this.password) {
        this.errors.password = '密码不能为空'
        isValid = false
      } else if (this.password.length < 6 || this.password.length > 10) {
        this.errors.password = '密码长度应在6-10个字符之间'
        isValid = false
      }
      
      // 验证确认密码
      if (!this.confirmPassword) {
        this.errors.confirmPassword = '请确认密码'
        isValid = false
      } else if (this.confirmPassword !== this.password) {
        this.errors.confirmPassword = '两次输入的密码不一致'
        isValid = false
      }
      
      return isValid
    },
    
    async handleRegister() {
      // 验证表单
      if (!this.validateForm()) {
        return
      }
      
      this.isLoading = true
      
      try {
        // 调用注册API接口，根据后端RegisterDTO实体类调整参数名
        const response = await axios.post('http://localhost:8081/api/auth/register', {
          username: this.username,
          mailbox: this.email,       // 改为mailbox以匹配后端DTO
          onePassword: this.password, // 改为onePassword以匹配后端DTO
          twoPassword: this.confirmPassword, // 改为twoPassword以匹配后端DTO
          code: this.captcha         // 改为code以匹配后端DTO
        })
        
        // 注册成功后在新标签页打开登录页面
        alert('注册成功，请登录')
        window.open('/login', '_blank')
      } catch (error) {
        // 处理注册失败
        console.error('注册失败:', error)
        const errorMessage = error.response?.data?.message || '注册失败，请稍后重试'
        alert(errorMessage)
      } finally {
        this.isLoading = false
      }
    },
    
    goToLogin() {
      window.open('/login', '_blank')
    },
    
    // 刷新验证码
    async refreshCaptcha() {
      // 验证用户名是否已输入
      if (!this.username.trim()) {
        alert('请先输入用户名');
        return;
      }
      try {
        // 创建请求配置，包含请求头
        const config = {
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        }
        
        // 打印请求头信息
        console.log('发送验证码请求的请求头:', config.headers)
        
        // 调用后端验证码接口，使用POST请求，传递username参数（小写）
        const response = await axios.post('http://localhost:8081/api/auth/verify-code', { username: this.username }, config)
        
        // 从响应的data字段中获取验证码（根据后端统一响应格式）
        const captcha = response.data.data || response.data.code || response.data
        
        // 在页面上显示验证码
        this.displayedCaptcha = captcha
        
        // 清空输入框和错误提示
        this.captcha = ''
        this.errors.captcha = ''
        
        // 更新验证码图片显示（如果接口返回图片）
        if (response.data.image) {
          this.captchaImage = response.data.image
        } else {
          // 保留原有的占位图或使用文字提示
          this.captchaImage = ''
        }
      } catch (error) {
        // 打印详细错误信息
        console.error('获取验证码失败:', error);
        if (error.response) {
          console.error('响应状态:', error.response.status);
          console.error('响应数据:', error.response.data);
          alert(`获取验证码失败: ${error.response.status} ${error.response.data?.message || ''}`);
        } else if (error.request) {
          console.error('请求已发送但未收到响应:', error.request);
          alert(`获取验证码失败: ${error.message || '网络错误'}`);
        } else {
          console.error('请求配置错误:', error.message);
          alert(`获取验证码失败: ${error.message}`);
        }
      }
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
  position: relative;
  overflow: hidden;
  padding: var(--spacing-lg);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.register-card {
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-md);
  width: 100%;
  max-width: 420px;
  padding: var(--spacing-xl);
  position: relative;
  z-index: 1;
  border: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
}

.register-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.register-title {
  font-size: 24px;
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
  font-weight: 700;
}

.register-title .logo-text {
  color: var(--accent-primary);
}

.register-subtitle {
  color: var(--text-tertiary);
  font-size: 14px;
  line-height: 1.5;
}

.register-form .form-group {
  margin-bottom: var(--spacing-lg);
  position: relative;
}

.form-label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-weight: 500;
  color: var(--text-primary);
  font-size: 14px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper i {
  position: absolute;
  left: var(--spacing-md);
  color: var(--text-tertiary);
  font-size: 16px;
  z-index: 1;
  transition: color var(--transition-normal);
}

.form-input:focus + i {
  color: var(--accent-primary);
}

/* 验证码样式 */
.captcha-wrapper {
  display: flex;
  gap: var(--spacing-md);
}

.captcha-input {
  flex: 1;
  padding-right: var(--spacing-md);
}

.captcha-button {
  width: 120px;
  height: 45px;
  cursor: pointer;
  border-radius: var(--border-radius);
  border: 1px solid var(--accent-primary);
  background-color: transparent;
  color: var(--accent-primary);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-normal);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: inherit;
}

/* 验证码显示样式 */
.captcha-display {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.captcha-label {
  color: var(--text-tertiary);
  font-size: 14px;
}

.captcha-code {
  font-size: 16px;
  font-weight: bold;
  color: var(--accent-primary);
  letter-spacing: 2px;
}

.captcha-button:hover {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.captcha-button:active {
  transform: translateY(0);
}

.form-input {
  width: 100%;
  padding: var(--spacing-md) var(--spacing-md) var(--spacing-md) 45px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  font-size: 16px;
  transition: all var(--transition-normal);
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  font-family: inherit;
}

.form-input:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(242, 163, 58, 0.1);
}

.error-message {
  display: block;
  margin-top: var(--spacing-xs);
  color: var(--accent-error);
  font-size: 12px;
}

.register-button {
  width: 100%;
  padding: var(--spacing-md);
  font-size: 16px;
  font-weight: 500;
  border: none;
  border-radius: var(--border-radius);
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  cursor: pointer;
  transition: all var(--transition-normal);
  margin-bottom: var(--spacing-lg);
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 48px;
  line-height: 1;
  font-family: inherit;
}

.register-button:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.register-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.register-footer {
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
}

.login-link {
  color: var(--accent-primary);
  text-decoration: none;
  font-weight: 500;
  transition: color var(--transition-normal);
}

.login-link:hover {
  opacity: 0.9;
}

/* 背景装饰 */
.register-bg-decor {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.decor-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(242, 163, 58, 0.05) 0%, transparent 70%);
}

.decor-circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
}

.decor-circle-2 {
  width: 400px;
  height: 400px;
  bottom: -200px;
  right: -200px;
}

.decor-circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
  transform: translateY(-50%);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-card {
    padding: var(--spacing-lg) var(--spacing-md);
  }
  
  .register-title {
    font-size: 20px;
  }
  
  .form-input {
    font-size: 14px;
  }
  
  .captcha-wrapper {
    flex-direction: column;
  }
  
  .captcha-button {
    width: 100%;
  }
}
</style>