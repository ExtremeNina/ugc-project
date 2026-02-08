<template>
  <div class="profile-container">
    <!-- 主内容区 -->
    <div class="profile-main">
      <!-- 侧边栏 -->
      <aside class="profile-sidebar">
        <!-- 用户信息卡片 -->
        <div class="user-info-card">
          <div class="avatar-container">
            <div class="avatar">
              <img :src="userProfile.avatar" alt="用户头像" class="profile-avatar">
              <div class="avatar-border"></div>
            </div>
          </div>
          
          <div class="user-details">
            <h1 class="username">{{ userProfile.username }}</h1>
            <div class="user-meta">
              <span class="level-badge">Lv.{{ userProfile.level }}</span>
              <span class="joined-date">{{ formatDate(userProfile.joinedDate) }}</span>
            </div>
            <p class="bio">{{ userProfile.bio }}</p>
            
            <!-- 统计信息 -->
            <div class="stats-row">
              <div class="stat-item">
                <span class="stat-number">{{ userProfile.followers }}</span>
                <span class="stat-label">粉丝</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-number">{{ userProfile.following }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-divider"></div>
              <div class="stat-item">
                <span class="stat-number">{{ userProfile.likes }}</span>
                <span class="stat-label">获赞</span>
              </div>
            </div>
            
            <button class="edit-profile-btn" @click="startEditingProfile">
              <span class="btn-icon">✏️</span>
              <span>编辑资料</span>
            </button>
          </div>
        </div>
        
        <!-- 内容导航菜单 -->
        <div class="content-nav">
          <button 
            v-for="tab in tabs" 
            :key="tab.id"
            :class="['nav-btn', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
          >
            <span class="nav-icon">{{ tab.icon }}</span>
            <span class="nav-text">{{ tab.name }}</span>
          </button>
        </div>
      </aside>
      
      <!-- 主内容 -->
      <main class="profile-content">
        <!-- 编辑资料表单 -->
        <div v-if="isEditing" class="profile-edit-form">
          <h2 class="form-title">编辑个人资料</h2>
          
          <!-- 错误信息提示框 -->
          <div v-if="showError" class="error-message-box">
            <span class="error-icon">❌</span>
            <span class="error-text">{{ errorMessage }}</span>
          </div>
          
          <form @submit.prevent="saveProfile" class="profile-edit-form">
            <div class="form-group">
              <label for="username">用户名</label>
              <input type="text" id="username" v-model="editForm.username" class="form-input">
            </div>
            
            <div class="form-group">
              <label for="password">密码</label>
              <input type="password" id="password" v-model="editForm.password" class="form-input" placeholder="留空表示不修改">
            </div>
            
            <div class="form-group">
              <label for="bio">个人简介</label>
              <textarea id="bio" v-model="editForm.bio" class="form-textarea" rows="4"></textarea>
            </div>
            
            <div class="form-group">
              <label for="mailbox">邮箱</label>
              <input type="email" id="mailbox" v-model="editForm.mailbox" class="form-input">
            </div>
            
            <div class="form-group">
              <label for="location">位置</label>
              <input type="text" id="location" v-model="editForm.location" class="form-input">
            </div>
            
            <div class="form-group">
              <label for="phone">电话</label>
              <input type="tel" id="phone" v-model="editForm.phone" class="form-input">
            </div>
            
            <div class="form-group">
              <label for="avatar">头像</label>
              <div class="avatar-upload">
                <div class="avatar-preview" :style="{ backgroundImage: `url(${editForm.avatar})` }">
                  <div class="upload-icon">📷</div>
                </div>
                <button type="button" class="change-avatar-btn" @click="triggerAvatarUpload">更改头像</button>
                <input type="file" id="avatar" accept="image/*" @change="handleAvatarUpload" class="avatar-input">
              </div>
            </div>
            
            <div class="form-actions">
              <button type="button" class="btn btn-cancel" @click="cancelEditing">取消</button>
              <button type="submit" class="btn btn-save">保存更改</button>
            </div>
          </form>
        </div>
        
        <!-- 内容列表 -->
        <div v-else class="content-list">
          <div v-if="activeTab === 'following'" class="following-content">
            <div v-for="user in userFollowing" :key="user.id" class="content-item">
              <div class="user-card">
                <img :src="user.avatar" alt="用户头像" class="user-avatar-small">
                <div class="user-info">
                  <h3 class="user-name">{{ user.username }}</h3>
                  <p class="user-bio">{{ user.bio }}</p>
                </div>
                <button class="unfollow-btn">已关注</button>
              </div>
            </div>
          </div>
          
          <div v-else-if="activeTab === 'columns'" class="columns-content">
            <div v-for="column in userColumns" :key="column.id" class="content-item">
              <h3 class="content-title">{{ column.title }}</h3>
              <p class="column-desc">{{ column.description }}</p>
              <div class="content-meta">
                <span class="article-count">{{ column.articleCount }} 篇文章</span>
                <span class="subscriber-count">👥 {{ column.subscribers }}</span>
              </div>
            </div>
          </div>
          
          <div v-else-if="activeTab === 'articles'" class="articles-content">
            <div v-for="article in userArticles" :key="article.id" class="content-item">
              <h3 class="content-title">{{ article.title }}</h3>
              <p class="content-excerpt">{{ article.content }}</p>
              <div class="content-meta">
                <span class="content-time">{{ formatDate(article.time) }} <span class="like-count">👍 {{ article.likes }}</span></span>
                <span class="content-stats">
                  <span class="view-count">👁️ {{ article.views }}</span>
                </span>
              </div>
            </div>
          </div>
          
          <div v-else-if="activeTab === 'collections'" class="collections-content">
            <div v-for="collection in userCollections" :key="collection.id" class="content-item">
              <h3 class="content-title">{{ collection.title }}</h3>
              <div class="collection-type">{{ collection.type }}</div>
              <div class="content-meta">
                <span class="content-time">{{ formatDate(collection.time) }}</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
export default {
  name: 'ProfileView',
  data() {
    return {
      activeTab: 'following',
      isEditing: false, // 编辑模式状态
      tabs: [
        { id: 'following', name: '关注', icon: '👥' },
        { id: 'columns', name: '专栏', icon: '📚' },
        { id: 'articles', name: '文章', icon: '📝' },
        { id: 'collections', name: '收藏夹', icon: '⭐' }
      ],
      // 用户资料数据
      userProfile: {
        username: '动漫爱好者',
        avatar: 'https://picsum.photos/id/1027/200/200',
        level: 5,
        joinedDate: '2023-05-15',
        bio: '热爱二次元文化，喜欢编程和游戏开发，希望结交更多志同道合的朋友！',
        followers: 128,
        following: 45,
        likes: 327,
        // 新增字段
        password: '',
        mailbox: 'anime@example.com',
        location: '上海',
        phone: '13800138000'
      },

      // 用户关注列表
      userFollowing: [
        {
          id: 1,
          username: '技术达人',
          avatar: 'https://picsum.photos/id/1012/100/100',
          bio: '全栈开发工程师，热爱分享技术心得'
        },
        {
          id: 2,
          username: '前端专家',
          avatar: 'https://picsum.photos/id/1025/100/100',
          bio: '专注前端10年，Vue核心贡献者'
        }
      ],
      // 用户专栏列表
      userColumns: [
        {
          id: 1,
          title: '前端性能优化实战',
          description: '深入解析前端性能优化的各种技巧和实践案例',
          articleCount: 12,
          subscribers: 328
        },
        {
          id: 2,
          title: 'JavaScript进阶指南',
          description: '从基础到高级，全面掌握JavaScript编程精髓',
          articleCount: 8,
          subscribers: 215
        }
      ],
      // 用户提问列表
      userQuestions: [
        {
          id: 1,
          title: 'Vue 3中如何实现高质量的动画效果？',
          content: '我正在开发一个二次元主题的网站，想要实现流畅的交互动画，有没有推荐的方法？',
          time: '2024-01-15T10:30:00',
          likes: 23,
          comments: 8
        },
        {
          id: 2,
          title: '如何优化React应用的性能？',
          content: '我的项目在处理大量数据时性能有点卡顿，有什么好的优化策略吗？',
          time: '2024-01-10T15:20:00',
          likes: 15,
          comments: 6
        }
      ],
      // 用户回答列表
      userAnswers: [
        {
          id: 1,
          questionTitle: 'CSS Grid布局的最佳实践是什么？',
          content: 'CSS Grid是一个强大的布局工具，建议使用fr单位和minmax()函数来创建灵活的网格系统...',
          time: '2024-01-14T09:45:00',
          likes: 42
        }
      ],
      // 用户文章列表
      userArticles: [
        {
          id: 1,
          title: '深入理解JavaScript闭包',
          content: '闭包是JavaScript中一个非常重要的概念，它允许函数访问其词法作用域外的变量...',
          time: '2024-01-05T14:30:00',
          views: 328,
          likes: 56
        }
      ],
      // 用户收藏列表
      userCollections: [
        {
          id: 1,
          title: '前端性能优化技巧合集',
          type: '文章',
          time: '2024-01-12T11:20:00'
        },
        {
          id: 2,
          title: 'Vue 3组合式API实战案例',
          type: '教程',
          time: '2024-01-08T16:45:00'
        }
      ],
      // 编辑表单数据
      editForm: {
        username: '',
        password: '',
        bio: '',
        avatar: '',
        mailbox: '',
        location: '',
        phone: ''
      },
      // 错误信息展示
      errorMessage: '',
      showError: false
    }
  },
  mounted() {
    // 设置网页标题为'萌技术社区 - 个人界面'
    document.title = '萌技术社区 - 个人界面';
    // 获取用户个人资料
    this.fetchUserProfile()
  },
  methods: {
    // 获取用户个人资料
    async fetchUserProfile() {
      try {
        // 检查并设置认证token
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        
        // 设置请求头
        const headers = {}
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
        }
        
        // 调用后端接口
        const response = await axios.get('http://localhost:8081/api/users/me', { headers })
        console.log('获取用户个人资料响应:', response.data)
        
        // 处理响应数据
        const profileData = response.data.data || response.data
        
        // 更新用户资料
        this.userProfile = {
          ...this.userProfile,
          username: profileData.username || this.userProfile.username,
          avatar: profileData.icon || this.userProfile.avatar,
          bio: profileData.description || this.userProfile.bio,
          followers: profileData.fan || 0, // 关注者数
          following: profileData.follows || 0, // 关注数（使用新的follows字段）
          likes: profileData.love || 0 // 获赞数
        }
      } catch (error) {
        console.error('获取用户个人资料失败:', error)
      }
    },
    // 直接返回后端传递的日期字符串
    formatDate(dateString) {
      return dateString || ''
    },
    // 开始编辑个人资料
     startEditingProfile() {
       this.isEditing = true;
       // 将当前用户信息复制到编辑表单
       this.editForm = {
         ...this.userProfile,
         // 确保所有字段都有默认值
         password: '', // 密码字段不显示当前值，需要用户重新输入
         mailbox: this.userProfile.mailbox || '',
         location: this.userProfile.location || '',
         phone: this.userProfile.phone || ''
       };
     },
     // 保存个人资料
     async saveProfile() {
       try {
         // 更新本地用户资料
         this.userProfile.username = this.editForm.username;
         this.userProfile.bio = this.editForm.bio;
         this.userProfile.avatar = this.editForm.avatar;
         this.userProfile.mailbox = this.editForm.mailbox;
         this.userProfile.location = this.editForm.location;
         this.userProfile.phone = this.editForm.phone;
         
         // 只有当密码字段不为空时才更新密码
         if (this.editForm.password) {
           this.userProfile.password = this.editForm.password;
         }
         
         // 从localStorage或sessionStorage获取token
         const token = localStorage.getItem('token') || sessionStorage.getItem('token');
         
         // 设置请求头
         const headers = {
           'Content-Type': 'application/json'
         };
         
         if (token) {
           // 处理token格式，确保只添加一次'Bearer '前缀
           const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
           headers['Authorization'] = `Bearer ${cleanToken}`;
         }
         
         // 准备更新数据
         const updateData = {
           username: this.editForm.username,
           userSummary: this.editForm.bio,
           icon: this.editForm.avatar,
           mailbox: this.editForm.mailbox,
           location: this.editForm.location,
           phone: this.editForm.phone
         };
         
         // 如果密码不为空，则包含密码
         if (this.editForm.password) {
           updateData.password = this.editForm.password;
         }
         
         // 调用更新用户资料接口
         await axios.put('http://localhost:8081/api/users/me', updateData, {
           headers
         });
         
         // 退出编辑模式
         this.isEditing = false;
         
         console.log('个人资料保存成功');
         // 显示保存成功提示
         alert('个人资料保存成功');
         // 可以在这里添加成功提示
       } catch (error) {
         console.error('个人资料保存失败:', error);
         // 显示错误信息
         this.errorMessage = error.response?.data?.message || error.message || '保存失败，请稍后重试';
         this.showError = true;
         // 3秒后自动关闭错误提示
         setTimeout(() => {
           this.showError = false;
         }, 3000);
       }
     },
     // 取消编辑
     cancelEditing() {
       this.isEditing = false;
     },
     // 触发头像上传
     triggerAvatarUpload() {
       document.getElementById('avatar').click();
     },
    // 处理头像上传
    async handleAvatarUpload(e) {
      const file = e.target.files[0];
      if (file) {
        // 先在本地预览
        const reader = new FileReader();
        reader.onload = (event) => {
          this.editForm.avatar = event.target.result;
        };
        reader.readAsDataURL(file);
        
        // 创建FormData上传图片到服务器
        const formData = new FormData();
        formData.append('file', file);
        
        try {
          // 调用上传图片接口，使用新的RESTful路径
          const token = localStorage.getItem('token') || sessionStorage.getItem('token');
          const uploadHeaders = {
            'Content-Type': 'multipart/form-data'
          };
          
          if (token) {
            const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
            uploadHeaders['Authorization'] = `Bearer ${cleanToken}`;
          }
          
          const response = await axios.post('http://localhost:8081/api/users/me/upload/image', formData, {
            headers: uploadHeaders
          });
          
          // 根据后端返回的数据结构调整
          const imageUrl = response.data.data || response.data;
          
          // 更新用户资料中的头像URL
          this.userProfile.avatar = imageUrl;
          this.editForm.avatar = imageUrl;
          
          // 可以在这里添加成功提示
          console.log('头像上传成功:', imageUrl);
        } catch (error) {
          console.error('头像上传失败:', error);
          // 可以在这里添加失败提示
        }
      }
    }
  }
}
</script>

<style scoped>
/* 容器样式 */
.profile-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-primary);
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
}

/* 主内容区 */
.profile-main {
  display: flex;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  flex: 1;
  align-items: flex-start;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

/* 侧边栏 */
.profile-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 用户信息卡片 - 与首页风格一致 */
.user-info-card {
  width: 100%;
  padding: var(--spacing-xl);
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  transition: all var(--transition-normal);
}

.user-info-card:hover {
  box-shadow: var(--shadow-md);
  border-color: rgba(242, 163, 58, 0.3);
}

.user-info-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  position: relative;
  overflow: hidden;
  transition: all var(--transition-normal);
  text-align: center;
}

/* 内容导航菜单 */
.content-nav {
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  padding: var(--spacing-sm) 0;
  transition: all var(--transition-normal);
  width: 100%;
}

.content-nav:hover {
  box-shadow: var(--shadow-md);
  border-color: rgba(242, 163, 58, 0.3);
}

.nav-btn {
  width: 100%;
  padding: var(--spacing-md) var(--spacing-lg);
  background: none;
  border: none;
  text-align: left;
  font-size: 16px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  transition: all var(--transition-normal);
  border-radius: var(--border-radius);
}

.nav-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
}

.nav-btn.active {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  font-weight: 600;
  border-left: none;
}

.nav-icon {
  font-size: 20px;
}

.nav-text {
  font-size: 16px;
}

/* 主内容区 */
.profile-content {
  flex: 1;
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  min-height: 600px;
  transition: all var(--transition-normal);
  padding: var(--spacing-xl);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.profile-content:hover {
  box-shadow: var(--shadow-md);
  border-color: rgba(242, 163, 58, 0.3);
}

/* 个人资料编辑表单样式 */
.profile-edit-form {
  background-color: var(--card-background);
  border-radius: var(--border-radius-md);
  padding: var(--spacing-xl);
  box-shadow: var(--box-shadow);
}

.form-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-xl);
  color: var(--text-primary);
}

.form-group {
  margin-bottom: var(--spacing-lg);
}

.form-group label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.form-input,
.form-textarea {
  width: 100%;
  padding: var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-sm);
  background-color: var(--input-background);
  color: var(--text-primary);
  font-size: var(--font-size-md);
  transition: border-color var(--transition-speed) ease;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--primary-color);
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

/* 头像上传样式 */
.avatar-upload {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.avatar-preview {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  background-color: #1a1a1a;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid var(--border-color);
  transition: border-color var(--transition-speed) ease;
  background-repeat: no-repeat;
}

.avatar-preview:hover {
  border-color: var(--primary-color);
}

.upload-icon {
  font-size: var(--font-size-xl);
  color: var(--text-secondary);
}

.avatar-input {
  display: none;
}

/* 更改头像按钮样式 */
.change-avatar-btn {
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  padding: var(--spacing-sm) var(--spacing-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-speed) ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transform: translateY(0);
}

/* 错误信息提示框样式 */
.error-message-box {
  background-color: rgba(255, 76, 76, 0.1);
  border: 1px solid #ff4c4c;
  border-radius: var(--border-radius-sm);
  padding: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.error-icon {
  color: #ff4c4c;
  font-size: var(--font-size-lg);
}

.error-text {
  color: var(--text-primary);
  font-size: var(--font-size-md);
}

.change-avatar-btn:hover {
  background-color: var(--primary-hover);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.change-avatar-btn:active {
  transform: translateY(1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 表单按钮样式 */
.form-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
  justify-content: flex-end;
}

.btn {
  padding: var(--spacing-md) var(--spacing-xl);
  border: none;
  border-radius: var(--border-radius-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--transition-speed) ease;
  font-size: var(--font-size-md);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transform: translateY(0);
}

.btn-save {
  background-color: var(--primary-color);
  color: white;
}

.btn-save:hover {
  background-color: var(--primary-hover);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.btn-save:active {
  transform: translateY(1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.btn-cancel {
  background-color: var(--background-secondary);
  color: var(--text-primary);
}

.btn-cancel:hover {
  background-color: var(--background-tertiary);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.btn-cancel:active {
  transform: translateY(1px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 内容列表样式 */
.content-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  flex: 1;
}

.content-item {
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  padding: var(--spacing-lg);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
}

.content-item:hover {
  box-shadow: var(--shadow-sm);
  border-color: rgba(242, 163, 58, 0.3);
}

.user-info-card:hover {
  transform: translateY(0);
  box-shadow: var(--shadow-md);
}

/* 头像样式 - 与首页风格一致 */
.avatar-container {
  flex-shrink: 0;
}

.avatar {
  position: relative;
  width: 104px;
  height: 104px;
  border-radius: 50%;
  overflow: hidden;
  z-index: 2;
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #1a1a1a;
  border: 2px solid var(--border-color);
}

.profile-avatar {
  width: 100%;
  height: 100%;
  min-width: 100%;
  min-height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform var(--transition-normal);
}

.avatar:hover .profile-avatar {
  transform: scale(1.05);
}

/* 移除原有的头像边框，直接在.avatar容器上设置边框 */
.avatar-border {
  display: none;
}

.upload-icon {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background-color: var(--accent-primary);
  color: white;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  z-index: 4;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.upload-icon:hover {
  transform: scale(1.1);
  background-color: var(--accent-hover);
}

/* 用户详情 - 二次元风格 */
.user-details {
  flex: 1;
}

.username {
  margin: 0;
  font-size: 28px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.username::after {
  content: '🌟';
  font-size: 20px;
  animation: float 2s ease-in-out infinite;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  font-size: 14px;
  color: var(--text-secondary);
}

/* 二次元风格等级徽章 */
.level-badge {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(255, 119, 170, 0.3);
  position: relative;
  overflow: hidden;
}

.level-badge::before {
  content: '✨';
  position: absolute;
  left: 5px;
  font-size: 12px;
  animation: float 1.5s ease-in-out infinite;
}

.bio {
  margin: var(--spacing-lg) 0 var(--spacing-xl);
  color: var(--text-secondary);
  line-height: 1.6;
  font-size: 15px;
  background-color: var(--bg-tertiary);
  padding: var(--spacing-lg);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  width: 100%;
  box-sizing: border-box;
}

/* 统计信息 - 确保格式一致 */
.stats-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: var(--spacing-xl) 0;
  padding: var(--spacing-lg);
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  width: 100%;
  box-sizing: border-box;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-normal);
  cursor: pointer;
  flex: 1;
}

.stat-item:hover {
  transform: translateY(0);
}

.stat-number {
  font-size: 20px;
  font-weight: 600;
  color: var(--accent-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  line-height: 1;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background-color: var(--border-color);
  margin: 0 var(--spacing-md);
}

/* 编辑资料按钮 - 与首页风格一致 */
.edit-profile-btn {
  background-color: var(--accent-primary);
  color: white;
  border: none;
  padding: var(--spacing-md) var(--spacing-xl);
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

.edit-profile-btn:hover {
  background-color: var(--accent-hover);
  transform: translateY(0);
  box-shadow: var(--shadow-md);
}

.edit-profile-btn:active {
  transform: translateY(0);
}

/* 数据统计卡片 - 与首页风格一致 */
.stats-card {
  max-width: 800px;
  margin: var(--spacing-xxl) auto;
  padding: var(--spacing-xl);
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius-large);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
}

.stats-card:hover {
  transform: translateY(0);
  box-shadow: var(--shadow-md);
  border-color: rgba(242, 163, 58, 0.3);
}

/* 标题样式 - 与首页风格一致 */
.section-title {
  margin: 0 0 var(--spacing-xl) 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.section-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 24px;
  background-color: var(--accent-primary);
  margin-right: var(--spacing-sm);
  border-radius: 2px;
}

/* 活动统计 - 与首页风格一致 */
.activity-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.activity-item {
  text-align: center;
  padding: var(--spacing-lg);
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  transition: all var(--transition-normal);
  cursor: pointer;
}

.activity-item:hover {
  transform: translateY(0);
  background-color: var(--bg-secondary);
  box-shadow: var(--shadow-sm);
}

.activity-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color), var(--secondary-color));
}

.activity-icon {
  display: block;
  font-size: 28px;
  margin-bottom: 10px;
  animation: float 3s ease-in-out infinite;
}

.activity-number {
  display: block;
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-color);
  margin-bottom: 5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.activity-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* 活跃度进度条 - 二次元风格 */
.activity-progress {
  margin-top: 25px;
  padding: 20px;
  background-color: rgba(135, 206, 235, 0.05);
  border-radius: var(--sm-radius);
  border: 1px solid var(--border-color);
  position: relative;
}

.activity-progress::before {
  content: '🔥';
  position: absolute;
  right: 20px;
  top: 20px;
  font-size: 20px;
  animation: float 2s ease-in-out infinite;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 16px;
  color: var(--text-primary);
  font-weight: 600;
}

.progress-bar {
  width: 100%;
  height: 14px;
  background-color: #f0f0ff;
  border-radius: 7px;
  overflow: hidden;
  border: 2px solid var(--border-color);
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-color), var(--secondary-color), var(--accent-color));
  border-radius: 5px;
  transition: width 1s ease;
  position: relative;
  overflow: hidden;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-value {
  text-align: right;
  font-size: 16px;
  color: var(--primary-color);
  font-weight: 700;
  margin-top: 8px;
}



/* 内容列表 - 二次元风格 */
.content-list {
  padding: 25px;
}

.content-item {
  padding: 25px 0;
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
  transition: transform 0.3s ease, padding-left 0.3s ease;
  position: relative;
}

.content-item:last-child {
  border-bottom: none;
}

.content-item:hover {
  transform: translateX(10px);
  padding-left: 10px;
  background-color: rgba(255, 119, 170, 0.03);
}

.content-item:hover::before {
  content: '';
  position: absolute;
  left: 0;
  top: 25px;
  bottom: 25px;
  width: 4px;
  background: linear-gradient(to bottom, var(--primary-color), var(--secondary-color));
  border-radius: 2px;
}

.content-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 700;
  transition: color 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.content-title:hover {
  color: var(--primary-color);
}

.content-title::before {
  content: '📌';
  font-size: 16px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.content-item:hover .content-title::before {
  opacity: 1;
}

.content-excerpt {
  margin: 0 0 15px 0;
  color: var(--text-secondary);
  line-height: 1.7;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  padding-left: 24px;
}

.content-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--text-secondary);
  padding-left: 24px;
}

/* 关注用户卡片样式 */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  transition: all var(--transition-normal);
}

.user-card:hover {
  background-color: var(--bg-secondary);
  box-shadow: var(--shadow-sm);
}

.user-avatar-small {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-bio {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.unfollow-btn {
  padding: 8px 16px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.unfollow-btn:hover {
  background-color: var(--accent-danger);
  color: white;
  border-color: var(--accent-danger);
}

/* 专栏样式 */
.column-desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 8px 0;
  line-height: 1.6;
}

.article-count,
.subscriber-count {
  margin-right: 16px;
  font-size: 14px;
  color: var(--text-tertiary);
}

.content-stats {
  display: flex;
  gap: 20px;
}

.content-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: color 0.3s ease;
}

.content-stats span:hover {
  color: var(--primary-color);
}

.answer-question {
  color: var(--primary-color);
  margin-bottom: 12px;
  font-weight: 600;
  font-size: 16px;
  padding-left: 24px;
  position: relative;
}

.answer-question::before {
  content: '💬';
  position: absolute;
  left: 0;
  font-size: 16px;
}

.collection-type {
  background-color: rgba(135, 206, 235, 0.2);
  color: var(--secondary-color);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  display: inline-block;
  margin-bottom: 12px;
  margin-left: 24px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.collection-type:hover {
  background-color: rgba(135, 206, 235, 0.3);
  transform: scale(1.05);
}

/* 响应式设计 - 增强二次元风格 */
@media (max-width: 768px) {
  .profile-container {
    padding-bottom: 30px;
  }
  
  .profile-header {
    height: 180px;
  }
  
  .user-info-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
    margin-top: -70px;
    padding: 20px;
    gap: 20px;
  }
  
  .username {
    font-size: 24px;
    justify-content: center;
  }
  
  .user-meta {
    flex-direction: column;
    gap: 10px;
  }
  
  .bio {
    font-size: 14px;
    padding: var(--spacing-md);
    background-color: var(--bg-tertiary);
    color: var(--text-secondary);
    border: 1px solid var(--border-color);
    border-radius: var(--border-radius);
  }
  
  .stats-row {
    justify-content: center;
    gap: 20px;
    padding: 12px;
  }
  
  .stat-number {
    font-size: 20px;
  }
  
  .activity-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }
  
  .activity-item {
    padding: 15px;
  }
  
  .activity-number {
    font-size: 28px;
  }
  
  .tabs-header {
    padding: 0 5px;
  }
  
  .tab-btn {
    padding: 15px 10px;
    font-size: 14px;
  }
  
  .content-list {
    padding: 20px;
  }
  
  .content-item {
    padding: 20px 0;
  }
  
  .content-title {
    font-size: 18px;
  }
  
  .content-meta {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
    padding-left: 0;
  }
  
  .content-excerpt,
  .content-title,
  .answer-question,
  .collection-type {
    padding-left: 0;
  }
  
  .content-title::before {
    display: none;
  }
  
  .answer-question::before {
    display: none;
  }
}

@media (max-width: 480px) {
  .profile-header {
    height: 150px;
  }
  
  .avatar {
    width: 110px;
    height: 110px;
  }
  
  .user-info-card {
    margin-top: -60px;
    padding: 15px;
  }
  
  .stats-row {
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .activity-stats {
    grid-template-columns: 1fr;
  }
  
  .tab-btn {
    flex-direction: column;
    padding: 12px 5px;
    gap: 4px;
  }
  
  .tab-icon {
    font-size: 16px;
  }
  
  .content-list {
    padding: 15px;
  }
  
  .content-stats {
    flex-wrap: wrap;
    gap: 15px;
  }
  
  .section-title {
    font-size: 20px;
  }
}
</style>