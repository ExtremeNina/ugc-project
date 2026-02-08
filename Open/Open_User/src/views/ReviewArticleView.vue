<template>
  <div class="review-article-container">
    <!-- 页面标题和返回按钮 -->
    <div class="header">
      <button class="back-button" @click="goBack">
        <i class="fa fa-arrow-left"></i> 返回
      </button>
      <h1>待审核文章详情</h1>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="error" class="error-container">
      <p class="error-message">{{ error }}</p>
      <button class="retry-button" @click="fetchReviewArticle">重试</button>
    </div>

    <!-- 文章内容 -->
    <div v-else-if="article" class="article-content">
      <!-- 文章基本信息 -->
      <div class="article-header">
        <h2 class="article-title">{{ article.title }}</h2>
        <div class="article-meta">
          <span class="article-category">{{ article.category }}</span>
          <span class="article-date">{{ formatDate(article.createTime) }}</span>
          <span class="article-status">{{ getStatusText(article.status) }}</span>
        </div>
      </div>

      <!-- 文章摘要 -->
      <div v-if="article.summary" class="article-summary">
        <h3>摘要</h3>
        <p>{{ article.summary }}</p>
      </div>

      <!-- 文章内容 -->
      <div class="article-body">
        <div v-html="article.content"></div>
      </div>

      <!-- 文章标签 -->
      <div v-if="tags && tags.length > 0" class="article-tags">
        <h3>标签</h3>
        <div class="tags-container">
          <span v-for="tag in tags" :key="tag" class="tag">
            {{ tag }}
          </span>
        </div>
      </div>

      <!-- 文章统计信息 -->
      <div class="article-stats">
        <div class="stat-item">
          <i class="fa fa-eye"></i>
          <span>{{ article.viewCount || 0 }} 浏览</span>
        </div>
        <div class="stat-item">
          <i class="fa fa-thumbs-up"></i>
          <span>{{ article.loveCount || article.likeCount || 0 }} 点赞</span>
        </div>
      </div>
    </div>

    <!-- 无文章数据 -->
    <div v-else class="no-article">
      <p>未找到文章信息</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'ReviewArticleView',
  data() {
    return {
      article: null,
      tags: [],
      loading: false,
      error: null
    }
  },
  mounted() {
    // 设置网页标题为'萌技术社区 - 待审核文章详情'
    document.title = '萌技术社区 - 待审核文章详情';
    // 从路由参数中获取文章ID
    const articleId = this.$route.params.id
    if (articleId) {
      this.fetchReviewArticle()
    } else {
      this.error = '文章ID不存在'
    }
  },
  methods: {
    async fetchReviewArticle() {
      this.loading = true
      this.error = null
      
      try {
    const articleId = this.$route.params.id
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
    
    if (token && typeof token === 'string') {
      const cleanToken = token.trim()
      headers['Authorization'] = `Bearer ${cleanToken}`
    } else {
      headers['Authorization'] = 'Bearer '
    }
    
    console.log('正在请求文章ID:', articleId)
    
    // 修正URL
    const response = await axios.get(
          `http://localhost:8081/api/create-center/${articleId}`,
          { headers }
        )
    
    console.log('完整响应:', response)
    console.log('响应数据:', response.data)
    
    // 下划线命名转驼峰命名的辅助函数
    const underscoreToCamel = (str) => {
      return str.replace(/([-_][a-z])/ig, (match) => {
        return match.charAt(1).toUpperCase();
      });
    };
    
    // 递归转换对象的所有键名
    const convertKeysToCamel = (obj) => {
      if (typeof obj !== 'object' || obj === null) {
        return obj;
      }
      
      if (Array.isArray(obj)) {
        return obj.map(item => convertKeysToCamel(item));
      }
      
      const convertedObj = {};
      for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          const camelKey = underscoreToCamel(key);
          convertedObj[camelKey] = convertKeysToCamel(obj[key]);
        }
      }
      return convertedObj;
    };
    
    // 根据后端新的返回格式解析数据: {article: ReviewVO, tags: List<String>}
    if (response.data && response.data.code === 200) {
      const resultData = response.data.data
      
      if (resultData && typeof resultData === 'object') {
        // 直接从resultData中获取article和tags字段
        if (resultData.article && typeof resultData.article === 'object') {
          // 转换article对象中的字段名，从下划线命名转为驼峰命名
          // 例如: author_id -> authorId, category_id -> categoryId
          this.article = convertKeysToCamel(resultData.article)
          console.log('成功获取并转换文章数据:', this.article)
          
          // 获取标签列表，如果不存在则设为空数组
          this.tags = Array.isArray(resultData.tags) ? resultData.tags : []
          console.log('成功获取标签数据:', this.tags)
        } else {
          // 处理没有article字段的情况
          this.error = '数据结构不完整: 缺少article字段'
          console.error('缺少article字段:', resultData)
        }
      } else {
        this.error = '数据格式错误'
        console.error('resultData类型错误:', typeof resultData)
      }
    } else {
      this.error = response.data.msg || '获取文章数据失败'
    }
  } catch (error) {
    console.error('获取待审核文章详情失败:', error)
    if (error.response) {
      console.error('响应错误状态:', error.response.status)
      console.error('响应错误数据:', error.response.data)
      this.error = `加载失败: ${error.response.data?.msg || error.response.status}`
    } else if (error.request) {
      console.error('请求发送失败:', error.request)
      this.error = '无法连接到服务器，请检查网络'
    } else {
      this.error = '加载文章失败，请重试'
    }
  } finally {
    this.loading = false
  }
    },
    formatDate(dateString) {
      return dateString || ''
    },
    getStatusText(status) {
      switch (status) {
        case 'pending': return '待审核'
        case 'approved': return '已通过'
        case 'rejected': return '已拒绝'
        default: return status
      }
    },
    goBack() {
      this.$router.push('/articles/create/pending')
    }
  }
}
</script>

<style scoped>
.review-article-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
  border-bottom: 1px solid #eaeaea;
  padding-bottom: 15px;
}

.back-button {
  background: none;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  margin-right: 20px;
  transition: all 0.3s ease;
}

.back-button:hover {
  background-color: #f5f5f5;
  border-color: #ccc;
}

.header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  text-align: center;
}

.error-message {
  color: #e74c3c;
  margin-bottom: 20px;
  font-size: 16px;
}

.retry-button {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.retry-button:hover {
  background-color: #2980b9;
}

.article-content {
  background-color: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.article-header {
  margin-bottom: 30px;
}

.article-title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 15px;
  color: #333;
}

.article-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
  flex-wrap: wrap;
}

.article-category {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
}

.article-status {
  background-color: #fff3e0;
  color: #f57c00;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
}

.article-summary {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 6px;
  border-left: 4px solid #3498db;
}

.article-summary h3 {
  margin-top: 0;
  color: #555;
  font-size: 18px;
  margin-bottom: 10px;
}

.article-summary p {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.article-body {
  margin-bottom: 40px;
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
}

.article-body h1,
.article-body h2,
.article-body h3,
.article-body h4,
.article-body h5,
.article-body h6 {
  margin-top: 30px;
  margin-bottom: 15px;
  color: var(--text-primary);
}

.article-body p {
  margin-bottom: 20px;
}

.article-body img {
  max-width: 100%;
  height: auto;
  margin: 20px 0;
  border-radius: 4px;
}

.article-body blockquote {
  border-left: 4px solid #3498db;
  padding-left: 20px;
  margin: 20px 0;
  color: #7f8c8d;
  font-style: italic;
}

.article-tags {
  margin-bottom: 30px;
}

.article-tags h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #555;
  font-size: 18px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag {
  background-color: #ecf0f1;
  color: #2c3e50;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 14px;
  display: inline-block;
}

.article-stats {
  display: flex;
  gap: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  color: #7f8c8d;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.no-article {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
  color: #7f8c8d;
  font-size: 18px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .review-article-container {
    padding: 15px;
  }
  
  .article-title {
    font-size: 24px;
  }
  
  .article-meta {
    gap: 10px;
  }
  
  .article-content {
    padding: 20px;
  }
}
</style>