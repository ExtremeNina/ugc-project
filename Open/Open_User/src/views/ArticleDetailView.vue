<template>
  <div class="article-detail-container">
    <!-- 页面标题和返回按钮 -->
    <div class="page-header">
      <button class="back-button" @click="goBack">
        <i class="fa fa-arrow-left"></i> 返回
      </button>
      <h1>文章详情</h1>
    </div>

    <div class="main-content-wrapper">
      <!-- 左侧导航栏 -->
      <aside class="article-sidebar">
        <div class="sidebar-card">
          <h3 class="sidebar-title">文章导航</h3>
          <nav class="toc-nav">
            <ul class="toc-list">
              <li 
                v-for="(section, index) in sections" 
                :key="index" 
                class="toc-item"
                :class="{ 'toc-item-h3': section.level === 'h3' }"
              >
                <a 
                  :href="`#${section.id}`" 
                  class="toc-link" 
                  @click.prevent="scrollToSection(section.id)"
                  :class="{ active: activeSection === section.id }"
                >
                  {{ section.title }}
                </a>
              </li>
            </ul>
          </nav>
        </div>

        <div class="sidebar-card">
          <h3 class="sidebar-title">分享文章</h3>
          <div class="share-buttons">
            <button class="share-btn share-weibo">
              <i class="fa fa-weibo"></i> 微博
            </button>
            <button class="share-btn share-wechat">
              <i class="fa fa-weixin"></i> 微信
            </button>
            <button class="share-btn share-link">
              <i class="fa fa-link"></i> 复制链接
            </button>
          </div>
        </div>
      </aside>

      <!-- 主要内容区域 -->
      <main class="article-main">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- 错误提示 -->
        <div v-else-if="error" class="error-container">
          <p class="error-message">{{ error }}</p>
          <button class="retry-button" @click="fetchArticleDetail">重试</button>
        </div>

        <!-- 文章内容 -->
        <div v-else-if="article" class="article-content">
          <!-- 文章标题 -->
          <h1 class="article-title">{{ article.title || '未命名文章' }}</h1>
          
          <!-- 分类和标签 -->
          <div class="article-categories-tags">
            <div class="article-meta">
              <div class="article-author">
                <span class="author-label">作者：</span>
                <span class="author-name">{{ article.author ? article.author : (article.authorName || article.username || '未知') }}</span>
              </div>
              <div class="article-time">
                <span class="time-label">发布时间：</span>
                <span class="time-value">{{ article.createTime ? article.createTime : (article.createdAt || article.publishTime || '未知') }}</span>
              </div>
              <div class="article-category">
                 <span class="category-label">分类：</span>
                 <span class="category-name">{{ getCategoryName(article.categoryId || article.category || article.category_id || '未分类') }}</span>
               </div>
            </div>
            <!-- 单独一行显示标签 -->
            <div class="article-tags">
              <span class="tags-label">标签：</span>
              <template v-if="tags && tags.length > 0">
                <span v-for="(tag, index) in tags" :key="index" class="tag">
                  {{ tag }}
                </span>
              </template>
              <span v-else class="tag-empty">暂无标签</span>
            </div>
          </div>

          <!-- 文章摘要 -->
          <div v-if="article.summary" class="article-summary">
            <h2 class="summary-title">摘要</h2>
            <p class="summary-content">{{ article.summary }}</p>
          </div>

          <!-- 文章主体 -->
          <div v-if="article.content" class="article-body" ref="articleBody" v-html="article.content"></div>
          <div v-else class="article-body-placeholder">
            <p>暂无文章内容</p>
          </div>

          <!-- 封面图片 -->
          <div v-if="article.coverImageUrl || article.coverImage" class="article-cover-image">
            <div class="cover-label">封面图片</div>
            <img :src="processImageUrl(article.coverImageUrl || article.coverImage)" :alt="article.title || '文章封面'" class="cover-image" @error="handleImageError">
          </div>

          <!-- 文章统计和操作 -->
          <div class="article-actions">
            <div class="article-stats">
              <div class="stat-item">
                <i class="fa fa-eye"></i>
                <span>{{ article.viewCount !== undefined ? article.viewCount : 0 }} 浏览</span>
              </div>
              <div class="stat-item">
                <i class="fa fa-thumbs-up"></i>
                <span>{{ article.loveCount !== undefined ? article.loveCount : (article.likeCount !== undefined ? article.likeCount : 0) }} 点赞</span>
              </div>
              <div class="stat-item">
                <i class="fa fa-comment"></i>
                <span>{{ article.comments !== undefined ? article.comments : (article.commentCount !== undefined ? article.commentCount : 0) }} 评论</span>
              </div>
          </div>
          </div>
        </div>

        <!-- 无文章数据 -->
        <div v-else class="no-article">
          <p>文章数据不存在</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'ArticleDetailView',
  data() {
    return {
      article: null,
      tags: [],
      loading: false,
      error: null,
      sections: [],
      activeSection: '',
      scrollTimer: null,
      // 分类ID到分类名称的映射表
      categories: {
        1: '前端开发',
        2: '后端开发',
        3: '移动开发',
        4: '人工智能',
        5: 'DevOps',
        6: '数据库',
        7: '开发工具',
        8: '其他'
      }
    }
  },
  mounted() {
    // 设置网页标题为'萌技术社区 - 文章详情'
    document.title = '萌技术社区 - 文章详情';
    // 从路由参数中获取文章ID
    const articleId = this.$route.params.id
    if (articleId) {
      this.fetchArticleDetail()
    } else {
      this.error = '文章ID不存在'
    }
    
    // 监听滚动事件，更新当前激活的导航项
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    // 辅助函数：将下划线命名转换为驼峰命名
    underscoreToCamel(str) {
      return str.replace(/_([a-z])/g, function (g) {
        return g[1].toUpperCase();
      });
    },
    
    // 递归转换对象所有键为驼峰命名
    convertKeysToCamel(obj) {
      if (obj === null || typeof obj !== 'object') {
        return obj;
      }
      
      if (Array.isArray(obj)) {
        return obj.map(item => this.convertKeysToCamel(item));
      }
      
      const converted = {};
      for (const key in obj) {
        if (obj.hasOwnProperty(key)) {
          const camelKey = this.underscoreToCamel(key);
          converted[camelKey] = this.convertKeysToCamel(obj[key]);
        }
      }
      return converted;
    },
    
    // 检查用户登录状态
    checkLoginStatus() {
      return !!localStorage.getItem('isLoggedIn') || !!sessionStorage.getItem('isLoggedIn');
    },
    
    
    
    async fetchArticleDetail() {
      this.loading = true
      this.error = null
      
      try {
        // 从路由参数中获取文章ID
        const articleId = this.$route.params.id
        
        // 获取token
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        
        // 准备请求头
        const headers = {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
        
        // 无论是否有token，都添加Authorization头
        if (token && typeof token === 'string') {
          const cleanToken = token.trim()
          headers['Authorization'] = `Bearer ${cleanToken}`
          console.log('使用token进行认证:', cleanToken)
        } else {
          // 当没有token时，添加空的Authorization头以避免jwt为null的问题
          headers['Authorization'] = 'Bearer '
          console.log('未找到有效的token，添加空的Authorization头')
        }
        
        console.log('正在请求文章ID:', articleId)
        // 修改参数名为articleId以匹配后端要求
        console.log('请求URL:', `http://localhost:8081/api/create-center/${articleId}`)
        console.log('请求头信息:', headers)
        
        // 调用后端接口获取文章详情
        const response = await axios.get(
            `http://localhost:8081/api/create-center/${articleId}`,
            { headers }
          )
        
        console.log('获取文章详情响应状态:', response.status)
        console.log('获取文章详情响应:', response.data)
        
        // 根据接口返回的数据结构，提取文章数据和标签
        // 后端返回的是{article: ReviewVO, tags: List<String>}结构
        try {
          console.log('响应数据结构类型:', typeof response.data)
          console.log('响应数据完整内容:', JSON.stringify(response.data))
          
          // 初始化数据
          this.article = null
          this.tags = []
          
          // 检查响应数据是否存在且为对象
          if (response.data && typeof response.data === 'object') {
            console.log('响应数据键列表:', Object.keys(response.data))
            
            // 尝试直接从response.data中获取article和tags字段
            // 支持两种格式：直接在data中或在data.data中
            const resultData = response.data.data || response.data;
            
            if (resultData.article) {
              // 使用convertKeysToCamel函数将下划线命名转换为驼峰命名
              this.article = this.convertKeysToCamel(resultData.article);
              console.log('找到并转换article数据:', this.article);
            }
            
            if (resultData.tags) {
              this.tags = resultData.tags;
              console.log('找到tags数据:', this.tags);
            }
            
            // 如果没有找到article，但有其他数据，尝试直接使用
            if (!this.article && Object.keys(resultData).length > 0) {
              // 检查是否直接包含article的字段
              if (resultData.id !== undefined || resultData.title !== undefined) {
                this.article = this.convertKeysToCamel(resultData);
                console.log('直接使用响应数据作为article:', this.article);
              }
            }
            
            // 检查是否成功提取了文章数据
            if (this.article) {
              console.log('成功提取并转换文章数据:', this.article);
              // 检查是否已点赞（如果API返回这个信息）
              this.isLiked = this.article.isLiked || false;
            } else {
              console.warn('未找到符合ReviewVO结构的数据');
              // 仍然设置响应数据，以便前端能够显示一些内容
              this.article = response.data;
            }
          } else {
            console.error('响应数据不是有效的对象:', response.data);
            this.error = '获取文章数据失败';
          }
        } catch (parseError) {
          console.error('数据解析过程中发生错误:', parseError);
          this.error = '解析文章数据失败';
        }
        
        // 处理完成后，分析文章内容，生成导航
        this.$nextTick(() => {
          // 为了确保v-html内容完全渲染，再等待一个nextTick
          this.$nextTick(() => {
            this.generateSectionNavigation()
          })
        })
      } catch (error) {
        console.error('获取文章详情失败:', error)
        
        // 更详细的错误信息
        if (error.response) {
          console.error('响应错误状态:', error.response.status)
          console.error('响应错误数据:', error.response.data)
          
          // 根据不同的状态码提供更具体的错误信息
          if (error.response.status === 403) {
            this.error = '没有权限访问该文章'
            console.warn('403错误：可能需要登录或权限不足')
          } else if (error.response.status === 404) {
            this.error = '文章不存在'
          } else if (error.response.status === 500) {
            this.error = '服务器内部错误'
          } else {
            this.error = `加载失败: ${error.response.status || '未知错误'}`
          }
        } else if (error.request) {
          console.error('请求发送失败:', error.request)
          this.error = '无法连接到服务器，请检查网络或服务器是否运行'
        } else {
          console.error('请求配置错误:', error.message)
          this.error = `加载失败: ${error.message || '未知错误'}`
        }
      } finally {
        this.loading = false
      }
    },
    
    // 生成文章导航
    generateSectionNavigation() {
      if (!this.$refs.articleBody) return
      
      // 获取所有h2, h3标题
      const headings = this.$refs.articleBody.querySelectorAll('h2, h3')
      this.sections = []
      
      headings.forEach((heading, index) => {
        // 如果标题没有id，为其创建一个
        let id = heading.id
        if (!id) {
          id = `section-${index + 1}`
          heading.id = id
        }
        
        this.sections.push({
          id,
          title: heading.textContent.trim(),
          level: heading.tagName.toLowerCase()
        })
      })
      
      console.log('生成的导航节段:', this.sections)
      
      // 导航生成完成后，立即触发一次滚动检查，设置初始激活项
      this.$nextTick(() => {
        this.handleScroll()
      })
    },
    
    // 滚动到指定节段
    scrollToSection(sectionId) {
      const element = document.getElementById(sectionId)
      if (element) {
        // 暂停滚动监听，避免滚动过程中频繁更新activeSection
        window.removeEventListener('scroll', this.handleScroll)
        
        // 计算滚动位置，考虑顶部导航栏高度
        const headerOffset = 80
        const elementPosition = element.getBoundingClientRect().top
        const offsetPosition = elementPosition + window.pageYOffset - headerOffset
        
        // 先立即设置activeSection，提供即时反馈
        this.activeSection = sectionId
        
        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        })
        
        // 滚动动画完成后恢复滚动监听
        setTimeout(() => {
          window.addEventListener('scroll', this.handleScroll)
        }, 1000) // 平滑滚动通常在600-800ms完成，给1s确保足够
      }
    },
    
    // 处理滚动事件，更新当前激活的导航项
    handleScroll() {
      if (!this.sections.length) return
      
      // 使用节流优化性能
      if (this.scrollTimer) return
      
      this.scrollTimer = setTimeout(() => {
        const scrollPosition = window.scrollY + 100
        
        // 找到当前可见的最高节段
        let currentSection = ''
        
        // 优化查找逻辑：先检查当前激活的节段是否仍在视图中
        if (this.activeSection) {
          const currentActive = document.getElementById(this.activeSection)
          if (currentActive && currentActive.offsetTop <= scrollPosition) {
            // 检查下一个节段是否进入视图
            const currentIndex = this.sections.findIndex(s => s.id === this.activeSection)
            if (currentIndex < this.sections.length - 1) {
              const nextSection = document.getElementById(this.sections[currentIndex + 1].id)
              if (!nextSection || nextSection.offsetTop > scrollPosition) {
                // 当前节段仍然是活动的
                this.scrollTimer = null
                return
              }
            }
          }
        }
        
        // 完整遍历查找当前可见节段
        for (let i = this.sections.length - 1; i >= 0; i--) {
          const section = document.getElementById(this.sections[i].id)
          if (section) {
            // 使用getBoundingClientRect更准确地判断元素位置
            const rect = section.getBoundingClientRect()
            if (rect.top <= 100) {
              currentSection = this.sections[i].id
              break
            }
          }
        }
        
        if (currentSection !== this.activeSection) {
          this.activeSection = currentSection
        }
        
        this.scrollTimer = null
      }, 50) // 50ms的节流时间
    },
    

    
    goBack() {
      // 返回上一页
      this.$router.back()
    },
    
    // 根据分类ID或名称获取分类显示文本
    getCategoryName(category) {
      // 特殊处理'未分类'情况
      if (category === '未分类') {
        return '未分类';
      }
      // 首先检查article对象中是否有category_id字段
      if (!category && this.article && this.article.category_id) {
        category = this.article.category_id;
      }
      // 如果是数字ID，尝试从映射表中获取名称
      if (typeof category === 'number' || /^\d+$/.test(category)) {
        const categoryId = parseInt(category);
        // 如果在映射表中找到对应的名称，返回名称
        if (this.categories[categoryId]) {
          return this.categories[categoryId];
        }
        // 如果没找到，返回ID作为后备
        return `分类${categoryId}`;
      }
      // 如果已经是字符串名称，直接返回
      return category || '未分类';
    },
    
    // 处理minio图片URL
    processImageUrl(url) {
      // 检查URL是否合法
      if (!url || typeof url !== 'string') {
        return '';
      }
      
      // MinIO URL可能已经是完整的URL，直接返回
      // 如果需要对URL进行特殊处理，可以在这里添加逻辑
      return url;
    },
    
    // 处理图片加载错误
    handleImageError(event) {
      console.warn('封面图片加载失败:', event);
      // 可以在这里设置默认图片或隐藏图片元素
      // event.target.src = '/default-cover.jpg'; // 如果有默认图片
    }
  }
}
</script>

<style scoped>
.article-detail-container {
  min-height: 100vh;
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.page-header {
  background-color: var(--bg-secondary);
  padding: var(--spacing-lg) var(--spacing-lg) var(--spacing-md);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: var(--spacing-xl);
}

.page-header .back-button {
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--border-radius);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 14px;
  transition: all var(--transition-normal);
  margin-right: var(--spacing-lg);
}

.page-header .back-button:hover {
  background-color: var(--bg-tertiary);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.page-header h1 {
  display: inline-block;
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.main-content-wrapper {
  display: flex;
  gap: var(--spacing-xl);
  max-width: 1400px; /* 增加最大宽度 */
  margin: 0 auto;
  padding: 0 var(--spacing-lg) var(--spacing-xxl);
}

/* 左侧侧边栏 */
.article-sidebar {
  width: 240px; /* 减小侧边栏宽度 */
  flex-shrink: 0;
  position: sticky;
  top: var(--spacing-xl);
  height: fit-content;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

/* 主要内容区域 */
.article-main {
  flex: 1;
  min-width: 0; /* 确保flex子元素不会溢出容器 */
}

.article-content {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xl);
  transition: all var(--transition-normal);
  width: 100%; /* 确保内容占满可用宽度 */
}

.sidebar-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  transition: all var(--transition-normal);
}

.sidebar-card:hover {
  box-shadow: var(--shadow-md);
  border-color: rgba(242, 163, 58, 0.3);
}

.sidebar-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--border-color);
}

.toc-nav .toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-item-h3 {
  padding-left: 20px;
  font-size: 0.9em;
  color: var(--text-tertiary);
}

.toc-nav .toc-item {
  margin-bottom: var(--spacing-sm);
}

.toc-nav .toc-link {
  display: block;
  padding: var(--spacing-sm) var(--spacing-md);
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: var(--border-radius);
  transition: all var(--transition-normal);
  font-size: 14px;
  line-height: 1.5;
}

.toc-nav .toc-link:hover {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
}

.toc-nav .toc-link.active {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  font-weight: 500;
}

/* 分享按钮 */
.share-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.share-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal);
  font-size: 14px;
}

.share-btn:hover {
  background-color: var(--bg-primary);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

/* 主要内容区域 */
.article-main {
  flex: 1;
}

.article-content {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xl);
  transition: all var(--transition-normal);
}

.article-content:hover {
  box-shadow: var(--shadow-md);
  border-color: rgba(242, 163, 58, 0.3);
}

/* 文章标题 */
.article-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
  line-height: 1.3;
}

/* 分类和标签 */
.article-categories-tags {
  display: block;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.article-category {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.article-author {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.author-label {
  color: var(--text-secondary);
  font-size: 14px;
}

.author-name {
  color: var(--accent-primary);
  font-weight: 500;
  font-size: 14px;
}

.article-time {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-right: var(--spacing-md);
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.time-label {
  color: var(--text-secondary);
  font-size: 14px;
}

.time-value {
  color: var(--text-tertiary);
  font-size: 14px;
}

.category-label {
  color: var(--text-tertiary);
  font-size: 14px;
}

.category-name {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--border-radius);
  font-size: 14px;
}

.cover-label {
  position: absolute;
  top: var(--spacing-sm);
  left: var(--spacing-sm);
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius-sm);
  font-size: 12px;
  z-index: 10;
}

.article-cover-image {
  position: relative;
  font-weight: 500;
}

.article-tags {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.tags-label {
  color: var(--text-tertiary);
  font-size: 14px;
}

.tag {
  background-color: var(--bg-tertiary);
  color: var(--text-secondary);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--border-radius);
  font-size: 14px;
  border: 1px solid var(--border-color);
}

/* 文章摘要 */
.article-summary {
  background-color: var(--bg-tertiary);
  border-left: 4px solid var(--accent-primary);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
  border-radius: var(--border-radius);
}

.summary-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--accent-primary);
  margin-bottom: var(--spacing-md);
}

.summary-content {
  color: var(--text-primary);
  line-height: 1.7;
  margin: 0;
}

/* 文章主体 */
.article-body {
  margin-bottom: var(--spacing-xl);
  line-height: 1.8;
  color: var(--text-primary);
}

.article-body h1,
.article-body h2,
.article-body h3,
.article-body h4,
.article-body h5,
.article-body h6 {
  color: var(--text-primary);
  margin-top: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  scroll-margin-top: 80px;
}

.article-body h2 {
  font-size: 24px;
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--border-color);
}

.article-body h3 {
  font-size: 20px;
}

.article-body p {
  margin-bottom: var(--spacing-lg);
}

.article-body img {
  max-width: 100%;
  height: auto;
  border-radius: var(--border-radius);
  margin: var(--spacing-lg) 0;
  display: block;
}

.article-body blockquote {
  border-left: 4px solid var(--accent-primary);
  padding-left: var(--spacing-lg);
  margin: var(--spacing-lg) 0;
  color: var(--text-tertiary);
  font-style: italic;
}

.article-body code {
  background-color: var(--bg-tertiary);
  color: var(--accent-primary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.article-body pre {
  background-color: var(--bg-tertiary);
  padding: var(--spacing-lg);
  border-radius: var(--border-radius);
  overflow-x: auto;
  margin: var(--spacing-lg) 0;
}

.article-body pre code {
  background: none;
  padding: 0;
}

.article-body ul,
.article-body ol {
  margin: var(--spacing-lg) 0;
  padding-left: var(--spacing-xl);
}

.article-body li {
  margin-bottom: var(--spacing-sm);
}

/* 封面图片 */
.article-cover-image {
  margin-bottom: var(--spacing-xl);
  border-radius: var(--border-radius);
  overflow: hidden;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  background: linear-gradient(135deg, var(--bg-tertiary), #444);
  color: var(--text-secondary);
  font-size: 18px;
  text-align: center;
  padding: var(--spacing-xl);
}

.cover-image {
  width: 100%;
  max-height: 500px;
  height: auto;
  object-fit: contain;
  display: block;
  transition: transform var(--transition-slow);
}

.article-cover-image:hover {
  box-shadow: var(--shadow-md);
}

.article-cover-image:hover .cover-image {
  transform: scale(1.02);
}

/* 文章统计和操作 */
.article-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-color);
}

.article-stats {
  display: flex;
  gap: var(--spacing-xl);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--text-tertiary);
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: var(--spacing-md);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal);
  font-size: 14px;
}

.action-btn:hover {
  background-color: var(--accent-primary);
  border-color: var(--accent-primary);
  color: var(--bg-primary);
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: var(--text-tertiary);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top: 3px solid var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-lg);
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: var(--accent-error);
  text-align: center;
  padding: var(--spacing-xl);
}

.error-message {
  margin-bottom: var(--spacing-lg);
  font-size: 16px;
}

.retry-button {
  background-color: var(--accent-primary);
  color: var(--bg-primary);
  border: none;
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-normal);
}

.retry-button:hover {
  background-color: var(--text-primary);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* 无文章内容占位 */
.article-body-placeholder {
  padding: var(--spacing-xl);
  text-align: center;
  color: var(--text-tertiary);
  font-style: italic;
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  margin-bottom: var(--spacing-xl);
}

/* 标签为空 */
.tag-empty {
  color: var(--text-tertiary);
  font-size: 14px;
  font-style: italic;
}

/* 无文章数据 */
.no-article {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: var(--text-tertiary);
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .main-content-wrapper {
    flex-direction: column;
  }
  
  .article-sidebar {
    width: 100%;
    position: static;
    max-height: none;
  }
  
  .article-title {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: var(--spacing-md);
  }
  
  .page-header h1 {
    font-size: 20px;
  }
  
  .main-content-wrapper {
    padding: 0 var(--spacing-md) var(--spacing-xl);
    gap: var(--spacing-lg);
  }
  
  .article-content {
    padding: var(--spacing-lg);
  }
  
  .article-title {
    font-size: 24px;
  }
  
  .article-categories-tags {
    flex-direction: column;
    gap: var(--spacing-md);
  }
  
  .article-actions {
    flex-direction: column;
    gap: var(--spacing-lg);
    align-items: stretch;
  }
  
  .article-stats {
    justify-content: space-around;
  }
  
  .action-buttons {
    justify-content: center;
  }
}
</style>