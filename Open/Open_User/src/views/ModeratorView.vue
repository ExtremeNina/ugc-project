<template>
  <div class="moderator-container">
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
            <li class="nav-item">
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
            <li class="nav-item active">
              <a href="/moderator" class="nav-link">
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
          
          <!-- 写文章按钮 -->
          <button class="create-btn" @click="handleCreateClick">
            <i class="fas fa-pen create-icon text-primary text-lg"></i>
            <span class="create-text">创作中心</span>
          </button>
          
          <!-- 个人按钮和消息按钮（右对齐） -->
          <div class="icon-buttons">
            <button class="icon-btn notification-btn" @click="handleNotificationClick">
              <i class="fas fa-bell notification-icon text-neutral-400 text-lg"></i>
              <span v-if="unreadMessageCount !== 0 && unreadMessageCount !== null" class="notification-badge">{{ unreadMessageCount }}</span>
            </button>
            
            <!-- 个人中心带下拉菜单 -->
            <div class="user-dropdown">
              <button class="icon-btn user-center-btn" @click="toggleDropdown">
                <span v-if="!isLoggedIn" class="user-login-text">登录</span>
                <img v-else :src="userAvatar || 'https://picsum.photos/id/64/40/40'" alt="用户头像" class="user-avatar">
              </button>
              <div class="dropdown-menu" v-if="dropdownVisible">
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
    
    <!-- 风纪委员管理主内容区 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-shield-alt title-icon"></i>
        风纪委员管理中心
      </h1>
      <p class="page-description">管理社区内容，维护良好的运营环境</p>
    </div>
    
    <div class="moderator-content">
      
      <!-- 左侧侧边栏 -->
      <div class="moderator-sidebar">
        <!-- 总体数据卡片 -->
        <div class="sidebar-card overall-stats">
          <div class="card-title">
            <i class="fas fa-chart-bar card-icon"></i>
            总体数据
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">1,234</div>
              <div class="stat-label">文章总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">5,678</div>
              <div class="stat-label">评论总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">9,012</div>
              <div class="stat-label">用户总数</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">345</div>
              <div class="stat-label">举报总数</div>
            </div>
          </div>
        </div>
        
        <!-- 模块管理卡片 -->
        <div class="sidebar-card module-management">
          <div class="card-title">
            <i class="fas fa-tools card-icon"></i>
            模块管理
          </div>
          <div class="module-list">
            <div class="module-item" @click="handleOverview">
              <i class="fas fa-home module-icon overview-icon"></i>
              <div class="module-info">
                <span class="module-name">今日概况</span>
              </div>
            </div>
            <div class="module-item" @click="handleArticleManagement">
              <i class="fas fa-file-alt module-icon article-icon"></i>
              <div class="module-info">
                <span class="module-name">文章管理</span>
                <span class="module-count">{{ pendingArticlesCount }} 篇待审核</span>
              </div>
            </div>
            <div class="module-item" @click="handleCommentManagement">
              <i class="fas fa-comment module-icon comment-icon"></i>
              <div class="module-info">
                <span class="module-name">评论管理</span>
                <span class="module-count">45 条待处理</span>
              </div>
            </div>
            <div class="module-item" @click="handleUserManagement">
              <i class="fas fa-users module-icon user-icon"></i>
              <div class="module-info">
                <span class="module-name">用户管理</span>
                <span class="module-count">12 个违规用户</span>
              </div>
            </div>
            <div class="module-item" @click="handleReportManagement">
              <i class="fas fa-flag module-icon report-icon"></i>
              <div class="module-info">
                <span class="module-name">举报处理</span>
                <span class="module-count">8 件待处理</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 统计趋势卡片 -->
        <div class="sidebar-card stats-trend">
          <div class="card-title">
            <i class="fas fa-arrow-up card-icon"></i>
            统计趋势
          </div>
          <div class="trend-list">
            <div class="trend-item">
              <span class="trend-label">今日新增文章</span>
              <span class="trend-value">+23</span>
            </div>
            <div class="trend-item">
              <span class="trend-label">今日新增评论</span>
              <span class="trend-value">+145</span>
            </div>
            <div class="trend-item">
              <span class="trend-label">今日新增用户</span>
              <span class="trend-value">+89</span>
            </div>
            <div class="trend-item">
              <span class="trend-label">今日新增举报</span>
              <span class="trend-value">+12</span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧主内容区 -->
      <div class="content-main">
        <!-- 今日概况 -->
        <div v-if="activeModule === 'overview'" class="today-overview">
          <h2 class="section-title">今日概况</h2>
          <div class="overview-cards">
            <div class="overview-card">
              <div class="overview-content">
                <h3 class="overview-title">待审核文章</h3>
                <div class="overview-value">{{ pendingArticlesCount }}</div>
                <div class="overview-change">+5 较昨日</div>
              </div>
              <i class="fas fa-file-alt overview-icon article-icon"></i>
            </div>
            <div class="overview-card">
              <div class="overview-content">
                <h3 class="overview-title">待处理评论</h3>
                <div class="overview-value">45</div>
                <div class="overview-change">+12 较昨日</div>
              </div>
              <i class="fas fa-comment overview-icon comment-icon"></i>
            </div>
            <div class="overview-card">
              <div class="overview-content">
                <h3 class="overview-title">违规用户</h3>
                <div class="overview-value">12</div>
                <div class="overview-change">+3 较昨日</div>
              </div>
              <i class="fas fa-users overview-icon user-icon"></i>
            </div>
            <div class="overview-card">
              <div class="overview-content">
                <h3 class="overview-title">待处理举报</h3>
                <div class="overview-value">8</div>
                <div class="overview-change">-2 较昨日</div>
              </div>
              <i class="fas fa-flag overview-icon report-icon"></i>
            </div>
          </div>
        </div>
        
        <!-- 最近待处理事项 -->
        <div v-if="activeModule === 'overview'" class="recent-tasks">
          <h2 class="section-title">最近待处理事项</h2>
          <div class="task-list">
            <div class="task-item">
              <i class="fas fa-file-alt task-icon article-icon"></i>
              <div class="task-content">
                <h3 class="task-title">新文章待审核</h3>
                <p class="task-description">用户"技术爱好者"发布了新文章《前端性能优化技巧》</p>
                <div class="task-meta">
                  <span class="task-time">2小时前</span>
                  <span class="task-status pending">待审核</span>
                </div>
              </div>
            </div>
            <div class="task-item">
              <i class="fas fa-comment task-icon comment-icon"></i>
              <div class="task-content">
                <h3 class="task-title">评论违规</h3>
                <p class="task-description">用户"匿名用户"在文章下发布违规评论</p>
                <div class="task-meta">
                  <span class="task-time">4小时前</span>
                  <span class="task-status pending">待处理</span>
                </div>
              </div>
            </div>
            <div class="task-item">
              <i class="fas fa-flag task-icon report-icon"></i>
              <div class="task-content">
                <h3 class="task-title">内容举报</h3>
                <p class="task-description">用户举报文章《如何快速赚钱》含有违规内容</p>
                <div class="task-meta">
                  <span class="task-time">6小时前</span>
                  <span class="task-status pending">待处理</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 文章管理界面 -->
        <div v-if="activeModule === 'articles'" class="articles-management">
          <div class="articles-header">
            <h2 class="section-title">文章管理</h2>
            <div class="articles-filter">
              <div class="search-box">
                <input 
                  type="text" 
                  v-model="searchKeyword" 
                  placeholder="搜索文章标题..." 
                  @keyup.enter="searchArticles"
                >
                <button @click="searchArticles" class="search-btn">
                  <i class="fas fa-search"></i>
                </button>
              </div>
              <div class="status-filter">
                <select v-model="statusFilter" @change="searchArticles">
                  <option value="">全部状态</option>
                  <option :value="2">待审核</option>
                  <option :value="3">已发布</option>
                  <option :value="0">已拒绝</option>
                </select>
              </div>
            </div>
          </div>
          
          <div class="articles-list-container">
            <div v-if="loading" class="loading-indicator">
              <i class="fas fa-spinner fa-spin"></i> 加载中...
            </div>
            
            <div v-else-if="articles.length === 0" class="empty-state">
              <i class="fas fa-file-alt"></i>
              <p>暂无文章数据</p>
            </div>
            
            <div v-else class="articles-list">
              <div v-for="article in articles" :key="article.id" class="article-item">
                <div class="article-info">
                  <div class="article-cover">
                    <img :src="article.coverImageUrl" :alt="article.title">
                  </div>
                  <div class="article-details">
                    <h3 class="article-title">{{ article.title }}</h3>
                    <div class="article-meta">
                      <span class="meta-item">
                        <i class="fas fa-user"></i> 作者ID: {{ article.authorId }}
                      </span>
                      <span class="meta-item">
                        <i class="fas fa-folder"></i> 分类ID: {{ article.categoryId }}
                      </span>
                      <span class="meta-item">
                        <i class="fas fa-eye"></i> 浏览: {{ article.viewCount }}
                      </span>
                      <span class="meta-item">
                        <i class="fas fa-heart"></i> 点赞: {{ article.loveCount }}
                      </span>
                    </div>
                    <div class="article-time">
                      <span>创建时间: {{ article.createTime }}</span>
                      <span v-if="article.updateTime">更新时间: {{ article.updateTime }}</span>
                    </div>
                  </div>
                </div>
                
                <div class="article-status">
                  <span :class="['status-tag', {
                    'status-pending': article.status === 2,
                    'status-published': article.status === 3,
                    'status-rejected': article.status === 0
                  }]">
                    {{ article.status === 2 ? '待审核' : article.status === 3 ? '已发布' : article.status === 0 ? '已拒绝' : '未知状态' }}
                  </span>
                </div>
                
                <div class="article-actions">
                  <button 
                    v-if="article.status === 2" 
                    @click="reviewArticle(article.id, 3)" 
                    class="action-btn approve-btn"
                  >
                    <i class="fas fa-check"></i> 通过
                  </button>
                  <button 
                    v-if="article.status === 2" 
                    @click="reviewArticle(article.id, 0)" 
                    class="action-btn reject-btn"
                  >
                    <i class="fas fa-times"></i> 拒绝
                  </button>
                  <button 
                    v-if="article.status === 3" 
                    @click="reviewArticle(article.id, 0)" 
                    class="action-btn reject-btn"
                  >
                    <i class="fas fa-times"></i> 下架
                  </button>
                  <button 
                    v-if="article.status === 0" 
                    @click="reviewArticle(article.id, 3)" 
                    class="action-btn approve-btn"
                  >
                    <i class="fas fa-check"></i> 重新发布
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 分页组件 -->
          <div v-if="totalArticles > 0" class="pagination">
            <button 
              @click="changePage(1)" 
              :disabled="currentPage === 1"
              class="page-btn"
            >
              <i class="fas fa-chevron-left"></i>
            </button>
            <button 
              v-for="page in Math.ceil(totalArticles / pageSize)" 
              :key="page"
              @click="changePage(page)"
              :class="['page-btn', { active: currentPage === page }]"
            >
              {{ page }}
            </button>
            <button 
              @click="changePage(Math.ceil(totalArticles / pageSize))" 
              :disabled="currentPage === Math.ceil(totalArticles / pageSize)"
              class="page-btn"
            >
              <i class="fas fa-chevron-right"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import '../assets/main.css'

export default {
  name: 'ModeratorView',
  data() {
    return {
      isLoggedIn: false,
      userAvatar: '',
      currentUserId: null,
      unreadMessageCount: null,
      dropdownVisible: false,
      // 当前选中的模块
      activeModule: 'overview', // overview, articles, comments, users, reports
      // 文章管理数据
      articles: [],
      totalArticles: 0,
      pendingArticlesCount: 0, // 待审核文章数量
      currentPage: 1,
      pageSize: 10,
      // 搜索和筛选条件
      searchKeyword: '',
      statusFilter: null, // 2: 待审核, 3: 已发布, 0: 已拒绝
      // 加载状态
      loading: false
    }
  },
  mounted() {
    // 检查登录状态
    this.checkLoginStatus()
    // 获取用户头像和未读消息数量
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
    // 切换下拉菜单显示状态
    toggleDropdown() {
      this.dropdownVisible = !this.dropdownVisible
    },
    // 检查登录状态
    checkLoginStatus() {
      this.isLoggedIn = !!localStorage.getItem('isLoggedIn') || !!sessionStorage.getItem('isLoggedIn')
      console.log('登录状态:', this.isLoggedIn ? '已登录' : '未登录')
      
      // 如果未登录，重定向到登录页面
      if (!this.isLoggedIn) {
        alert('请先登录后再进入风纪委员管理界面')
        this.$router.push('/login')
      }
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
    // 处理搜索按钮点击
    handleSearchClick() {
      // 在新标签页中打开搜索页面
      window.open('/search', '_blank')
    },
    // 处理写文章按钮点击
    handleCreateClick() {
      // 在新页面打开创作中心
      window.open('/articles/create', '_blank')
    },
    // 处理私信按钮点击
    handleMessageClick() {
      window.open('/messages', '_blank')
    },
    // 处理通知按钮点击事件
    handleNotificationClick() {
      // 由于通知页面已移除，在新标签页打开私信页面
      window.open('/messages', '_blank')
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
      
      // 跳转到首页
      this.$router.push('/community')
    },
    // 返回今日概况首页
    handleOverview() {
      this.activeModule = 'overview'
    },
    
    // 文章管理
    handleArticleManagement() {
      this.activeModule = 'articles'
      this.fetchArticles()
    },
    
    // 获取文章数据
    async fetchArticles() {
      this.loading = true
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {}
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
        }
        
        // 调用接口获取文章数据（后端不需要参数）
        const response = await axios.get('http://localhost:8081/api/admin/articles/review-and-publish', {
          headers
        })
        
        // 调整响应数据处理，根据后端PageInfo结构
        const pageInfo = response.data.data || { list: [], total: 0 }
        let allArticles = pageInfo.list || []
        
        // 计算待审核文章数量（status=2）
        this.pendingArticlesCount = allArticles.filter(article => article.status === 2).length
        
        // 前端实现搜索和过滤
        if (this.searchKeyword || this.statusFilter !== null) {
          allArticles = allArticles.filter(article => {
            // 搜索关键词过滤
            const matchesSearch = !this.searchKeyword || 
              article.title.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
              article.authorId.toString().includes(this.searchKeyword)
            
            // 状态过滤
            const matchesStatus = this.statusFilter === null || article.status === this.statusFilter
            
            return matchesSearch && matchesStatus
          })
        }
        
        // 更新总文章数
        this.totalArticles = allArticles.length
        
        // 前端实现分页
        const startIndex = (this.currentPage - 1) * this.pageSize
        const endIndex = startIndex + this.pageSize
        this.articles = allArticles.slice(startIndex, endIndex)
      } catch (error) {
        console.error('获取文章数据失败:', error)
        // 如果接口调用失败，使用模拟数据
        let allArticles = this.getMockArticles()
        
        // 前端实现搜索和过滤
        if (this.searchKeyword || this.statusFilter !== null) {
          allArticles = allArticles.filter(article => {
            // 搜索关键词过滤
            const matchesSearch = !this.searchKeyword || 
              article.title.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
              article.authorId.toString().includes(this.searchKeyword)
            
            // 状态过滤
            const matchesStatus = this.statusFilter === null || article.status === this.statusFilter
            
            return matchesSearch && matchesStatus
          })
        }
        
        // 更新总文章数
        this.totalArticles = allArticles.length
        
        // 前端实现分页
        const startIndex = (this.currentPage - 1) * this.pageSize
        const endIndex = startIndex + this.pageSize
        this.articles = allArticles.slice(startIndex, endIndex)
      } finally {
        this.loading = false
      }
    },
    
    // 模拟文章数据
    getMockArticles() {
      return [
        {
          id: 1,
          title: '时间的镜像：《精进的美事》与迟来的理解',
          authorId: 1001,
          categoryId: 1,
          pageview: 1234,
          status: 2,
          createTime: '2025-07-23 15:45:30',
          updateTime: '2025-07-23 15:45:30',
          coverImageUrl: 'https://picsum.photos/id/1/200/120',
          loveCount: 45,
          viewCount: 1234
        },
        {
          id: 2,
          title: '无限的乌托邦为何现在残害着少女',
          authorId: 1002,
          categoryId: 2,
          pageview: 567,
          status: 2,
          createTime: '2025-07-23 14:30:15',
          updateTime: '2025-07-23 14:30:15',
          coverImageUrl: 'https://picsum.photos/id/2/200/120',
          loveCount: 28,
          viewCount: 567
        },
        {
          id: 3,
          title: '前端性能优化技巧总结',
          authorId: 1003,
          categoryId: 3,
          pageview: 2345,
          status: 3,
          createTime: '2025-07-22 10:20:30',
          updateTime: '2025-07-22 10:20:30',
          coverImageUrl: 'https://picsum.photos/id/3/200/120',
          loveCount: 89,
          viewCount: 2345
        },
        {
          id: 4,
          title: 'Vue 3 组合式 API 最佳实践',
          authorId: 1004,
          categoryId: 3,
          pageview: 1890,
          status: 3,
          createTime: '2025-07-21 16:45:00',
          updateTime: '2025-07-21 16:45:00',
          coverImageUrl: 'https://picsum.photos/id/4/200/120',
          loveCount: 67,
          viewCount: 1890
        },
        {
          id: 5,
          title: 'JavaScript 异步编程深入理解',
          authorId: 1005,
          categoryId: 3,
          pageview: 3456,
          status: 4,
          createTime: '2025-07-20 09:15:45',
          updateTime: '2025-07-20 09:15:45',
          coverImageUrl: 'https://picsum.photos/id/5/200/120',
          loveCount: 123,
          viewCount: 3456
        }
      ]
    },
    
    // 审核文章
    async reviewArticle(articleId, status) {
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        const headers = {}
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
          headers['Authorization'] = `Bearer ${cleanToken}`
        }
        
        // 调用审核接口 - 使用正确的路径和请求参数格式
        await axios.put(
          `http://localhost:8081/api/admin/articles/${articleId}`,
          {},
          { 
            headers, 
            params: { status } // 使用query参数传递status
          }
        )
        
        // 重新获取文章列表
        this.fetchArticles()
        alert('审核操作成功')
      } catch (error) {
        console.error('审核文章失败:', error)
        alert('审核操作失败')
      }
    },
    
    // 搜索文章
    searchArticles() {
      this.currentPage = 1
      this.fetchArticles()
    },
    
    // 切换页面
    changePage(page) {
      this.currentPage = page
      this.fetchArticles()
    },
    // 评论管理
    handleCommentManagement() {
      alert('评论管理功能开发中')
    },
    // 用户管理
    handleUserManagement() {
      alert('用户管理功能开发中')
    },
    // 举报处理
    handleReportManagement() {
      alert('举报处理功能开发中')
    }
  }
}
</script>

<style scoped>
/* 风纪委员管理界面样式 */
.moderator-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* 头部样式 */
.community-header {
  /* 优化：使用与首页一致的背景色 */
  background-color: #1E1E2E;
  border-bottom: 1px solid #9D8CF0;
  /* 优化：添加与首页一致的底部阴影 */
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

/* 导航菜单样式 */
.main-nav {
  flex: 1;
  margin: 0 var(--spacing-lg);
}

.nav-list {
  display: flex;
  gap: var(--spacing-md);
  list-style: none;
  justify-content: flex-start;
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
  /* 优化：使用与首页一致的未选中文字色 */
  color: #F8FAFC;
  font-size: 16px;
  font-weight: 500;
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius);
  transition: all var(--transition-normal);
}

.nav-link:hover {
  color: var(--accent-primary);
}

.nav-item.active .nav-link {
  color: var(--accent-primary);
  font-weight: 600;
}

/* 右侧功能按钮区 */
.function-buttons {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.search-container {
  flex-shrink: 0;
}

.search-btn {
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
}

.search-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* 写文章按钮 */
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

.icon-buttons {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  transition: all var(--transition-normal);
}

.icon-btn:hover {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  border-color: var(--accent-primary);
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: var(--accent-error);
  color: white;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

/* 私信按钮 */
.message-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: var(--accent-primary);
  color: white;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
}

/* 用户下拉菜单 */
.user-dropdown {
  position: relative;
}

.user-center-btn {
  /* 继承icon-btn的样式 */
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

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  min-width: 150px;
  margin-top: var(--spacing-xs);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  width: 100%;
  background: none;
  border: none;
  text-align: left;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.dropdown-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
}

.item-icon {
  font-size: 16px;
}

/* 主内容区样式 */
.moderator-content {
  flex: 1;
  display: flex;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: var(--spacing-lg);
  gap: var(--spacing-xl);
}

/* 左侧侧边栏 */
.moderator-sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
  background-color: var(--bg-primary);
  border-right: 1px solid var(--border-color);
  padding: var(--spacing-lg);
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .moderator-content {
    flex-direction: column;
  }
  
  .moderator-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
    padding-bottom: var(--spacing-xl);
  }
  
  .content-main {
    max-width: 100% !important;
  }
}

/* 侧边栏卡片 */
.sidebar-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

/* 总体数据卡片 */
.overall-stats .card-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}

.overall-stats .card-icon {
  font-size: 24px;
  color: var(--accent-primary);
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-primary);
  margin-bottom: var(--spacing-xs);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}

/* 模块管理卡片 */
.module-management .card-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}

.module-management .card-icon {
  font-size: 24px;
  color: var(--accent-primary);
}

.module-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.module-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg);
  border-radius: var(--border-radius);
  background-color: var(--bg-tertiary);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.module-item:hover {
  background-color: rgba(var(--accent-primary-rgb), 0.05);
  border-color: var(--accent-primary);
  transform: translateX(4px);
}

.module-icon {
  font-size: 24px;
  color: var(--accent-primary);
  width: 32px;
  text-align: center;
}

.module-info {
  flex: 1;
}

.module-name {
  display: block;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
}

.module-count {
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
  color: var(--accent-primary);
  background-color: rgba(var(--accent-primary-rgb), 0.1);
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid rgba(var(--accent-primary-rgb), 0.2);
}

/* 统计趋势卡片 */
.stats-trend .card-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}

.stats-trend .card-icon {
  font-size: 24px;
  color: var(--accent-primary);
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.trend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) 0;
  border-bottom: 1px solid var(--border-color);
}

.trend-item:last-child {
  border-bottom: none;
}

.trend-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.trend-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--accent-primary);
}

/* 右侧主内容区 */
.content-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
  max-width: calc(100% - 340px); /* 减去侧边栏宽度和间距 */
  min-width: 0; /* 允许内容区收缩 */
}

/* 页面标题 */
.page-header {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  text-align: center;
  padding: var(--spacing-xl);
  background-color: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.page-title {
  margin: 0 0 var(--spacing-sm);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
}

.title-icon {
  font-size: 36px;
}

.page-description {
  margin: 0;
  font-size: 16px;
  color: var(--text-secondary);
}

/* 今日概况 */
.today-overview {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xxl);
  box-shadow: var(--shadow-sm);
}

.section-title {
  margin: 0 0 var(--spacing-xl);
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.section-title::before {
  content: '';
  width: 4px;
  height: 24px;
  background-color: var(--accent-primary);
  border-radius: 2px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--spacing-lg);
  max-width: 100%;
}

.overview-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius-large);
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
  text-align: center;
}

.overview-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--accent-primary);
}

.overview-icon {
  font-size: 36px;
  color: var(--accent-primary);
  margin-bottom: var(--spacing-md);
  padding: var(--spacing-md);
  background-color: rgba(var(--accent-primary-rgb), 0.1);
  border-radius: 50%;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  width: 100%;
}

.overview-title {
  font-size: 16px;
  color: var(--text-secondary);
  margin: 0;
  font-weight: 500;
}

.overview-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--accent-primary);
  margin: 0;
}

.overview-change {
  font-size: 14px;
  color: var(--accent-success);
  font-weight: 500;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
}

/* 最近待处理事项 */
.recent-tasks {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  max-width: 100%;
}

.task-item {
  min-width: 0;
}

.task-item {
  display: flex;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border-radius: var(--border-radius);
  background-color: var(--bg-tertiary);
}

.task-icon {
  font-size: 24px;
  color: var(--accent-primary);
  flex-shrink: 0;
}

.task-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.task-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
}

.task-description {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-xs);
}

.task-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.task-status {
  font-size: 12px;
  font-weight: 500;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius);
}

.task-status.pending {
  background-color: var(--accent-warning);
  color: var(--bg-primary);
}

/* 图标颜色 */
.article-icon {
  color: var(--accent-primary);
}

.comment-icon {
  color: var(--accent-secondary);
}

.user-icon {
  color: var(--accent-info);
}

.report-icon {
  color: var(--accent-error);
}

/* 文章管理样式 */
.articles-management {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xl);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.articles-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
}

.articles-filter {
  display: flex;
  gap: var(--spacing-lg);
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-box input {
  padding: var(--spacing-sm) var(--spacing-md) var(--spacing-sm) 40px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  min-width: 200px;
}

.search-box .search-btn {
  position: absolute;
  left: 10px;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-filter select {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background-color: var(--bg-primary);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
}

.articles-list-container {
  background-color: var(--bg-primary);
  border-radius: var(--border-radius);
  padding: var(--spacing-lg);
}

.loading-indicator {
  text-align: center;
  padding: var(--spacing-xl);
  color: var(--text-secondary);
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: var(--spacing-xxl);
  color: var(--text-secondary);
}

.empty-state i {
  font-size: 48px;
  margin-bottom: var(--spacing-lg);
  color: var(--text-tertiary);
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.article-item {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background-color: var(--bg-secondary);
  transition: all var(--transition-normal);
}

.article-item:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--accent-primary);
}

.article-info {
  display: flex;
  gap: var(--spacing-md);
  flex: 1;
  min-width: 0;
}

.article-cover {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 16px;
  background-color: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  transition: transform var(--transition-normal);
}

.article-item:hover .article-cover img {
  transform: scale(1.05);
}

.article-details {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.article-title {
  font-size: 16px;
  font-weight: bold;
  color: #C8B8F0;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  font-size: 14px;
  color: #808090;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.article-time {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: var(--spacing-xs);
}

.article-status {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.status-pending {
  background-color: var(--accent-warning);
  color: white;
}

.status-published {
  background-color: var(--accent-success);
  color: white;
}

.status-rejected {
  background-color: var(--accent-error);
  color: white;
}

.article-actions {
  display: flex;
  gap: var(--spacing-sm);
  flex-shrink: 0;
  align-items: center;
}

.action-btn {
  padding: var(--spacing-xs) var(--spacing-sm);
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  transition: all var(--transition-normal);
}

.approve-btn {
  background-color: var(--accent-success);
  color: white;
}

.approve-btn:hover {
  background-color: rgba(var(--accent-success-rgb), 0.9);
}

.reject-btn {
  background-color: var(--accent-error);
  color: white;
}

.reject-btn:hover {
  background-color: rgba(var(--accent-error-rgb), 0.9);
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  gap: var(--spacing-sm);
  align-items: center;
  flex-wrap: wrap;
}

.page-btn {
  padding: var(--spacing-xs) var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background-color: var(--bg-primary);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-normal);
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  background-color: var(--bg-tertiary);
  border-color: var(--accent-primary);
}

.page-btn.active {
  background-color: var(--accent-primary);
  color: white;
  border-color: var(--accent-primary);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: var(--spacing-lg);
  }
  
  .main-nav {
    margin: var(--spacing-md) 0;
  }
  
  .nav-list {
    gap: var(--spacing-md);
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .moderator-cards {
    grid-template-columns: 1fr;
  }
  
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .articles-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .articles-filter {
    justify-content: stretch;
  }
  
  .article-item {
    flex-direction: column;
  }
  
  .article-info {
    flex-direction: column;
  }
  
  .article-cover {
    width: 100%;
    height: 120px;
  }
  
  .article-actions {
    justify-content: stretch;
  }
  
  .action-btn {
    flex: 1;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 24px;
  }
  
  .title-icon {
    font-size: 28px;
  }
  
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
  </style>