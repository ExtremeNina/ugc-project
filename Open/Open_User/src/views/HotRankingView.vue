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
            <li class="nav-item">
              <a href="/community" class="nav-link">
                <span class="nav-text">首页</span>
              </a>
            </li>
            <li class="nav-item active">
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
          <a href="/articles/create" class="create-btn" @click.prevent="handleCreateClick">
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
              <button class="icon-btn user-center-btn" @click="toggleDropdown">
                <span v-if="!isLoggedIn" class="user-login-text">登录</span>
                <img v-else :src="userAvatar || 'https://picsum.photos/id/64/40/40'" alt="用户头像" class="user-avatar">
              </button>
              <div v-if="showDropdown" class="dropdown-menu">
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

    <!-- 主要内容区 -->
    <main class="community-content">
      <div class="content-main" style="max-width: 1200px; margin: 0 auto; padding: 0 var(--spacing-lg);">
        <div style="max-width: 50%;">
        <!-- 热榜文章列表 -->
        <div class="hot-ranking-container">
          <div class="ranking-header">
            <h2 class="ranking-title">热门文章</h2>
            <div class="ranking-time">
              <span>更新于 {{ currentTime }}</span>
            </div>
          </div>
          <div class="hot-article-list">
            <div
              v-for="(article, index) in hotArticles"
              :key="article.id"
              class="hot-article-item"
            >
              <div class="hot-article-number">{{ index + 1 }}</div>
              <div class="hot-article-content">
                <h3 class="hot-article-title">{{ article.title }}</h3>
                <div class="hot-article-meta">
                  <span>作者: {{ article.author || '匿名' }}</span>
                  <span>•</span>
                  <span>{{ article.loveCount || 0 }} 点赞</span>
                  <span>•</span>
                  <span>{{ article.viewCount || 0 }} 浏览</span>
                  <span>•</span>
                  <span>{{ formatDate(article.createdAt) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    </main>


  </div>
</template>

<script>
import axios from 'axios'
export default {
  name: 'HotRankingView',
  data() {
    return {
      // 用户是否登录
      isLoggedIn: false,
      // 用户头像
      userAvatar: '',
      // 当前用户ID
      currentUserId: null,
      // 未读消息数量
      unreadMessageCount: null,
      // 热榜文章列表
      hotArticles: [],
      // 当前时间
      currentTime: '',
      // 下拉菜单显示状态
      showDropdown: false
    };
  },
  created() {
    // 初始化页面
    this.initPage();
    // 定时更新当前时间
    this.updateCurrentTime();
    setInterval(() => {
      this.updateCurrentTime();
    }, 60000);
  },
  methods: {
    // 初始化页面
    initPage() {
      this.checkLoginStatus();
      this.loadHotArticles();
      // 如果已登录，获取用户头像和未读消息数量
      if (this.isLoggedIn) {
        this.getUserAvatar();
        this.getCurrentUserId();
        // 获取用户ID后再获取未读消息数量
        this.$nextTick(() => {
          if (this.currentUserId) {
            this.getUnreadMessageCount();
          }
        });
      }
    },
    // 检查登录状态
    checkLoginStatus() {
      this.isLoggedIn = !!localStorage.getItem('isLoggedIn') || !!sessionStorage.getItem('isLoggedIn');
      console.log('登录状态:', this.isLoggedIn ? '已登录' : '未登录');
    },
    // 获取当前用户ID
    getCurrentUserId() {
      try {
        // 从localStorage或sessionStorage获取用户信息
        const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          if (userInfo.id || userInfo.userId) {
            this.currentUserId = userInfo.id || userInfo.userId;
            console.log('当前用户ID:', this.currentUserId);
          }
        }
      } catch (error) {
        console.error('获取用户ID失败:', error);
      }
    },
    // 获取用户头像
    async getUserAvatar() {
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = {};
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`);
        }
        
        // 使用新的API端点获取头像路径
        const response = await axios.get('http://localhost:8081/api/community/user/avatar', { headers });
        console.log('获取用户头像API响应数据:', response.data);
        
        // 处理后端返回的Map集合
        if (response.data && response.data.data) {
          const userData = response.data.data;
          console.log('用户数据Map:', userData);
          
          // 设置头像路径
          if (userData.icon) {
            this.userAvatar = userData.icon;
            console.log('设置头像路径:', userData.icon);
          }
          
          // 如果还没有用户ID，尝试从用户数据中获取
          if (!this.currentUserId && (userData.id || userData.userId)) {
            this.currentUserId = userData.id || userData.userId;
            console.log('从用户数据中获取用户ID:', this.currentUserId);
            // 获取到用户ID后，获取未读消息数量
            this.getUnreadMessageCount();
          }
        }
      } catch (error) {
        console.error('获取用户头像失败:', error);
        // 保持默认头像
      }
    },
    // 获取未读消息数量
    async getUnreadMessageCount() {
      if (!this.currentUserId) {
        console.error('无法获取未读消息数量：用户ID为空');
        return;
      }
      
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = {};
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 调用未读消息数量接口
        const response = await axios.get('http://localhost:8081/api/community/unreadMessage', {
          headers,
          params: {
            userId: this.currentUserId
          }
        });
        
        console.log('获取未读消息数量API响应:', response.data);
        
        // 处理返回结果
        if (response.data && response.data.data !== undefined) {
          this.unreadMessageCount = response.data.data;
        } else {
          this.unreadMessageCount = response.data;
        }
        
        console.log('未读消息数量:', this.unreadMessageCount);
      } catch (error) {
        console.error('获取未读消息数量失败:', error);
        this.unreadMessageCount = null;
      }
    },
    // 加载热榜文章
    loadHotArticles() {
      // 模拟热榜文章数据
      this.hotArticles = [
        {
          id: 1,
          title: 'Vue 3 组合式 API 深度解析与最佳实践',
          author: '技术达人',
          loveCount: 156,
          viewCount: 2345,
          createdAt: Date.now() - 3600000
        },
        {
          id: 2,
          title: 'React 18 新特性与并发渲染详解',
          author: '前端开发者',
          loveCount: 128,
          viewCount: 1890,
          createdAt: Date.now() - 7200000
        },
        {
          id: 3,
          title: 'TypeScript 5.0 新功能全览',
          author: 'TS爱好者',
          loveCount: 98,
          viewCount: 1567,
          createdAt: Date.now() - 10800000
        },
        {
          id: 4,
          title: 'Node.js 18 性能优化技巧',
          author: '后端工程师',
          loveCount: 87,
          viewCount: 1234,
          createdAt: Date.now() - 14400000
        },
        {
          id: 5,
          title: 'CSS Grid 与 Flexbox 结合使用的高级布局技巧',
          author: '前端设计师',
          loveCount: 76,
          viewCount: 1123,
          createdAt: Date.now() - 18000000
        },
        {
          id: 6,
          title: 'Docker 容器化部署最佳实践',
          author: 'DevOps专家',
          loveCount: 65,
          viewCount: 987,
          createdAt: Date.now() - 21600000
        },
        {
          id: 7,
          title: 'GraphQL API 设计与实现',
          author: '全栈开发者',
          loveCount: 54,
          viewCount: 876,
          createdAt: Date.now() - 25200000
        },
        {
          id: 8,
          title: 'WebAssembly 在前端的应用场景',
          author: '技术探索者',
          loveCount: 43,
          viewCount: 765,
          createdAt: Date.now() - 28800000
        },
        {
          id: 9,
          title: '微前端架构设计与实践',
          author: '架构师',
          loveCount: 32,
          viewCount: 654,
          createdAt: Date.now() - 32400000
        },
        {
          id: 10,
          title: 'PWA 应用开发与部署',
          author: '移动开发者',
          loveCount: 21,
          viewCount: 543,
          createdAt: Date.now() - 36000000
        }
      ];
    },
    // 更新当前时间
    updateCurrentTime() {
      const now = new Date();
      this.currentTime = now.toLocaleTimeString();
    },
    // 格式化日期
    formatDate(date) {
      const now = new Date();
      const target = new Date(date);
      const diff = now - target;
      
      const minutes = Math.floor(diff / 60000);
      const hours = Math.floor(diff / 3600000);
      const days = Math.floor(diff / 86400000);
      
      if (minutes < 60) {
        return `${minutes}分钟前`;
      } else if (hours < 24) {
        return `${hours}小时前`;
      } else {
        return `${days}天前`;
      }
    },
    // 处理搜索按钮点击
    handleSearchClick() {
      this.$router.push('/search');
    },
    // 处理创作中心按钮点击
    handleCreateClick() {
      // 检查是否登录
      if (this.isLoggedIn) {
        this.$router.push('/articles/create');
      } else {
        this.$router.push('/login');
      }
    },
    // 处理风纪委员点击
    handleModeratorClick() {
      if (this.isLoggedIn) {
        this.$router.push('/moderator');
      } else {
        this.$router.push('/login');
      }
    },
    // 处理通知点击
    handleNotificationClick() {
      window.open('/messages', '_blank');
    },
    // 处理个人资料点击
    handleProfileClick() {
      if (this.isLoggedIn) {
        this.$router.push('/profile');
      } else {
        this.$router.push('/login');
      }
    },
    // 处理退出登录
    handleLogout() {
      // 清除本地存储
      localStorage.removeItem('isLoggedIn');
      localStorage.removeItem('userInfo');
      localStorage.removeItem('token');
      sessionStorage.removeItem('isLoggedIn');
      sessionStorage.removeItem('userInfo');
      sessionStorage.removeItem('token');
      this.isLoggedIn = false;
      this.userAvatar = '';
      this.showDropdown = false;
      // 重定向到登录页
      this.$router.push('/login');
    },
    // 切换下拉菜单显示/隐藏
    toggleDropdown() {
      this.showDropdown = !this.showDropdown;
    }
  }
};
</script>

<style scoped>
/* 热榜容器样式 */
.hot-ranking-container {
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.ranking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.ranking-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.ranking-time {
  font-size: 14px;
  color: var(--text-tertiary);
}

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

/* 功能按钮区 */
.function-buttons {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-shrink: 0;
}

/* 搜索按钮 */
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
  outline: none;
  text-decoration: none;
}

.search-btn:hover {
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
  background-color: var(--bg-secondary);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

/* 通知按钮 */
.notification-btn {
  position: relative;
}

.notification-icon {
  font-size: 18px;
}

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

/* 个人中心下拉菜单 */
.user-dropdown {
  position: relative;
  z-index: 1001;
}

.user-center-btn {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 顶部导航栏用户头像样式 */
.user-dropdown .user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  transition: transform var(--transition-normal);
}

.user-dropdown .user-avatar:hover {
  transform: scale(1.1);
}

/* 下拉菜单 */
.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  min-width: 160px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-sm) 0;
  z-index: 1001;
  margin-top: var(--spacing-xs);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-normal);
  text-align: left;
}

.dropdown-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
}

.item-icon {
  font-size: 16px;
}

.item-text {
  flex: 1;
}

/* 热榜文章列表 */
.hot-article-list {
  display: flex;
  flex-direction: column;
  gap: 21px;
}

.hot-article-item {
  /* 垂直布局 */
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md);
  /* 优化：使用指定的背景色 */
  background: #273449;
  /* 优化：使用指定的边框 */
  border: 1px solid rgba(184,146,255,0.1);
  border-radius: 12px;
  padding: 16px;
  /* 优化：使用指定的默认阴影 */
  box-shadow: 0 4px 12px rgba(184,146,255,0.1);
  /* 过渡动画 */
  transition: all 300ms ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.hot-article-item:hover {
  /* 增强的提升效果 */
  transform: translateY(-3px);
  /* 优化：使用指定的hover背景色 */
  background: #2C3A52;
  /* 优化：使用指定的hover阴影 */
  box-shadow: 0 6px 18px rgba(184,146,255,0.18);
  /* 优化：边框颜色变化 */
  border-color: rgba(184,146,255,0.2);
}

.hot-article-item:hover .hot-article-title {
  /* 优化：hover时使用主色 */
  color: #976EFF;
}

.hot-article-number {
  width: 24px;
  height: 24px;
  background-color: var(--accent-hot-rank);
  color: var(--bg-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 2px;
}

.hot-article-content {
  flex: 1;
}

.hot-article-title {
  font-size: 19px;
  font-weight: bold;
  /* 优化：使用与首页一致的主标题颜色 */
  color: #C8B8F0;
  margin-bottom: 6px;
  line-height: 1.4;
  transition: color var(--transition-normal);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-article-meta {
  font-size: 14px;
  color: var(--text-tertiary);
}

.hot-article-meta span {
  margin-right: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hot-article-item {
    padding: 10px 6px;
  }
  
  .hot-article-title {
    font-size: 12px;
  }
  
  .hot-article-meta {
    font-size: 10px;
  }
}
</style>