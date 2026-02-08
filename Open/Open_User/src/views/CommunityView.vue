<template>
  <div class="community-container">
    <!-- 顶部导航区 -->
    <header class="community-header">
  <div class="header-content">
    <!-- 平台标识 -->
    <div class="logo-container">
      <h1 class="community-title">
        <span class="logo-text">萌技术社区</span>
      </h1>
    </div>
        
        <!-- 主导航菜单 -->
        <nav class="main-nav">
          <ul class="nav-list">
            <li class="nav-item active">
              <a href="/community" class="nav-link">
                <span class="nav-text">首页</span>
              </a>
            </li>
            <li class="nav-item">
              <a href="/hot-ranking" class="nav-link">
                <span class="nav-text">每日热榜</span>
              </a>
            </li>
            <li class="nav-item">
              <a href="/dynamic" class="nav-link">
                <span class="nav-text">动态</span>
              </a>
            </li>
            <li class="nav-item">
              <a href="/moderator" class="nav-link" @click.prevent="handleModeratorClick">
                <span class="nav-text">风纪委员</span>
              </a>
            </li>
          </ul>
        </nav>
        
        <!-- 右侧功能按钮区 -->
        <div class="function-buttons">
          <!-- 搜索按钮 -->
          <div class="search-container">
            <button class="search-btn" @click="handleSearchClick">
              <i class="fas fa-search search-icon text-neutral-400 text-lg"></i>
              <span class="search-text">搜索</span>
            </button>
          </div>
          <!-- 创作中心按钮 -->
          <a href="/articles/create" class="create-btn" @click.prevent="handleCreateArticle">
            <i class="fas fa-pen create-icon text-primary text-lg"></i>
            <span class="create-text">创作中心</span>
          </a>
          
          <!-- 个人按钮和消息按钮（右对齐） -->
          <div class="icon-buttons">
            <button class="icon-btn notification-btn" @click="handleNotificationClick">
              <i class="fas fa-bell notification-icon text-neutral-400 text-lg"></i>
              <span v-if="unreadMessageCount !== 0 && unreadMessageCount !== null" class="notification-badge">{{ unreadMessageCount }}</span>
            </button>
            
            <!-- 个人中心带下拉菜单 -->
            <div class="user-dropdown">
              <button class="icon-btn user-center-btn">
                <span v-if="!isLoggedIn" class="user-login-text">登录</span>
                <img v-else :src="userAvatar || 'https://picsum.photos/id/64/40/40'" alt="用户头像" class="user-avatar">
              </button>
              <div class="dropdown-menu">
                <button class="dropdown-item" @click="handleProfileClick">
                  <i class="fas fa-user item-icon text-neutral-400 text-sm"></i>
                  <span class="item-text">{{ isLoggedIn ? '个人资料' : '登录' }}</span>
                </button>
                <button v-if="isLoggedIn" class="dropdown-item" @click="handleLogout">
                  <i class="fas fa-sign-out-alt item-icon text-neutral-400 text-sm"></i>
                  <span class="item-text">退出登录</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
    
    <!-- 分类标签区 -->
    <section class="category-section">
      <div class="category-container">
        <div class="category-tabs">
          <button class="category-tab active">
            <i class="fas fa-list tab-icon"></i>
            <span class="tab-text">全部</span>
          </button>
          <button class="category-tab backend">
            <i class="fas fa-server tab-icon"></i>
            <span class="tab-text">后端</span>
          </button>
          <button class="category-tab frontend">
            <i class="fas fa-code tab-icon"></i>
            <span class="tab-text">前端</span>
          </button>
          <button class="category-tab github">
            <i class="fab fa-github tab-icon"></i>
            <span class="tab-text">GitHub</span>
          </button>
          <button class="category-tab ai">
            <i class="fas fa-brain tab-icon"></i>
            <span class="tab-text">人工智能</span>
          </button>
          <button class="category-tab game-dev">
            <i class="fas fa-gamepad tab-icon"></i>
            <span class="tab-text">游戏开发</span>
          </button>
          <button class="category-tab more">
            <i class="fas fa-ellipsis-h tab-icon"></i>
            <span class="tab-text">更多</span>
          </button>
        </div>
      </div>
    </section>
    
    <!-- 主要内容区 -->
    <main class="community-content">
      <!-- 左侧内容 -->
      <div class="content-main">

        
        <!-- 推荐文章列表板块 -->
        <section class="article-list">
          <h3 class="list-title">
            <span class="list-icon">�</span>
            <span class="list-text">推荐文章</span>
          </h3>
          
          <!-- 加载状态 -->
          <div v-if="loading" class="loading-indicator">
            <span>加载推荐文章中...</span>
          </div>
          
          <!-- 错误状态 -->
          <div v-else-if="error" class="error-message">
            <span>{{ error }}</span>
          </div>
          
          <!-- 文章列表 -->
          <div v-else>
            <div 
              v-for="(article, index) in articles" 
              :key="article.id" 
              class="article-item" 
              data-aos="fade-up" 
              :data-aos-delay="index * 100"
              @click="viewArticleDetail(article.id)"
              style="cursor: pointer;"
            >
              <div class="article-content">
                <h3 class="article-title">{{ article.title }}</h3>
                <div class="article-meta">
                  <div class="author-info">
                    <div class="author-avatar" :style="{ backgroundImage: `url('${article.icon || 'https://picsum.photos/id/65/30/30'}')` }"></div>
                    <div class="author-details">
                      <span class="author-name">{{ article.author }}</span>
                      <span class="publish-time">发布时间：{{ article.publishTime }}</span>
                    </div>
                  </div>
                </div>
                <p class="article-excerpt">{{ article.summary }}</p>
              </div>
              <div class="article-thumbnail">
                <div 
                  class="thumbnail-image" 
                  :style="{ backgroundImage: `url('${article.coverImageUrl || 'https://picsum.photos/id/20/300/200'}')` }"
                ></div>
              </div>
              <div class="article-actions">
                <span class="action-item">
                  <i class="fas fa-thumbs-up action-icon" :style="{ color: article.isLoved || article.isLove ? '#ef4444' : '#9ca3af' }"></i>
                  <span class="action-text">{{ article.loveCount }}</span>
                </span>
                <span class="action-item">
                  <i class="fas fa-comment action-icon text-neutral-400"></i>
                  <span class="action-text">{{ article.comments }}</span>
                </span>
                <span class="action-item">
                  <i class="fas fa-eye action-icon text-neutral-400"></i>
                  <span class="action-text">{{ article.viewCount }}</span>
                </span>
                <span class="action-item">
                  <i class="fas fa-share-alt action-icon text-neutral-400"></i>
                  <span class="action-text">分享</span>
                </span>
                <span class="action-item">
                  <i class="fas fa-star action-icon text-neutral-400"></i>
                  <span class="action-text">收藏</span>
                </span>
              </div>
            </div>
          </div>
        </section>
      </div>
      
      <!-- 右侧侧边栏 -->
      <aside class="sidebar">
        <!-- 热门标签 -->
        <section class="sidebar-section hot-tags">
          <h3 class="sidebar-title" data-emoji="🏷️">热门标签</h3>
          <div class="tags-content">
            <div class="tag-item">Vue.js</div>
            <div class="tag-item">React</div>
            <div class="tag-item">TypeScript</div>
            <div class="tag-item">Node.js</div>
            <div class="tag-item">动漫开发</div>
            <div class="tag-item">前端框架</div>
            <div class="tag-item">机器学习</div>
            <div class="tag-item">游戏开发</div>
          </div>
        </section>
        
        <!-- 活跃用户 -->
        <section class="sidebar-section active-users">
          <h3 class="sidebar-title" data-emoji="👥">活跃用户</h3>
          <div class="users-content">
            <div class="user-item">
              <div class="user-avatar active-user-avatar"></div>
              <div class="user-info">
                <div class="user-name">技术达人 | 文章：156</div>
              </div>
            </div>
            <div class="user-item">
              <div class="user-avatar active-user-avatar"></div>
              <div class="user-info">
                <div class="user-name">前端开发者 | 文章：98</div>
              </div>
            </div>
            <div class="user-item">
              <div class="user-avatar active-user-avatar"></div>
              <div class="user-info">
                <div class="user-name">动漫爱好者 | 文章：72</div>
              </div>
            </div>
          </div>
        </section>
        
        <!-- 近期活动 -->
        <section class="sidebar-section recent-activities">
          <h3 class="sidebar-title" data-emoji="📅">近期活动</h3>
          <div class="activities-content">
            <div class="activity-item">
              <div class="activity-time">2023-12-01</div>
              <div class="activity-title">Vue 3 实战 workshop</div>
            </div>
            <div class="activity-item">
              <div class="activity-time">2023-12-15</div>
              <div class="activity-title">动漫技术分享会</div>
            </div>
            <div class="activity-item">
              <div class="activity-time">2023-12-25</div>
              <div class="activity-title">社区年度盛典</div>
            </div>
          </div>
        </section>
        

      </aside>
    </main>
    
    <!-- 页脚 -->
    <footer class="community-footer">
      <div class="footer-content">
        <div class="footer-section footer-about">
          <div class="footer-logo">
            <h2 class="community-title">
              <span class="logo-text">萌技术社区</span>
            </h2>
          </div>
          <p class="footer-description">
            萌技术社区是一个面向动漫爱好者和技术开发者的交流平台，分享动漫相关的技术知识、开发经验和创意作品。
          </p>
        </div>
        
        <div class="footer-section footer-links">
          <h3 class="footer-title">快速链接</h3>
          <ul class="footer-link-list">
            <li class="footer-link-item">
              <a href="#" class="footer-link">首页</a>
            </li>
            <li class="footer-link-item">
              <a href="#" class="footer-link">教程</a>
            </li>
            <li class="footer-link-item">
              <a href="#" class="footer-link">创作中心</a>
            </li>
            <li class="footer-link-item">
              <a href="#" class="footer-link">关于我们</a>
            </li>
          </ul>
        </div>
        
        <div class="footer-section footer-contact">
          <h3 class="footer-title">联系我们</h3>
          <ul class="footer-contact-list">
            <li class="footer-contact-item">
              <span class="contact-icon">📧</span>
              <a href="mailto:contact@example.com" class="contact-link">contact@example.com</a>
            </li>
            <li class="footer-contact-item">
              <span class="contact-icon">🐦</span>
              <a href="#" class="contact-link">Twitter</a>
            </li>
            <li class="footer-contact-item">
              <span class="contact-icon">📘</span>
              <a href="#" class="contact-link">Facebook</a>
            </li>
            <li class="footer-contact-item">
              <span class="contact-icon">📷</span>
              <a href="#" class="contact-link">Instagram</a>
            </li>
          </ul>
        </div>
      </div>
      
      <div class="footer-bottom">
        <p class="footer-copyright">
          © 2023 萌技术社区. 保留所有权利.
        </p>
      </div>
    </footer>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'CommunityView',
  data() {
      return {
        // 推荐文章列表
        articles: [],
        // 加载状态
        loading: false,
        // 错误信息
        error: null,
        isLoggedIn: false,
        userAvatar: '',
        // 未读消息数量
        unreadMessageCount: null,
        // 当前用户ID
        currentUserId: null
      }
    },
  mounted() {
    // 组件挂载时检查登录状态
    this.checkLoginStatus()
    // 加载文章数据
    this.loadArticles()
    // 如果已登录，获取用户头像和未读消息数量
    if (this.isLoggedIn) {
      this.getUserAvatar()
      this.getCurrentUserId()
      // 获取用户ID后再获取未读消息数量
      this.$nextTick(() => {
        if (this.currentUserId) {
          this.getUnreadMessageCount()
        }
      })
    }
  },
  methods: {
    // 检查登录状态
    checkLoginStatus() {
      this.isLoggedIn = !!localStorage.getItem('isLoggedIn') || !!sessionStorage.getItem('isLoggedIn')
      console.log('登录状态:', this.isLoggedIn ? '已登录' : '未登录')
    },
    // 处理通知按钮点击事件
    handleNotificationClick() {
      // 由于通知页面已移除，在新标签页打开私信页面
      window.open('/messages', '_blank')
    },

    // 获取当前用户ID
    getCurrentUserId() {
      try {
        // 从localStorage或sessionStorage获取用户信息
        const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr)
          if (userInfo.id || userInfo.userId) {
            this.currentUserId = userInfo.id || userInfo.userId
            console.log('当前用户ID:', this.currentUserId)
          }
        }
      } catch (error) {
        console.error('获取用户ID失败:', error)
      }
    },
    // 获取用户头像
    async getUserAvatar() {
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {}
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`)
        }
        
        // 使用新的API端点获取头像路径
        const response = await axios.get('http://localhost:8081/api/community/user/avatar', { headers })
        console.log('获取用户头像API响应数据:', response.data)
        
        // 处理后端返回的Map集合
        if (response.data && response.data.data) {
          const userData = response.data.data
          console.log('用户数据Map:', userData)
          
          // 设置头像路径
          if (userData.icon) {
            this.userAvatar = userData.icon
            console.log('设置头像路径:', userData.icon)
          }
          
          // 如果还没有用户ID，尝试从用户数据中获取
          if (!this.currentUserId && (userData.id || userData.userId)) {
            this.currentUserId = userData.id || userData.userId
            console.log('从用户数据中获取用户ID:', this.currentUserId)
            // 获取到用户ID后，获取未读消息数量
            this.getUnreadMessageCount()
          }
        }
      } catch (error) {
        console.error('获取用户头像失败:', error)
        // 保持默认头像
      }
    },
    // 获取未读消息数量
    async getUnreadMessageCount() {
      if (!this.currentUserId) {
        console.error('无法获取未读消息数量：用户ID为空')
        return
      }
      
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {}
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
        }
        
        // 调用未读消息数量接口
        const response = await axios.get('http://localhost:8081/api/community/unreadMessage', {
          headers,
          params: {
            userId: this.currentUserId
          }
        })
        
        console.log('获取未读消息数量API响应:', response.data)
        
        // 处理返回结果
        if (response.data && response.data.data !== undefined) {
          this.unreadMessageCount = response.data.data
        } else {
          this.unreadMessageCount = response.data
        }
        
        console.log('未读消息数量:', this.unreadMessageCount)
      } catch (error) {
        console.error('获取未读消息数量失败:', error)
        this.unreadMessageCount = null
      }
    },
    // 加载文章数据
    async loadArticles() {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {}
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`)
        }
        
        // 调用实际的推荐文章接口
        const response = await axios.get('http://localhost:8081/api/community/Recommend', { headers })
        console.log('推荐文章接口响应:', response.data)
        
        // 处理返回的数据，检查是否包含data字段（根据其他API的返回格式）
        const articlesData = response.data && response.data.data ? response.data.data : response.data
        
        this.articles = articlesData.map(article => ({
          id: article.id,
          author: article.author,
          icon: article.icon,
          title: article.title,
          summary: article.summary,
          coverImageUrl: article.coverImageUrl,
          publishTime: article.publishTime,
          isLoved: article.isLoved || false,
          loveCount: article.loveCount || 0,
          viewCount: article.viewCount || 0,
          comments: article.comments || 0
        }))
        
        this.loading = false
      } catch (err) {
        console.error('加载推荐文章失败:', err)
        // 提供更详细的错误信息
        if (err.response && err.response.status === 403) {
          this.error = '加载推荐文章失败：权限不足，请先登录'
        } else if (err.response && err.response.status === 404) {
          this.error = '加载推荐文章失败：接口不存在'
        } else if (err.request) {
          this.error = '加载推荐文章失败：无法连接到服务器，请检查网络'
        } else {
          this.error = '加载推荐文章失败，请稍后重试'
        }
        this.loading = false
      }
    },
    // 处理写文章按钮点击
    handleCreateArticle() {
      if (!this.isLoggedIn) {
        // 未登录，显示提示框，阻止跳转
        alert('请先登录后再写文章')
        return false
      }
      // 已登录，在新标签页打开创作中心
      window.open('/articles/create', '_blank')
    },
    // 处理个人资料/登录按钮点击
    handleProfileClick() {
      if (this.isLoggedIn) {
        // 已登录，在新标签页打开个人资料页
        window.open('/profile', '_blank')
      } else {
        // 未登录，在新标签页打开登录页
        window.open('/login', '_blank')
      }
    },
    // 查看文章详情
    viewArticleDetail(articleId) {
      // 跳转到文章详情页面
      this.$router.push(`/article/${articleId}`)
    },
    // 退出登录
    async handleLogout() {
      try {
        // 获取token
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        // 准备请求头
        const headers = {}
        
        // 如果有token，添加到请求头
        if (token && typeof token === 'string') {
          const cleanToken = token.trim()
          headers['Authorization'] = `Bearer ${cleanToken}`
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`)
        }
        
        // 调用后端退出登录接口，带上认证头
        await axios.post('http://localhost:8081/api/auth/loginOut', {}, { headers })
      } catch (error) {
        console.error('退出登录API调用失败:', error)
        // 即使API调用失败，也清除本地登录状态
      }
      
      // 清除登录状态
      localStorage.removeItem('isLoggedIn')
      sessionStorage.removeItem('isLoggedIn')
      localStorage.removeItem('userInfo')
      sessionStorage.removeItem('userInfo')
      localStorage.removeItem('token')
      sessionStorage.removeItem('token')
      
      // 更新登录状态
      this.isLoggedIn = false
      
      // 保持在首页（不跳转）
      // 刷新页面以确保所有组件都能正确更新状态
      window.location.reload()
    },
    // 处理搜索按钮点击
    handleSearchClick() {
      // 在新标签页中打开搜索页面
      window.open('/search', '_blank')
    },
    // 处理风纪委员入口点击
    handleModeratorClick() {
      if (!this.isLoggedIn) {
        // 未登录，显示提示框，阻止跳转
        alert('请先登录后再进入风纪委员管理界面')
        return false
      }
      // 已登录，跳转到风纪委员管理界面
      this.$router.push('/moderator')
    }
  }
}
</script>

<style scoped>
/* CommunityView.vue 特定样式 */
.community-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-primary);
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
}

/* 顶部导航区 */
.community-header {
  /* 优化：使用指定的背景色 */
  background-color: #1E1E2E;
  border-bottom: 1px solid #9D8CF0;
  /* 优化：添加指定的底部阴影 */
  box-shadow: 0 2px 8px rgba(184,146,255,0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.logo-container {
  flex-shrink: 0;
}

.community-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.logo-text {
  color: var(--accent-primary);
}

/* 导航菜单 */
.main-nav {
  flex: 1;
  margin: 0 var(--spacing-lg);
}

.nav-list {
  display: flex;
  gap: var(--spacing-md);
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav-item {
  position: relative;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  text-decoration: none;
  /* 优化：使用指定的未选中文字色 */
  color: #F8FAFC;
  font-size: 16px;
  font-weight: 500;
  padding: var(--spacing-sm) var(--spacing-md); /* 调整：左右内边距增至16px */
  border-radius: var(--border-radius);
  transition: all var(--transition-normal);
}

.nav-link:hover {
  /* 优化：hover时使用主色 */
  color: #976EFF;
}

.nav-item.active .nav-link {
  /* 优化：使用指定的选中文字色 */
  color: #976EFF;
  font-weight: 600;
}

/* 搜索按钮 */
.search-container {
  flex-shrink: 0;
}

.search-btn,
.create-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
  outline: none;
  text-decoration: none;
  height: 40px;
  min-height: 40px;
}

.search-btn:hover,
.create-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  background-color: var(--accent-primary);
}

.search-icon {
  font-size: 16px;
}

.search-text {
  font-size: 14px;
}

/* 功能按钮区 */
.function-buttons {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-shrink: 0;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
  text-decoration: none;
}

.create-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* 图标按钮 */
.icon-buttons {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.icon-btn {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
}

.icon-btn:hover {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  border-color: var(--accent-primary);
}

/* 通知徽章 */
.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: var(--accent-error);
  color: white;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 50%;
  min-width: 20px;
  text-align: center;
}

/* 未登录时的登录文字 */
.user-login-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-align: center;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 用户下拉菜单 */
.user-dropdown {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: var(--spacing-sm);
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  min-width: 150px;
  z-index: 1000;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all var(--transition-normal);
}

.user-dropdown:hover .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
  width: 100%;
  text-align: left;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  transition: transform var(--transition-normal);
}

.user-avatar:hover {
  transform: scale(1.1);
}

.user-icon {
  font-size: 24px;
}

.dropdown-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
}

/* 分类标签区 */
.category-section {
  background-color: var(--bg-primary);
  padding: var(--spacing-md) 0 0 0; /* 移除底部内边距，进一步减小与推荐文章的间距 */
}

.category-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
}

.category-tabs {
  display: flex;
  gap: 12px; /* 调整：标签间距至12px */
  overflow-x: auto;
  padding: var(--spacing-sm) 0;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: 14px 19px; /* 调整：内边距上下14px+左右19px（原基础上+2px），扩大点击区域 */
  background-color: #4E4E7E; /* 浅紫背景色 */
  border: 1px solid transparent; /* 透明边框 */
  border-radius: 4px; /* 4px圆角 */
  color: #B0B0C0; /* 浅灰文字色 */
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--transition-normal);
}

.tab-icon {
  font-size: 16px; /* 图标大小 */
  color: #C8B8F0; /* 浅紫图标色 */
}

/* 技术分类标签颜色保持不变 */
.category-tab.backend {
  background-color: #8BB8F0;
  color: #1E1E2E;
}

.category-tab.frontend {
  background-color: #F0A8C8;
  color: #1E1E2E;
}

.category-tab.ai {
  background-color: #8CF0C8;
  color: #1E1E2E;
}

.category-tab.github {
  background-color: #4E4E7E;
  color: #B0B0C0;
}

.category-tab.game-dev {
  background-color: #4E4E7E;
  color: #B0B0C0;
}

.category-tab.more {
  background-color: #4E4E7E;
  color: #B0B0C0;
}

/* 激活状态样式 */
.category-tab.active {
  background-color: #C8B8F0; /* 浅紫高亮背景 */
  color: #1E1E2E; /* 深色文字 */
  border: 1px solid #C8B8F0;
  box-shadow: 0 2px 8px rgba(200,184,240,0.3);
}

.category-tab:hover {
  background-color: #5E5E8E; /* 加深背景色 */
  color: #C8B8F0; /* 浅紫文字色 */
  border-color: #C8B8F0;
  box-shadow: 0 2px 8px rgba(200,184,240,0.2);
}

/* 激活状态的hover效果 */
.category-tab.active:hover {
  background-color: #D8C8F0; /* 更浅的紫色 */
  box-shadow: 0 2px 10px rgba(200,184,240,0.4);
}

/* 主要内容区 */
.community-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-md) var(--spacing-lg); /* 缩小顶部内边距，减小分类按钮与推荐文章间距 */
  display: flex;
  gap: var(--spacing-xl);
  flex: 1;
}

.content-main {
  flex: 1;
}


/* 文章列表 */
.article-list {
  margin-top: calc(var(--spacing-sm) - 2px); /* 将全部按钮到推荐文章文本的间距缩减一半 */
  margin-bottom: var(--spacing-lg); /* 缩小推荐文章区域的底部间距 */
  padding-top: var(--spacing-xs); /* 将全部按钮到推荐文章文本的间距缩减一半 */
}

.list-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 16px; /* 缩小推荐文章标题字体 */
  font-weight: bold; /* 加粗 */
  color: #C8B8F0; /* 浅紫高亮色 */
  margin-bottom: calc(var(--spacing-xs) + 1px); /* 将推荐文章文本和文字卡片之间的间距缩减一半 */
  padding-bottom: 4px; /* 增加底部内边距 */
  border-bottom: 1px solid #C8B8F0; /* 增加1px同色细分割线 */
}

.article-item {
  /* 垂直布局 */
  display: block;
  /* 使用导航栏背景色 */
  background: var(--bg-tertiary);
  /* 优化：使用指定的边框 */
  border: 1px solid #9D8CF0;
  border-radius: 6px;
  padding: 16px; /* 缩减卡片内边距 */
  margin-bottom: 12px; /* 卡片之间上下间距增加至12px（原基础上+8px） */
  /* 优化：使用指定的默认阴影 */
  box-shadow: 0 4px 12px rgba(184,146,255,0.12);
  /* 过渡动画 */
  transition: all 300ms ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.article-item:hover {
  /* 增强的提升效果 */
  transform: translateY(-3px);
  /* 优化：使用指定的hover背景色 */
  background: #2C3A52;
  /* 优化：使用指定的hover阴影 */
  box-shadow: 0 6px 18px rgba(184,146,255,0.18);
  /* 优化：边框颜色变化 */
  border-color: rgba(184,146,255,0.2);
}

.article-content {
  margin-bottom: 12px; /* 缩减内容区底部间距 */
}

.article-title {
  font-size: 21px; /* 字号增大2号（基于原字号） */
  font-weight: bold; /* 加粗 */
  color: #C8B8F0; /* 浅紫高亮色 */
  margin-bottom: 12px; /* 增加标题底部间距 */
  line-height: 1.4;
  transition: color var(--transition-normal);
}

.article-item:hover .article-title {
  /* 优化：hover时使用稍深的紫色 */
  color: #B8A8E0;
}

.article-excerpt {
  /* 优化：使用指定的正文/摘要颜色 */
  color: #B0B0C0; /* 浅灰 */
  margin-bottom: 12px; /* 缩减摘要底部间距 */
  line-height: 1.4; /* 调整：标题与摘要的行距调至1.4倍 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-wrap: balance;
}

.article-meta {
  margin-bottom: 12px; /* 缩减元数据底部间距 */
}

.article-meta .author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background-size: cover;
  background-position: center;
  margin-right: 8px; /* 调整：头像与作者名的间距调至8px */
}

.author-info {
  display: flex;
  align-items: center;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: 13px; /* 字号缩小1号 */
  font-weight: 500;
  /* 优化：使用指定的作者名颜色（关键数据） */
  color: #B0B0C0; /* 浅灰色 */
  display: block;
}

.publish-time {
  font-size: 11px; /* 字号缩小1号 */
  color: #B0B0C0; /* 浅灰色 */
  display: block;
}

.article-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.action-button {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  transition: color var(--transition-normal);
}

.action-button:hover {
  color: var(--accent-primary);
}

.action-text {
  font-size: 12px; /* 小号字体 */
  color: #808090; /* 更浅灰 */
}

/* 缩略图样式，根据设计要求调整 */
.article-thumbnail {
  width: 100%;
  height: 256px;
  margin-bottom: 16px;
  overflow: hidden;
  border-radius: 12px;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  object-fit: cover;
  transition: transform var(--transition-normal);
}

.article-item:hover .thumbnail-image {
  transform: scale(1.05);
}

.article-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 0;
  font-size: 13px;
  /* 优化：使用指定的关键数据颜色 */
  color: #B892FF;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: color var(--transition-normal);
}

.stat-item:hover {
  /* 优化：hover时使用主色 */
  color: #976EFF;
}

.stat-icon {
  font-size: 14px;
}

.article-actions {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 14px; /* 按钮间间距增加8px（从6px到14px） */
  cursor: pointer;
  font-size: 13px;
  color: var(--text-tertiary);
  transition: color var(--transition-normal);
}

.action-item:hover {
  color: var(--accent-primary);
}

.action-icon {
  font-size: 15.4px; /* 图标放大10% */
}

/* 右侧侧边栏 */
.sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: calc(var(--spacing-md) + 13px); /* 调整：模块之间增加8px空白间隔（从15px增加到23px） */
  align-self: flex-start;
}

.sidebar-section {
  background-color: #4E4E7E;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: var(--spacing-xl);
  transition: all var(--transition-normal);
}

.sidebar-section:hover {
  box-shadow: var(--shadow-card-hover);
  border-color: var(--border-color-light-primary);
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 18px;
  font-weight: bold; /* 加粗 */
  color: #C8B8F0; /* 浅紫高亮色 */
  margin-bottom: var(--spacing-lg);
  position: relative;
}

.sidebar-title::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 50px;
  height: 2px;
  background-color: var(--color-primary); /* 标题下划线：#B892FF */
  border-radius: 1px;
}

/* 平台介绍 */
.platform-intro {
  text-align: center;
}

.intro-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.intro-text {
  color: var(--text-tertiary);
  line-height: 1.7;
}

.intro-stats {
  display: flex;
  justify-content: space-around;
  padding: var(--spacing-lg) 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

.stat-number {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-primary);
}

.stat-label {
  display: block;
  font-size: 14px;
  color: var(--text-tertiary);
  margin-top: var(--spacing-xs);
}

.join-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background-color: var(--color-primary); /* 默认#B892FF（背景）+#F8FAFC（文字） */
  color: var(--bg-primary);
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-normal);
}



/* 热门标签样式 */
.tags-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  padding: 8px 14px; /* 每个标签增加2px内边距（原基础上+2px） */
  background-color: rgba(184, 146, 255, 0.1);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  font-size: 13px;
  color: var(--accent-primary);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.tag-item:hover {
  background-color: rgba(184, 146, 255, 0.2);
  border-color: var(--accent-primary);
  transform: translateY(-2px);
}

/* 活跃用户样式 */
.users-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.user-item:hover {
  transform: translateX(4px);
  color: var(--accent-primary);
}

.active-user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--border-color);
  background-image: linear-gradient(135deg, var(--color-primary), var(--accent-primary));
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 12px;
  font-weight: normal;
  color: #B0B0C0; /* 浅灰色小字 */
}

.user-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.stat-text {
  color: #B0B0C0; /* 浅灰 */
}

.stat-badge {
  display: inline-block;
  background-color: #C8B8F0; /* 浅紫背景 */
  color: #1E1E2E; /* 深色文字 */
  font-size: 10px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 10px; /* 圆角，形成小圆点效果 */
  min-width: 24px;
  text-align: center;
  line-height: 16px;
}

/* 近期活动样式 */
.activities-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  transition: all var(--transition-normal);
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.activity-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.activity-item:hover {
  color: var(--accent-primary);
}

.activity-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.activity-title {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.join-btn:hover {
  background: var(--color-primary-dark); /* hover#976EFF+ 阴影 */
  opacity: 1;
  transform: translateY(-2px);
  box-shadow: var(--shadow-button-hover); /* 按钮hover阴影：0 2px 8px rgba(184,146,255,0.2) */
}

/* 精选教程 */
.tutorial-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.tutorial-item {
  display: flex;
  gap: var(--spacing-md);
  /* 玻璃态效果 */
  background: rgba(37, 42, 54, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  /* 渐变背景 */
  background: linear-gradient(135deg, rgba(37, 42, 54, 0.8), rgba(40, 45, 58, 0.7));
  border: 1px solid rgba(65, 72, 88, 0.4);
  border-radius: var(--border-radius);
  padding: var(--spacing-sm);
  /* 精致阴影 */
  box-shadow: 
    0 2px 6px rgba(0, 0, 0, 0.15),
    0 0 10px rgba(30, 35, 45, 0.25);
  /* 提升动画 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.tutorial-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  transition: left 0.5s;
}

.tutorial-item:hover {
  /* 增强的提升效果 */
  transform: translateY(-4px);
  /* 增强的精致阴影 */
  box-shadow: 
    0 8px 16px rgba(0, 0, 0, 0.2),
    0 0 20px rgba(30, 35, 45, 0.35);
  /* 边框颜色变化 */
  border-color: rgba(141, 167, 190, 0.4);
}

.tutorial-item:hover::before {
  left: 100%;
}

.tutorial-thumbnail {
  width: 100px;
  height: 70px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--border-radius);
}

.tutorial-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.tutorial-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tutorial-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-tertiary);
}

.tutorial-level {
  padding: 2px 8px;
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  font-weight: 500;
}

.view-more-link {
  display: block;
  text-align: center;
  color: var(--accent-primary);
  text-decoration: none;
  font-size: 14px;
  padding: var(--spacing-sm);
  margin-top: var(--spacing-md);
  transition: all var(--transition-normal);
}

.view-more-link:hover {
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
}

/* 热门文章 */
.hot-article-list {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: var(--spacing-md);
}

.hot-article-item {
  display: flex;
  gap: 8px; /* 调整：排名数字与文章标题的间距调至8px */
  /* 玻璃态效果 */
  background: rgba(37, 42, 54, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  /* 渐变背景 */
  background: linear-gradient(135deg, rgba(37, 42, 54, 0.8), rgba(40, 45, 58, 0.7));
  border: 1px solid rgba(65, 72, 88, 0.4);
  border-radius: var(--border-radius);
  padding: 12px 8px; /* 调整：列表项上下内边距调至12px，左右增加8px */
  width: calc(50% - var(--spacing-sm));
  box-sizing: border-box;
  /* 精致阴影 */
  box-shadow: 
    0 2px 6px rgba(0, 0, 0, 0.15),
    0 0 10px rgba(30, 35, 45, 0.25);
  /* 提升动画 */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.hot-article-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  transition: left 0.5s;
}

.hot-article-item:hover {
  /* 增强的提升效果 */
  transform: translateY(-4px);
  /* 增强的精致阴影 */
  box-shadow: 
    0 8px 16px rgba(0, 0, 0, 0.2),
    0 0 20px rgba(30, 35, 45, 0.35);
  /* 边框颜色变化 */
  border-color: rgba(141, 167, 190, 0.4);
}

.hot-article-item:hover::before {
  left: 100%;
}

.hot-article-number {
  width: 20px;
  height: 20px;
  background-color: var(--accent-hot-rank); /* 热门排名：#87E8DE（浅青） */
  color: var(--bg-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.hot-article-content {
  flex: 1;
}

.hot-article-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-article-meta {
  font-size: 11px;
  color: var(--text-tertiary);
}

/* 活动公告 */
.event-banner {
  position: relative;
  border-radius: var(--border-radius);
  overflow: hidden;
  margin-bottom: var(--spacing-md);
  box-shadow: 0 4px 16px var(--color-primary-transparent-15); /* Banner底部主色阴影 */
}

.event-banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(rgba(26, 35, 50, 0.7), rgba(26, 35, 50, 0.4)); /* 压暗图片突出文字 */
  z-index: 1;
}

.event-image {
  width: 100%;
  height: 150px;
  background-size: cover;
  background-position: center;
  transition: transform var(--transition-normal);
}

.event-banner:hover .event-image {
  transform: scale(1.05);
}

.event-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(26, 35, 50, 0.9), transparent);
  color: var(--text-primary); /* 标题文字 */
  padding: var(--spacing-lg);
  z-index: 2;
}

.event-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: var(--spacing-xs);
  color: var(--text-primary); /* 标题文字 */
}

.event-description {
  font-size: 12px;
  color: var(--text-tertiary); /* 副标题/作者 */
  margin-bottom: var(--spacing-xs);
}

.event-date {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-tertiary); /* 日期信息 */
}

.join-event-btn {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-lg);
  background-color: var(--color-primary); /* 主色背景 */
  color: var(--text-primary); /* 文字颜色 */
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.join-event-btn:hover {
  background-color: var(--color-primary-dark); /* hover时使用主色深色变体 */
  transform: translateY(-2px);
  box-shadow: 0 2px 8px var(--color-primary-transparent-20); /* 主色阴影 */
}

/* 页脚 */
.community-footer {
  background-color: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  padding: var(--spacing-xxl) 0;
  margin-top: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-xxl);
  margin-bottom: var(--spacing-xl);
}

.footer-section {
  flex: 1;
}

.footer-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
}

.footer-description {
  color: var(--text-tertiary);
  line-height: 1.7;
  margin-bottom: var(--spacing-lg);
}

.footer-link-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.footer-link-item {
  margin-bottom: var(--spacing-sm);
}

.footer-link {
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color var(--transition-normal);
}

.footer-link:hover {
  color: var(--accent-primary);
}

.footer-contact-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.footer-contact-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.contact-icon {
  color: var(--accent-primary);
  font-size: 16px;
}

.contact-link {
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color var(--transition-normal);
}

.contact-link:hover {
  color: var(--accent-primary);
}

.footer-bottom {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-color);
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .community-content {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    position: static;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .main-nav {
    width: 100%;
    margin: var(--spacing-md) 0;
  }
  
  .nav-list {
    justify-content: space-between;
    gap: var(--spacing-md);
  }
  
  .search-container {
    width: 100%;
  }
  
  .function-buttons {
    width: 100%;
    justify-content: space-between;
  }
  
  .featured-title {
    font-size: 24px;
  }
  
  .article-item {
    flex-direction: column;
  }
  
  .article-thumbnail {
    width: 100%;
    height: 200px;
  }
  
  .footer-content {
    flex-direction: column;
    gap: var(--spacing-xl);
  }
}
</style>