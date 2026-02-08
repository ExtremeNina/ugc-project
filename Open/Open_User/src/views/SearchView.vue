<template>
  <div class="search-container">
    <!-- 顶部搜索栏 -->
    <header class="search-header">
      <div class="header-content">
        <!-- 平台标识 -->
        <div class="logo-container">
          <h1 class="community-title">
            <span class="logo-text">萌技术社区</span>
          </h1>
        </div>
        
        <!-- 搜索框 -->
        <div class="search-bar">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索感兴趣的内容..." 
            class="search-input"
            @keyup.enter="handleSearch"
          >
          <button class="search-btn" @click="handleSearch">
            <span class="search-icon">🔍</span>
          </button>
        </div>
        
        <!-- 右侧功能按钮区 -->
        <div class="function-buttons">
          <!-- 移除了写文章、个人模块和私信模块 -->
        </div>
      </div>
    </header>
    
    <!-- 搜索结果区域 -->
    <main class="search-results">
      <div class="results-container">
        <div class="results-header">
          <h2 class="results-title">
            {{ searchQuery ? `"${searchQuery}"` : '' }} 搜索结果
          </h2>
          <div class="filter-options">
            <button 
              v-for="filter in filters" 
              :key="filter.value"
              :class="['filter-btn', { active: selectedFilter === filter.value }]"
              @click="selectedFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>
        
        <!-- 搜索结果列表 -->
        <div class="results-list">
          <div v-if="searchResults.length > 0" class="results-grid">
            <div v-for="result in searchResults" :key="result.id" class="result-item">
              <div class="result-card" @click="goToArticleDetail(result.id)">
                <!-- 左侧图片区域 -->
                <div class="result-cover">
                  <img v-if="result.cover" :src="result.cover" alt="文章封面">
                  <div v-else class="no-cover">
                    <span class="no-cover-icon">{{ result.categoryIcon || '📝' }}</span>
                  </div>
                </div>
                <!-- 右侧内容区域 -->
                <div class="result-content">
                  <!-- 标题 -->
                  <h3 class="result-title">{{ result.title }}</h3>
                  
                  <!-- 底部信息和操作区 - 整合到同一行 -->
                  <div class="result-footer">
                    <!-- 元信息区域（分类、作者和日期） -->
                    <div class="result-meta">
                      <!-- 显示分类 -->
                      <span v-if="result.category" class="result-category">分类：{{ result.category }}</span>
                      <!-- 作者信息 -->
                      <div class="author-info">
                        <img v-if="result.userIcon" :src="result.userIcon" alt="用户头像" class="user-avatar">
                        <span class="result-author">{{ result.author }}</span>
                      </div>
                      <!-- 显示发布时间、浏览量和点赞数 -->
                      <span v-if="result.date" class="result-date">发布时间：{{ result.date }} <span class="view-count"><i class="fas fa-eye text-neutral-400"></i> {{ result.viewCount || 0 }}</span> <span class="like-count"><i class="fas fa-thumbs-up" :class="{ 'text-red-500': result.isLove, 'text-neutral-400': !result.isLove }"></i> {{ result.loveCount || result.likes || 0 }}</span></span>
                    </div>
                    
                    <!-- 交互按钮区域 -->
                    <div class="result-actions">
                      <!-- 点赞按钮 -->
                      <button class="like-btn" :class="{ liked: result.isLove }" @click.stop="handleLike(result.id)">
                        <span class="like-text">{{ result.isLove ? '已点赞' : '点赞' }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 无搜索结果时的提示 -->
          <div v-else-if="searchQuery" class="no-results">
            <div class="no-results-icon"><i class="fas fa-search text-4xl text-neutral-400"></i></div>
            <h3 class="no-results-title">没有找到相关文章</h3>
            <p class="no-results-text">请尝试使用其他关键词或筛选条件</p>
          </div>
          
          <!-- 未搜索时的提示 -->
          <div v-else class="empty-search">
            <div class="empty-search-icon">🔍</div>
            <h3 class="empty-search-title">开始你的搜索</h3>
            <p class="empty-search-text">在上方搜索框中输入关键词，查找你感兴趣的内容</p>
          </div>
        </div>
        
        <!-- 分页组件 -->
        <div v-if="searchResults.length > 0" class="pagination">
          <button class="page-btn" :disabled="currentPage === 1">上一页</button>
          <span class="page-info">第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
          <button class="page-btn" :disabled="currentPage === totalPages">下一页</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import axios from 'axios'
import LoveService from '../services/loveService.js'

export default {
  name: 'SearchView',
  data() {
    return {
      searchQuery: '',
      searchResults: [],
      selectedFilter: 'all',
      currentPage: 1,
      totalPages: 1,
      followingUsers: [], // 存储已关注的用户ID
      filters: [
        { label: '全部', value: 'all' },
        { label: '文章', value: 'article' },
        { label: '标签', value: 'tag' },
        { label: '作者', value: 'author' }
      ]
    }
  },
  mounted() {
    // 设置网页标题为'萌技术社区 - 搜索页面'
    document.title = '萌技术社区 - 搜索页面';
    // 从URL参数获取搜索关键词
    const query = this.$route.query.q
    if (query) {
      this.searchQuery = query
      this.handleSearch()
    }
  },
  methods: {
    // 处理搜索
    async handleSearch() {
      // 不支持空搜索
      const keyword = this.searchQuery.trim()
      if (!keyword) {
        alert('请输入搜索关键词')
        return
      }
      
      console.log('开始搜索:', keyword)
      
      try {
        // 获取token
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        console.log('使用的token:', token ? '存在' : '不存在')
        
        // 准备请求配置
        const config = {
          headers: {
            'Authorization': token ? `Bearer ${token}` : 'Bearer '
          }
        }
        
        console.log('请求配置:', {
          url: 'http://localhost:8081/api/search/articles',
          params: { keyword: keyword },
          headers: config.headers
        })
        
        // 发送请求
        const response = await axios.get(
          'http://localhost:8081/api/search/articles',
          {
            params: { keyword: keyword },
            ...config
          }
        )
        
        console.log('搜索响应成功:', {
          status: response.status,
          data: response.data
        })
        
        // 灵活处理不同格式的响应数据
        let articles = []
        if (Array.isArray(response.data)) {
          articles = response.data
        } else if (response.data && response.data.data) {
          articles = Array.isArray(response.data.data) ? response.data.data : []
        } else if (response.data) {
          // 尝试将响应数据作为单个文章处理
          articles = [response.data]
        }
        
        this.formatSearchResults(articles)
      } catch (error) {
        console.error('搜索请求失败:', error)
        
        // 详细的错误信息记录
        const errorDetails = {
          status: error.response?.status,
          statusText: error.response?.statusText,
          data: error.response?.data,
          message: error.message,
          request: error.request ? '存在' : '不存在'
        }
        console.log('错误详情:', errorDetails)
        
        // 即使在404情况下也显示空结果，避免页面空白
        this.searchResults = []
        this.totalPages = 1
        
        // 如果是404，可能是正常的搜索无结果情况
        if (error.response?.status === 404) {
          console.log('搜索未找到匹配结果')
        }
      }
    },
    
    // 格式化搜索结果，将SearchArticleVO转换为前端显示所需格式
    formatSearchResults(articleVOs) {
      console.log('原始文章数据:', articleVOs);
      this.searchResults = articleVOs.map(article => {
        console.log('单篇文章数据:', article);
        console.log('isLove值:', article.isLove);
        return {
          id: article.articleId || article.id,
          title: article.title,
          excerpt: '',
          cover: article.coverImageUrl || '',
          userIcon: article.userIcon || '',
          category: article.category || '',
          categoryIcon: '📝',
          date: article.publishTime || '',
          author: article.author || '未知作者',
          url: article.url || '',
          // 使用loveCount作为点赞数
          likes: article.loveCount ? parseInt(article.loveCount) : 0,
          // 添加ViewCount字段处理
          viewCount: article.viewCount || 0,
          // 使用isLove表示用户是否已点赞
          isLove: Boolean(article.isLove),
          userId: article.userId || ''
        };
      })
      
      this.totalPages = Math.ceil(this.searchResults.length / 10)
      this.currentPage = 1
    },
    
    // 直接返回后端传递的日期字符串
    formatDate(dateString) {
      return dateString || ''
    },
    
    // 检查登录状态
    checkLoginStatus() {
      this.isLoggedIn = !!localStorage.getItem('isLoggedIn') || !!sessionStorage.getItem('isLoggedIn')
      console.log('登录状态:', this.isLoggedIn ? '已登录' : '未登录')
      return this.isLoggedIn
    },
    
    // 处理点赞/取消点赞
    async handleLike(articleId) {
      // 检查登录状态
      if (!this.checkLoginStatus()) {
        alert('请先登录后再进行点赞操作')
        return
      }
      
      try {
        // 找到对应的文章
        const article = this.searchResults.find(item => item.id === articleId)
        if (!article) {
          console.error('找不到对应的文章:', articleId)
          return
        }
        
        // 准备数据
        const loveData = {
          id: article.articleId || article.id, // 确保使用正确的文章ID作为实体id
          loveTypeId: 1 // 文章点赞类型
        }
        
        // 使用同一接口进行点赞/取消点赞操作
        await LoveService.toggleLove(loveData)
        
        // 根据当前状态更新UI和显示提示信息
        if (article.isLove) {
          // 取消点赞
          article.isLove = false
          article.likes = Math.max(0, (article.likes || 0) - 1)
          alert('取消点赞成功')
        } else {
          // 点赞
          article.isLove = true
          article.likes = (article.likes || 0) + 1
          alert('点赞成功')
        }
        
      } catch (error) {
        console.error('点赞/取消点赞操作失败:', error)
        // 错误处理已在LoveService中完成
      }
    },
    
    // 检查是否已点赞 - 从searchResults中获取isLove属性
    isLiked(articleId) {
      const article = this.searchResults.find(item => item.id === articleId)
      return article ? article.isLove : false
    },
    
    // 跳转到文章详情页面
    goToArticleDetail(articleId) {
      // 尝试跳转到ArticleDetailPublishView，由于该组件可能不存在，使用articleDetail路由
      // 从搜索页跳转时传递来源信息和搜索关键词
      this.$router.push({
        path: `/article/${articleId}`,
        query: { from: 'search', q: this.searchQuery }
      });
    },
    
    // 处理关注
    handleFollow(userId, authorName) {
      console.log('关注用户:', userId, authorName)
      // 在实际应用中，这里会发送请求到后端进行关注操作
      // 这里简单模拟关注状态切换
      if (!this.followingUsers) {
        this.followingUsers = []
      }
      
      const index = this.followingUsers.indexOf(userId)
      if (index > -1) {
        // 取消关注
        this.followingUsers.splice(index, 1)
      } else {
        // 关注
        this.followingUsers.push(userId)
      }
    },
    
    // 检查是否已关注
    isFollowing(userId) {
      return this.followingUsers && this.followingUsers.includes(userId)
    }
  }
}
</script>

<style scoped>
/* 搜索页面容器 */
.search-container {
  width: 100%;
  min-height: 100vh;
  background-color: var(--bg-primary);
}

/* 用户头像样式 */
.user-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
  object-fit: cover;
}

/* 作者信息样式 */
.author-info {
  display: flex;
  align-items: center;
}

/* 头部样式 */
.search-header {
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
}

/* 平台标识 */
.logo-container {
  flex: 0 0 auto;
}

.community-title {
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
  margin: 0;
}

.logo-text {
  color: var(--accent-primary);
}

/* 搜索栏 */
.search-bar {
  flex: 1;
  max-width: 500px;
  margin: 0 40px;
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 45px 0 15px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.search-input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px rgba(242, 163, 58, 0.2);
}

.search-btn {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  height: 30px;
  width: 30px;
  border: none;
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s;
}

.search-btn:hover {
  background-color: #e09331;
}

.search-icon {
  font-size: 16px;
}

/* 功能按钮区 */
.function-buttons {
  display: flex;
  align-items: center;
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
  background-color: #e09331;
}

.create-icon {
  margin-right: 5px;
  font-size: 16px;
}

/* 图标按钮 */
.icon-buttons {
  display: flex;
  align-items: center;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border: none;
  background-color: transparent;
  color: var(--text-secondary);
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: background-color 0.3s;
}

.icon-btn:hover {
  background-color: var(--bg-tertiary);
}

.notification-icon {
  font-size: 20px;
}

.notification-badge {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 16px;
  height: 16px;
  background-color: var(--accent-error);
  color: var(--bg-primary);
  border-radius: 50%;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 用户下拉菜单 */
.user-dropdown {
  position: relative;
}

.user-center-btn {
  font-size: 14px;
  font-weight: 500;
}

.user-login-text {
  color: var(--accent-primary);
}

.user-icon {
  font-size: 20px;
  color: var(--text-secondary);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: var(--shadow-md);
  min-width: 120px;
  margin-top: 8px;
  z-index: 1000;
}

.dropdown-item {
  width: 100%;
  padding: 10px 15px;
  border: none;
  background-color: transparent;
  text-align: left;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: background-color 0.3s;
}

.dropdown-item:hover {
  background-color: var(--bg-tertiary);
}

.item-icon {
  margin-right: 8px;
  font-size: 16px;
}

/* 搜索结果区域 */
.search-results {
  flex: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  width: 100%;
}

.results-container {
  background-color: var(--bg-secondary);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  padding: 20px;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--border-color);
}

.results-title {
  font-size: 20px;
  font-weight: bold;
  color: var(--text-primary);
  margin: 0;
}

.filter-options {
  display: flex;
  gap: 10px;
}

.filter-btn {
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  border-radius: 16px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s;
}

.filter-btn:hover {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.filter-btn.active {
  background-color: var(--accent-primary);
  border-color: var(--accent-primary);
  color: var(--bg-primary);
}

/* 搜索结果列表 */
.results-list {
  width: 100%;
}

.results-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
  transition: all 0.3s;
}

.result-item:hover {
  transform: translateX(4px);
}

/* 横向排列的卡片样式 */
.result-card {
  display: flex;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  padding: 16px;
}

.result-item:hover .result-card {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: rgba(242, 163, 58, 0.3);
}

/* 左侧图片部分 */
.result-cover {
  width: 120px;
  height: 80px;
  flex-shrink: 0;
  overflow: hidden;
  background-color: #e8e8e8;
  border-radius: 8px;
  margin-right: 20px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  display: block;
}

.result-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.result-item:hover .result-cover img {
  transform: scale(1.05);
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e8e8e8;
}

.no-cover-icon {
  font-size: 24px;
  color: #999;
}

/* 右侧内容部分 */
.result-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

/* 标题样式 */
.result-title {
  font-size: 18px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

/* 作者信息样式 */
.author-info {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

/* 元信息样式 - 优化间距和布局 */
.result-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 14px;
  flex-wrap: wrap;
  margin: 0;
  flex: 1; /* 让元信息区域占据剩余空间 */
}

/* 分类标签样式 */
.result-category {
  background-color: rgba(242, 163, 58, 0.1);
  color: #f2a33a;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

/* 日期样式 */
.result-date {
  color: #FFFFFF;
  font-size: 14px;
}

/* 作者信息样式 */
.author-info {
  display: flex;
  align-items: center;
}

/* 用户头像样式 */
.user-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  margin-right: 6px;
  object-fit: cover;
}

/* 作者名称样式 */
.result-author {
  color: #FFFFFF;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 点赞数样式 */
.like-count {
  margin-left: 8px;
  color: #FFFFFF;
  font-size: 14px;
}

/* 浏览量样式 */
.view-count {
  margin-left: 8px;
  color: #FFFFFF;
  font-size: 14px;
}

/* 无搜索结果和空搜索状态 */
.no-results, .empty-search {
  text-align: center;
  padding: 80px 20px;
  color: var(--text-tertiary);
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  border: 1px dashed var(--border-color);
  margin-top: 30px;
}

.no-results-icon, .empty-search-icon {
  font-size: 56px;
  margin-bottom: 20px;
  color: var(--accent-primary);
  opacity: 0.7;
}

.no-results-title, .empty-search-title {
  font-size: 22px;
  font-weight: bold;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.no-results-text, .empty-search-text {
  font-size: 15px;
  color: var(--text-tertiary);
  margin: 0;
  max-width: 500px;
  margin: 0 auto;
  line-height: 1.5;
}

/* 底部信息和操作区样式 */
.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  width: 100%;
}

/* 交互按钮区域样式 */
.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 0; /* 移除原来的margin-top */
}

/* 点赞按钮样式 */
.like-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  color: #FFFFFF;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.like-btn:hover {
  background-color: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.like-btn:active {
  transform: translateY(0);
}

.like-icon {
  font-size: 16px;
}

/* 关注按钮样式 */
.follow-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background-color: #f2a33a;
  border: 1px solid #f2a33a;
  border-radius: 16px;
  color: #000000;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.follow-btn:hover {
  background-color: #ffb450;
  border-color: #ffb450;
  transform: translateY(-1px);
}

.follow-btn:active {
  transform: translateY(0);
}

/* 已关注状态样式 */
.follow-btn.following {
  background-color: transparent;
  border-color: rgba(255, 255, 255, 0.3);
  color: #FFFFFF;
}

.follow-icon {
  font-size: 16px;
  font-weight: bold;
}

/* 分页组件 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30px;
  gap: 20px;
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  border-radius: 4px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--text-tertiary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-wrap: wrap;
    height: auto;
    padding: 10px 20px;
  }
  
  .search-bar {
    order: 3;
    margin: 10px 0;
    max-width: 100%;
  }
  
  .logo-container {
    order: 1;
  }
  
  .function-buttons {
    order: 2;
  }
  
  .results-grid {
    grid-template-columns: 1fr;
  }
  
  .results-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .filter-options {
    flex-wrap: wrap;
  }
}
</style>