<template>
  <div class="dynamic-container">
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
            <li class="nav-item active">
              <a href="/dynamic" class="nav-link">
                <span class="nav-text">动态</span>
              </a>
            </li>
            <li class="nav-item">
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
          
          <!-- 个人按钮和消息按钮 -->
          <div class="icon-buttons">
            <!-- 通知按钮 -->
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
    
    <!-- 动态页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <i class="fas fa-users title-icon"></i>
        关注动态
      </h1>
      <p class="page-description">查看你关注的用户最新动态</p>
    </div>
    
    <div class="dynamic-content">
      
      <!-- 左侧侧边栏 -->
      <div class="dynamic-sidebar">
        <!-- 筛选选项 -->
        <div class="sidebar-card filter-options">
          <div class="card-title">
            <i class="fas fa-filter card-icon"></i>
            筛选
          </div>
          <div class="filter-list">
            <button class="filter-item" :class="{ 'active': activeFilter === 'all' }" @click="activeFilter = 'all'">
              <i class="fas fa-allergies filter-icon"></i>
              <span class="filter-name">全部动态</span>
            </button>
            <button class="filter-item" :class="{ 'active': activeFilter === 'articles' }" @click="activeFilter = 'articles'">
              <i class="fas fa-file-alt filter-icon"></i>
              <span class="filter-name">文章</span>
            </button>
            <button class="filter-item" :class="{ 'active': activeFilter === 'comments' }" @click="activeFilter = 'comments'">
              <i class="fas fa-comment filter-icon"></i>
              <span class="filter-name">评论</span>
            </button>
            <button class="filter-item" :class="{ 'active': activeFilter === 'likes' }" @click="activeFilter = 'likes'">
              <i class="fas fa-heart filter-icon"></i>
              <span class="filter-name">点赞</span>
            </button>
          </div>
        </div>
        
        <!-- 关注用户列表 -->
        <div class="sidebar-card following-users">
          <div class="card-title">
            <i class="fas fa-users card-icon"></i>
            关注的用户
          </div>
          <div class="user-list">
            <div class="user-item" v-for="user in followingUsers" :key="user.id">
              <img :src="user.avatar" alt="用户头像" class="user-avatar-small">
              <span class="user-name">{{ user.username }}</span>
            </div>
          </div>
        </div>
        
        <!-- 统计信息 -->
        <div class="sidebar-card activity-stats">
          <div class="card-title">
            <i class="fas fa-chart-line card-icon"></i>
            动态统计
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ totalDynamics }}</div>
              <div class="stat-label">总动态</div>
            </div>
            <div class="stat-item">
              <div class="stat-value">{{ todayDynamics }}</div>
              <div class="stat-label">今日动态</div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 右侧内容区 -->
      <div class="content-main">
        <!-- 动态列表 -->
        <div class="dynamic-list">
          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <div class="loading-text">加载中...</div>
          </div>
          
          <div v-else-if="dynamics.length === 0" class="empty-container">
            <div class="empty-icon">📭</div>
            <div class="empty-text">暂无动态</div>
          </div>
          
          <div v-else class="dynamic-items">
            <div class="dynamic-item" v-for="dynamic in filteredDynamics" :key="dynamic.id">
              <div class="result-card" @click="goToArticleDetail(dynamic.content.id)">
                <!-- 左侧头像区域 -->
                <div class="result-avatar">
                  <img :src="dynamic.user.avatar" alt="用户头像" class="user-avatar-large">
                </div>
                
                <!-- 中间内容区域 -->
                <div class="result-content">
                  <!-- 作者信息和发布动作 -->
                  <div class="author-info">
                    <span class="author-name">{{ dynamic.user.username }}</span>
                    <span class="post-action">发布了新文章</span>
                  </div>
                  
                  <!-- 文章标题 -->
                  <h3 class="result-title">{{ dynamic.content.title }}</h3>
                  
                  <!-- 元数据信息 -->
                  <div class="meta-info">
                    <span class="result-date">{{ formatTime(dynamic.time) }}</span>
                    <span class="meta-divider">・</span>
                    <span class="result-category">文章</span>
                    <span class="meta-divider">・</span>
                    <button class="like-btn" @click.stop="toggleLike(dynamic)" :class="{ 'liked': dynamic.isLove }">
                      <i class="fas fa-heart"></i>
                      <span class="like-count">{{ dynamic.content.likes }}</span>
                    </button>
                    <span class="view-count">
                      <i class="fas fa-eye"></i>
                      {{ dynamic.content.views }}
                    </span>
                  </div>
                </div>
                
                <!-- 右侧封面图片区域 -->
                <div class="result-cover">
                  <img v-if="dynamic.coverImageUrl" :src="dynamic.coverImageUrl" alt="文章封面">
                  <div v-else class="no-cover">
                    <span class="no-cover-icon">📝</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 分页组件 -->
        <div class="pagination">
          <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">上一页</button>
          <span class="page-info">第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="currentPage++">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
export default {
  name: 'DynamicView',
  data() {
    return {
      // 登录状态
      isLoggedIn: false,
      userAvatar: '',
      currentUserId: null,
      
      // 下拉菜单
      dropdownVisible: false,
      
      // 筛选选项
      activeFilter: 'all',
      
      // 分页
      currentPage: 1,
      totalPages: 1,
      
      // 关注的用户
      followingUsers: [
        { id: 1, username: '技术达人', avatar: 'https://picsum.photos/id/1012/40/40' },
        { id: 2, username: '前端专家', avatar: 'https://picsum.photos/id/1027/40/40' },
        { id: 3, username: '后端工程师', avatar: 'https://picsum.photos/id/1005/40/40' },
        { id: 4, username: '算法大师', avatar: 'https://picsum.photos/id/1000/40/40' }
      ],
      
      // 动态数据
      dynamics: [],
      
      // 加载状态
      loading: false,
      
      // 未读消息数量
      unreadMessageCount: null
    }
  },
  computed: {
    filteredDynamics() {
      if (this.activeFilter === 'all') {
        return this.dynamics;
      }
      return this.dynamics.filter(dynamic => dynamic.type === this.activeFilter);
    },
    totalDynamics() {
      return this.dynamics.length;
    },
    todayDynamics() {
      const today = new Date().toISOString().split('T')[0];
      return this.dynamics.filter(dynamic => {
        const dynamicDate = new Date(dynamic.time).toISOString().split('T')[0];
        return dynamicDate === today;
      }).length;
    }
  },
  methods: {
    // 格式化时间
    formatTime(time) {
      const now = new Date();
      const dynamicTime = new Date(time);
      const diff = now - dynamicTime;
      
      const minutes = Math.floor(diff / (1000 * 60));
      const hours = Math.floor(diff / (1000 * 60 * 60));
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      
      if (minutes < 60) {
        return `${minutes}分钟前`;
      } else if (hours < 24) {
        return `${hours}小时前`;
      } else {
        return `${days}天前`;
      }
    },
    
    // 处理搜索点击
    handleSearchClick() {
      // 跳转到搜索页面
      this.$router.push('/search');
    },
    
    // 处理写文章点击
    handleCreateClick() {
      // 在新页面打开创作中心
      window.open('/articles/create', '_blank');
    },
    
    // 处理私信点击
    handleMessageClick() {
      // 在新标签页打开私信页面
      window.open('/messages', '_blank');
    },
    
    // 处理通知点击
    handleNotificationClick() {
      // 由于通知页面已移除，在新标签页打开私信页面
      window.open('/messages', '_blank');
    },
    
    // 切换下拉菜单
    toggleDropdown() {
      this.dropdownVisible = !this.dropdownVisible;
    },
    
    // 处理个人资料点击
    handleProfileClick() {
      if (this.isLoggedIn) {
        // 跳转到个人资料页面
        // this.$router.push('/profile');
      } else {
        // 跳转到登录页面
        // this.$router.push('/login');
      }
      this.dropdownVisible = false;
    },
    
    // 处理退出登录
    handleLogout() {
      // 退出登录逻辑
      this.isLoggedIn = false;
      this.userAvatar = '';
      this.dropdownVisible = false;
      localStorage.removeItem('isLoggedIn');
      sessionStorage.removeItem('isLoggedIn');
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
    
    // 获取动态数据
    async fetchDynamics() {
      try {
        this.loading = true;
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = {};
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`);
        }
        
        // 调用动态API接口
        const response = await axios.get('http://localhost:8081/api/dynamic/newArticles', { headers });
        console.log('获取动态API响应数据:', response.data);
        
        // 处理响应数据，转换为组件需要的格式
        if (response.data && response.data.data) {
          const articles = response.data.data;
          // 将DyArticleVO转换为动态格式
          this.dynamics = articles.map(article => ({
            id: article.id,
            user: {
              id: 0, // API未返回用户ID，使用0占位
              username: article.author,
              avatar: article.userIcon || ''
            },
            type: 'article',
            time: article.publishTime,
            content: {
              id: article.id,
              title: article.title,
              preview: '', // API未返回预览内容，使用空字符串占位
              views: article.viewCount,
              comments: 0, // API未返回评论数，使用0占位
              likes: article.love
            },
            coverImageUrl: article.coverImageUrl,
            url: article.url,
            isLove: article.isLove
          }));
          
          // 设置总页数（假设每页10条数据）
          this.totalPages = Math.ceil(this.dynamics.length / 10);
        }
      } catch (error) {
        console.error('获取动态数据失败:', error);
      } finally {
        this.loading = false;
      }
    },
    
    // 调用查看文章详细接口
    async goToArticleDetail(articleId) {
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = {};
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 调用文章详情接口
        const response = await axios.get(`http://localhost:8081/api/articles/${articleId}`, { headers });
        console.log('获取文章详情响应:', response.data);
        
        // 如果接口调用成功，可以选择继续跳转到文章详情页面
        // 或者在当前页面显示文章详情
        window.location.href = `/article/${articleId}`;
      } catch (error) {
        console.error('获取文章详情失败:', error);
        alert('获取文章详情失败，请重试');
      }
    },
    
    // 处理点赞功能
    toggleLike(dynamic) {
      // 切换点赞状态
      dynamic.isLove = !dynamic.isLove;
      // 更新点赞数
      dynamic.content.likes += dynamic.isLove ? 1 : -1;
      
      // 这里可以添加实际的点赞API调用
      console.log(`文章 ${dynamic.id} 点赞状态: ${dynamic.isLove ? '已点赞' : '取消点赞'}`);
      
      // TODO: 调用点赞API接口
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = {};
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 调用点赞API
        // axios.post(`http://localhost:8081/api/articles/${dynamic.id}/like`, {}, { headers });
      } catch (error) {
        console.error('点赞操作失败:', error);
        // 失败时回滚状态
        dynamic.isLove = !dynamic.isLove;
        dynamic.content.likes += dynamic.isLove ? 1 : -1;
      }
    }
  },
  mounted() {
    // 设置网页标题
    document.title = '萌技术社区 - 动态';
    
    // 检查登录状态
    this.isLoggedIn = !!localStorage.getItem('isLoggedIn') || !!sessionStorage.getItem('isLoggedIn');
    console.log('登录状态:', this.isLoggedIn ? '已登录' : '未登录');
    
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
    
    // 获取动态数据
    this.fetchDynamics();
  }
}
</script>

<style scoped>
/* 动态页面容器 */
.dynamic-container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  display: flex;
  flex-direction: column;
}

/* 顶部导航区 */
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

/* 私信按钮 */
.message-btn {
  position: relative;
}

.message-icon {
  font-size: 18px;
}

.message-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background-color: var(--accent-primary);
  color: white;
  font-size: 12px;
  font-weight: bold;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

/* 个人中心下拉菜单 */
.user-dropdown {
  position: relative;
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

.user-login-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  z-index: 100;
  min-width: 140px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  width: 100%;
  text-align: left;
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.dropdown-item:hover {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
}

/* 主内容区样式 */
.dynamic-content {
  flex: 1;
  display: flex;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: var(--spacing-lg);
  gap: var(--spacing-xl);
}

/* 左侧侧边栏 */
.dynamic-sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
  background-color: var(--bg-primary);
  border-right: 1px solid var(--border-color);
  padding-right: var(--spacing-lg);
}

/* 侧边栏卡片 */
.sidebar-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

/* 卡片标题 */
.card-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.card-icon {
  font-size: 20px;
  color: var(--accent-primary);
}

/* 筛选列表 */
.filter-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background-color: transparent;
  border: none;
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  font-size: 16px;
  font-weight: 500;
}

.filter-item:hover {
  background-color: rgba(var(--accent-primary-rgb), 0.05);
  color: var(--accent-primary);
}

.filter-item.active {
  background-color: rgba(var(--accent-primary-rgb), 0.1);
  color: var(--accent-primary);
  border: 1px solid rgba(var(--accent-primary-rgb), 0.2);
}

.filter-icon {
  font-size: 18px;
  width: 20px;
  text-align: center;
}

/* 用户列表 */
.user-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.user-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-item:hover {
  background-color: rgba(var(--accent-primary-rgb), 0.05);
}

.user-avatar-small {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

/* 统计信息 */
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

/* 右侧内容区 */
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

/* 动态列表 */
.dynamic-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.dynamic-item {
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius);
  padding: var(--spacing-md);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
}

/* 动态头部 */
.dynamic-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  gap: var(--spacing-sm);
  padding-top: 10px;
}

.dynamic-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.dynamic-user-info {
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.user-name {
  font-size: 12px;
  font-weight: 400;
  color: #b3b3b3;
}

.meta-separator {
  color: #b3b3b3;
  font-size: 12px;
}

.dynamic-time {
  font-size: 12px;
  color: #b3b3b3;
}

/* 动态内容 */
.dynamic-content {
  margin-bottom: 0;
}

.dynamic-text {
  margin: 0 0 12px;
  line-height: 1.5;
  font-size: 13px;
}

.action-text {
  font-weight: 400;
  color: #b3b3b3;
  font-size: 14px;
}

.article-title {
  color: var(--accent-primary);
  text-decoration: none;
  font-weight: 700;
  font-size: 16px;
  display: block;
  margin-top: 8px;
  margin-bottom: 12px;
  line-height: 1.3;
  transition: color 0.2s ease;
}

.article-title:hover {
  text-decoration: underline;
}

/* 动态预览 */
.dynamic-preview {
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  padding: var(--spacing-md);
  margin-top: 0;
  width: 100%;
  box-sizing: border-box;
}

/* 文章封面 */
.article-cover {
  margin-bottom: var(--spacing-md);
  border-radius: var(--border-radius);
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: auto;
  display: block;
  object-fit: cover;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xxl);
  color: var(--text-secondary);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-md);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 16px;
  color: var(--text-secondary);
}

/* 空状态 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xxl);
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.empty-text {
  font-size: 16px;
  color: var(--text-secondary);
}

/* 点赞状态 */
.liked {
  color: #ff4757;
}

/* 动态项目容器 */
.dynamic-items {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

/* 动态卡片样式 */
.result-card {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 28px;
  background-color: var(--bg-secondary);
  border-radius: 10px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 0;
}

.result-card:hover {
  border-color: var(--accent-primary);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

/* 左侧头像区域 */
.result-avatar {
  flex-shrink: 0;
}

/* 放大的用户头像样式 */
.user-avatar-large {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

/* 作者信息容器 */
.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* 用户名样式 */
.author-name {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

/* 元数据信息容器 */
.meta-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  flex-wrap: wrap;
}

/* 点赞按钮样式 */
.meta-info .like-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}

/* 浏览数样式 */
.meta-info .view-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 发布动作文字样式 */
.post-action {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 中间内容区域 */
.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 右侧封面图 */
.result-cover {
  width: 200px;
  max-width: 240px;
  aspect-ratio: 4/3;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 8px;
  background-color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #444;
  color: #999;
}

.no-cover-icon {
  font-size: 48px;
  color: #999;
}

/* 右侧内容部分 */
.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 20px;
}

/* 头部区域样式 */
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

/* 标题样式 - 强化视觉重点 */
.result-title {
  font-size: 22px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  flex: 1;
}

/* 底部信息和操作区 */
.result-footer {
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

/* 分类标签样式 */
.result-category {
  background-color: #FF8C00;
  color: white;
  padding: 4px 8px;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

/* 日期样式 */
.result-date {
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  white-space: nowrap;
}

/* 作者信息区域样式 */
.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}

/* 用户头像样式 - 调大尺寸 */
.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

/* 作者名称样式 */
.result-author {
  color: #FFFFFF;
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}

/* 发布动作样式 */
.post-action {
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 元信息分隔符 */
.meta-divider {
  color: rgba(255, 255, 255, 0.5);
  font-size: 15px;
}

/* 作者信息右侧元数据 */
.meta-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #808090;
  font-size: 14px;
}

/* 交互按钮区域样式 - 放置在右上角 */
.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 0;
  flex-shrink: 0;
}

/* 点赞按钮样式 - 缩小图标 */
.like-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid var(--border-color);
  background-color: transparent;
  border-radius: 6px;
  font-size: 13px;
  color: #FFFFFF;
  cursor: pointer;
  transition: all 0.2s ease;
}

.like-btn:hover {
  border-color: #FF4757;
  color: #FF4757;
}

.like-btn.liked {
  background-color: #FF4757;
  border-color: #FF4757;
  color: #FFFFFF;
}

.like-btn.liked:hover {
  background-color: #FF3742;
  border-color: #FF3742;
}

/* 浏览量样式 - 缩小图标 */
.view-count {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  border-radius: 0;
  border: none;
  background-color: transparent;
}

/* 预览内容 */
.preview-content {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 300;
  color: #B0B0C0;
  line-height: 1.5;
  display: block;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 预览元信息 */
.preview-meta {
  display: flex;
  gap: 15px;
  font-size: 12px;
  color: #b3b3b3;
  justify-content: flex-start;
  flex-wrap: nowrap;
  width: 100%;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

/* 分页组件 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-xl);
  margin-top: var(--spacing-lg);
}

.page-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  border: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background-color: var(--bg-tertiary);
  border-color: var(--accent-primary);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .dynamic-content {
    flex-direction: column;
  }
  
  .dynamic-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
    padding-right: 0;
    padding-bottom: var(--spacing-xl);
  }
  
  .content-main {
    max-width: 100% !important;
  }
}
</style>