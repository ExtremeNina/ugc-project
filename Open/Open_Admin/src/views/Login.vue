<template>
  <div class="login-container">
    <div class="login-form-wrapper">
      <div class="login-title">
        <h2>二次元社区管理后台</h2>
        <p>请登录您的账号</p>
      </div>
      <el-form
        :model="loginForm"
        :rules="loginRules"
        ref="loginFormRef"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="User"
            :prefix-icon-style="{ color: '#909399' }"
            clearable
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            :prefix-icon-style="{ color: '#909399' }"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.remember" class="remember-checkbox">记住我</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
            :disabled="loading"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            type="default"
            @click="handleRegister"
            class="register-button"
            :disabled="loading"
          >
            注册账号
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth.js'

export default {
  name: 'Login',
  components: {
    User,
    Lock
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref()
    const loading = ref(false)
    
    const loginForm = reactive({
      username: '',
      password: '',
      remember: false
    })
    
    const loginRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
      ]
    }
    
    const handleRegister = () => {
      router.push('/register')
    }

    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      try {
        await loginFormRef.value.validate()
        loading.value = true
        
        // 调用实际登录API
        const response = await login({
          username: loginForm.username,
          password: loginForm.password
        })
        
        // 假设后端返回格式: { code: 200, data: { token: '...', userInfo: {...} }, message: 'success' }
        // 根据response.js的处理逻辑，这里的response已经是处理后的data部分
        
        // 构建用户信息对象
        const userInfo = {
          username: loginForm.username,
          ...response.userInfo // 合并后端返回的用户信息
        }
        
        // 获取token
        const token = response.token || response
        
        if (!token) {
          throw new Error('未返回token信息')
        }
        
        // 保存用户信息到localStorage或sessionStorage
        if (loginForm.remember) {
          localStorage.setItem('userInfo', JSON.stringify(userInfo))
          localStorage.setItem('token', token)
        } else {
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo))
          sessionStorage.setItem('token', token)
        }
        
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error(error.message || '用户名或密码错误')
      } finally {
        loading.value = false
      }
    }
    
    return {
      loginFormRef,
      loginForm,
      loginRules,
      loading,
      handleLogin,
      handleRegister
    }
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 40" width="80" height="40"><path fill="%23ffffff" fill-opacity="0.05" d="M0 40a19.96 19.96 0 0 1 5.9-14.11 20.17 20.17 0 0 1 19.44-5.2A20 20 0 0 1 20.2 40H0zM65.32.75A20.02 20.02 0 0 1 40.8 25.26 20.02 20.02 0 0 1 65.32.76zM.07 0h20.1l-.08.07A20.02 20.02 0 0 1 .75 5.25 20.08 20.08 0 0 1 .07 0zm1.94 40h2.53l4.26-4.24v-9.78A17.96 17.96 0 0 0 2 40zm65.13-1.93l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7z"></path></svg>');
}

.login-form-wrapper {
  width: 400px;
  max-width: 90%;
  background: #ffffff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 10;
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
}

.login-title h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
  font-weight: 600;
}

.login-title p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.login-form {
  width: 100%;
}

.login-form .el-form-item {
  margin-bottom: 24px;
}

.login-form .el-input {
  height: 44px;
}

.login-form .el-input__wrapper {
  border-radius: 8px;
}

.remember-checkbox {
  font-size: 14px;
  color: #606266;
}

.login-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-button:hover {
  background: linear-gradient(135deg, #5a67d8 0%, #6b46c1 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

.register-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 10px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-form-wrapper {
    padding: 30px 20px;
    margin: 10px;
  }
  
  .login-title h2 {
    font-size: 20px;
  }
  
  .login-form .el-form-item {
    margin-bottom: 20px;
  }
}
</style>