<template>
  <div class="edit-profile-container">
    <div class="edit-profile-header">
      <h1>编辑个人资料</h1>
    </div>
    <div class="edit-profile-content">
      <div class="profile-card">
        <!-- 头像上传区域 -->
        <div class="avatar-section">
          <div class="avatar-container">
            <div class="avatar-preview">
              <img :src="formData.avatar" alt="用户头像" class="avatar-img">
              <div class="avatar-border"></div>
            </div>
            <div class="avatar-upload">
              <label for="avatar-upload" class="upload-btn">
                <span class="upload-icon">📷</span>
                更换头像
              </label>
              <input 
                id="avatar-upload" 
                type="file" 
                accept="image/*" 
                class="hidden-input"
                @change="handleAvatarUpload"
              >
            </div>
          </div>
        </div>
        
        <!-- 个人信息表单 -->
        <form class="profile-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label for="username" class="form-label">用户名</label>
            <input 
              id="username" 
              type="text" 
              v-model="formData.username" 
              class="form-input"
              placeholder="请输入用户名"
            >
          </div>
          
          <div class="form-group">
            <label for="password" class="form-label">密码</label>
            <div class="password-input-container">
              <input 
                id="password" 
                :type="showPassword ? 'text' : 'password'" 
                v-model="formData.password" 
                class="form-input password-input"
                placeholder="请输入密码"
              >
              <button 
                type="button" 
                class="password-toggle-btn"
                @click="togglePasswordVisibility"
              >
                {{ showPassword ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>
          
          <div class="form-group">
            <label for="bio" class="form-label">个人简介</label>
            <textarea 
              id="bio" 
              v-model="formData.bio" 
              class="form-textarea"
              placeholder="请输入个人简介"
              rows="4"
            ></textarea>
          </div>
          
          <div class="form-group">
            <label for="email" class="form-label">邮箱</label>
            <input 
              id="email" 
              type="email" 
              v-model="formData.email" 
              class="form-input"
              placeholder="请输入邮箱"
            >
          </div>
          
          <div class="form-group">
            <label for="phone" class="form-label">手机号码</label>
            <input 
              id="phone" 
              type="tel" 
              v-model="formData.phone" 
              class="form-input"
              placeholder="请输入手机号码"
            >
          </div>
          
          <div class="form-group">
            <label for="location" class="form-label">所在地</label>
            <input 
              id="location" 
              type="text" 
              v-model="formData.location" 
              class="form-input"
              placeholder="请输入所在地"
            >
          </div>
          

          
          <div class="form-actions">
            <button type="button" class="btn btn-cancel" @click="handleCancel">取消</button>
            <button type="submit" class="btn btn-save">保存修改</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
export default {
  name: 'EditProfileView',
  data() {
    return {
      formData: {
        username: '动漫爱好者',
        avatar: 'https://picsum.photos/id/1027/200/200',
        bio: '热爱二次元文化，喜欢编程和游戏开发，希望结交更多志同道合的朋友！',
        email: 'anime@example.com',
        phone: '',
        location: '上海',
        password: ''
      },
      originalData: {}, // 用于保存原始数据，以便取消编辑时恢复
      showPassword: false, // 控制密码是否可见
      avatarFile: null // 存储待上传的头像文件对象
    }
  },
  // 路由守卫：当用户离开编辑资料页面时（包括点击后退键），跳转到首页
  beforeRouteLeave(to, from, next) {
    // 如果不是通过取消按钮返回个人资料页面，则跳转到首页
    if (to.path !== '/profile') {
      next('/community')
    } else {
      next() // 允许正常返回个人资料页面
    }
  },
  mounted() {
    // 保存原始数据
    this.originalData = JSON.parse(JSON.stringify(this.formData))
  },
  methods: {
    // 处理头像上传
    handleAvatarUpload(event) {
      const file = event.target.files[0]
      if (file) {
        // 存储文件对象用于后续上传
        this.avatarFile = file
        console.log('已将文件对象存储到this.avatarFile:', this.avatarFile)
        
        // 生成预览
        const reader = new FileReader()
        reader.onload = (e) => {
          this.formData.avatar = e.target.result
          console.log('已生成预览图片，this.formData.avatar:', this.formData.avatar)
        }
        reader.readAsDataURL(file)
      }
    },
    
    // 处理表单提交
    async handleSubmit() {
      // 表单验证
      if (!this.formData.username) {
        alert('用户名不能为空')
        return
      }
      
      try {
        // 1. 先上传头像（如果有新头像）
        let uploadedAvatarUrl = null
        if (this.avatarFile) {
          uploadedAvatarUrl = await this.uploadAvatarToServer()
          // 如果头像上传成功，更新formData中的avatar字段
          if (uploadedAvatarUrl) {
            this.formData.avatar = uploadedAvatarUrl
            console.log('头像上传成功，更新后的formData:', this.formData)
          }
        }
        
        // 2. 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {}
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
        }
        
        // 3. 发送API请求更新用户资料
        const response = await axios.put('http://localhost:8081/api/users/me', this.formData, { headers })
        console.log('更新用户资料响应:', response.data)
        
        // 4. 处理响应结果
        if (response.data.code === 200) {
          alert('资料更新成功！')
          this.$router.push('/profile') // 返回个人资料页
        } else {
          alert(`更新失败: ${response.data.message || '未知错误'}`)
        }
      } catch (error) {
        console.error('更新用户资料失败:', error)
        alert(`更新失败: ${error.response?.data?.message || '网络错误或服务器异常'}`)
      }
    },
    
    // 处理取消编辑
    handleCancel() {
      // 恢复原始数据
      this.formData = JSON.parse(JSON.stringify(this.originalData))
      this.$router.push('/profile') // 返回个人资料页
    },
    
    // 上传头像到后端
    async uploadAvatarToServer() {
      try {
        // 检查是否有头像文件需要上传
        if (!this.avatarFile) {
          console.log('没有需要上传的头像文件')
          return null
        }
        
        console.log('开始上传头像到后端，文件:', this.avatarFile)
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {
          'Authorization': `Bearer ${token}`
          // 注意：不要手动设置Content-Type，axios会自动处理multipart/form-data
        }
        
        // 创建FormData对象
        const formData = new FormData()
        formData.append('file', this.avatarFile)
        console.log('FormData创建成功，包含的文件:', formData.get('file'))
        
        // 发送上传请求
        console.log('准备发送请求到:', 'http://localhost:8081/api/users/me/upload/image')
        const response = await axios.post('http://localhost:8081/api/users/me/upload/image', formData, { headers })
        console.log('头像上传响应:', response.data)
        
        // 处理响应结果
        if (response.data.code === 200) {
          // 后端可能返回了新的头像URL，这里需要根据实际响应格式更新
          let avatarUrl = response.data.data || response.data
          
          // 根据后端返回的数据结构调整
          if (avatarUrl && typeof avatarUrl === 'object' && avatarUrl.url) {
            avatarUrl = avatarUrl.url
            console.log('从响应中提取的头像URL:', avatarUrl)
          }
          
          return avatarUrl
        } else {
          throw new Error(response.data.message || '上传失败')
        }
      } catch (error) {
        console.error('上传头像到后端失败:', error)
        throw error
      }
    },
    
    // 切换密码可见性
    togglePasswordVisibility() {
      this.showPassword = !this.showPassword
    }
  }
}
</script>

<style scoped>
.edit-profile-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-primary);
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  padding: var(--spacing-xl);
  box-sizing: border-box;
}

.edit-profile-header {
  margin-bottom: var(--spacing-xxl);
}

.edit-profile-header h1 {
  font-size: 32px;
  color: var(--text-primary);
  margin: 0;
  font-weight: 700;
}

.edit-profile-content {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.profile-card {
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  padding: var(--spacing-xxl);
  width: 100%;
  max-width: 800px;
  box-sizing: border-box;
}

/* 头像区域 */
.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-xxl);
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
}

.avatar-preview {
  position: relative;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-border {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border: 2px solid var(--border-color);
  border-radius: 50%;
}

.avatar-upload {
  display: flex;
  justify-content: center;
}

.upload-btn {
  background-color: var(--accent-primary);
  color: white;
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--border-radius);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.upload-btn:hover {
  background-color: var(--accent-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.hidden-input {
  display: none;
}

/* 表单样式 */
.profile-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.form-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.form-input, .form-textarea {
  padding: var(--spacing-md);
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  font-size: 16px;
  color: var(--text-primary);
  transition: all var(--transition-normal);
  width: 100%;
  box-sizing: border-box;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 3px rgba(242, 163, 58, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
  line-height: 1.6;
}

/* 密码输入样式 */
.password-input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input {
  padding-right: 48px; /* 为密码切换按钮留出空间 */
}

.password-toggle-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: color var(--transition-normal);
}

.password-toggle-btn:hover {
  color: var(--text-primary);
}

/* 表单按钮 */
.form-actions {
  display: flex;
  gap: var(--spacing-lg);
  justify-content: flex-end;
  margin-top: var(--spacing-xxl);
}

.btn {
  padding: var(--spacing-md) var(--spacing-xl);
  border-radius: var(--border-radius);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
  border: none;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  box-shadow: var(--shadow-sm);
}

.btn-cancel {
  background-color: var(--bg-tertiary);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.btn-cancel:hover {
  background-color: var(--bg-secondary);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.btn-save {
  background-color: var(--accent-primary);
  color: white;
}

.btn-save:hover {
  background-color: var(--accent-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .edit-profile-container {
    padding: var(--spacing-lg);
  }
  
  .profile-card {
    padding: var(--spacing-lg);
  }
  
  .avatar-preview {
    width: 120px;
    height: 120px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>