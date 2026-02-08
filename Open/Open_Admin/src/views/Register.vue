<template>
  <div class="register-container">
    <div class="register-form-wrapper">
      <div class="register-title">
        <h2>二次元社区管理后台</h2>
        <p>创建您的账号</p>
      </div>
      <el-form
        :model="registerForm"
        :rules="registerRules"
        ref="registerFormRef"
        class="register-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            prefix-icon="User"
            :prefix-icon-style="{ color: '#909399' }"
            clearable
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            :prefix-icon-style="{ color: '#909399' }"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            prefix-icon="Lock"
            :prefix-icon-style="{ color: '#909399' }"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱"
            prefix-icon="Message"
            :prefix-icon-style="{ color: '#909399' }"
            clearable
          ></el-input>
        </el-form-item>
        <el-form-item prop="verificationCode">
          <el-row :gutter="12">
            <el-col :span="16">
              <el-input
                v-model="registerForm.verificationCode"
                placeholder="验证码"
                prefix-icon="Ticket"
                :prefix-icon-style="{ color: '#909399' }"
                clearable
              ></el-input>
            </el-col>
            <el-col :span="8">
              <el-button 
                :disabled="countdown > 0"
                @click="sendVerificationCode"
                type="primary"
                class="send-code-button"
              >
                {{ countdown > 0 ? `${countdown}秒后重发` : '获取验证码' }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleRegister"
            class="register-button"
            :disabled="loading"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            type="default"
            @click="goToLogin"
            class="login-button"
            :disabled="loading"
          >
            已有账号，去登录
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
import { User, Lock, Message, Ticket } from '@element-plus/icons-vue'
import { register, sendVerificationCode } from '@/api/auth.js'

export default {
  name: 'Register',
  components: {
    User,
    Lock,
    Message,
    Ticket
  },
  setup() {
    const router = useRouter()
    const registerFormRef = ref()
    const loading = ref(false)
    const countdown = ref(0)
    let timer = null
    
    const registerForm = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
      verificationCode: ''
    })
    
    const validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'))
      } else if (value !== registerForm.password) {
        callback(new Error('两次输入密码不一致'))
      } else {
        callback()
      }
    }
    
    const registerRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        { validator: validatePass, trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
      ],
      verificationCode: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { min: 4, max: 6, message: '验证码长度为4-6位', trigger: 'blur' }
      ]
    }
    
    const sendVerificationCode = async () => {
      // 验证邮箱格式
      if (!registerForm.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
        ElMessage.warning('请输入正确的邮箱地址')
        return
      }
      
      try {
        loading.value = true
        // 调用发送验证码API
        await sendVerificationCode({ email: registerForm.email })
        
        // 启动倒计时
        countdown.value = 60
        timer = setInterval(() => {
          countdown.value--
          if (countdown.value <= 0) {
            clearInterval(timer)
          }
        }, 1000)
        
        ElMessage.success('验证码已发送，请查收邮箱')
      } catch (error) {
        console.error('发送验证码失败:', error)
        ElMessage.error(error.message || '发送验证码失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    const handleRegister = async () => {
      if (!registerFormRef.value) return
      
      try {
        await registerFormRef.value.validate()
        loading.value = true
        
        // 调用注册API
        await register({
          username: registerForm.username,
          password: registerForm.password,
          email: registerForm.email,
          verificationCode: registerForm.verificationCode
        })
        
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error(error.message || '注册失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
    
    const goToLogin = () => {
      router.push('/login')
    }
    
    return {
      registerFormRef,
      registerForm,
      registerRules,
      loading,
      countdown,
      sendVerificationCode,
      handleRegister,
      goToLogin
    }
  }
}
</script>

<style scoped>
.register-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.register-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 40" width="80" height="40"><path fill="%23ffffff" fill-opacity="0.05" d="M0 40a19.96 19.96 0 0 1 5.9-14.11 20.17 20.17 0 0 1 19.44-5.2A20 20 0 0 1 20.2 40H0zM65.32.75A20.02 20.02 0 0 1 40.8 25.26 20.02 20.02 0 0 1 65.32.76zM.07 0h20.1l-.08.07A20.02 20.02 0 0 1 .75 5.25 20.08 20.08 0 0 1 .07 0zm1.94 40h2.53l4.26-4.24v-9.78A17.96 17.96 0 0 0 2 40zm65.13-1.93l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7zm-3.42-2.58l1.71-1.7 1.71 1.7-1.71 1.7-1.71-1.7z"></path></svg>');
}

.register-form-wrapper {
  width: 400px;
  max-width: 90%;
  background: #ffffff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 10;
}

.register-title {
  text-align: center;
  margin-bottom: 30px;
}

.register-title h2 {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
  font-weight: 600;
}

.register-title p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.register-form {
  width: 100%;
}

.register-form .el-form-item {
  margin-bottom: 24px;
}

.register-form .el-input {
  height: 44px;
}

.register-form .el-input__wrapper {
  border-radius: 8px;
}

.register-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.register-button:hover {
  background: linear-gradient(135deg, #5a67d8 0%, #6b46c1 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.register-button:active {
  transform: translateY(0);
}

.login-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 10px;
}

.send-code-button {
  width: 100%;
  height: 44px;
  border-radius: 8px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .register-form-wrapper {
    padding: 30px 20px;
    margin: 10px;
  }
  
  .register-title h2 {
    font-size: 20px;
  }
  
  .register-form .el-form-item {
    margin-bottom: 20px;
  }
}
</style>