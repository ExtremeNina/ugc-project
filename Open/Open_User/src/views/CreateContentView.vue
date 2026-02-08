<template>
  <div class="create-content-container">
    <div class="content-sidebar">
      <div class="sidebar-header">
        <h3>创作中心</h3>
        <p>发布高质量的技术文章</p>
      </div>
      
      <div class="sidebar-stats">
        <div class="stat-item">
          <span class="stat-number">{{ userStats.articles }}</span>
          <span class="stat-label">已发布</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ userStats.drafts }}</span>
          <span class="stat-label">草稿</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ userStats.likes }}</span>
          <span class="stat-label">获得点赞</span>
        </div>
      </div>
      
      <div class="sidebar-nav">
        <button class="nav-button active" @click="showDataOverview">
          <i class="fa fa-bar-chart"></i> 数据总览
        </button>
        <button class="nav-button" @click="showCreateForm">
          <i class="fa fa-edit"></i> 撰写新文章
        </button>
        <button class="nav-button" @click="showDrafts">
          <i class="fa fa-save"></i> 我的草稿
        </button>
        <button class="nav-button" @click="showPublished">
          <i class="fa fa-newspaper-o"></i> 已发布
        </button>
        <button class="nav-button" @click="showPendingArticles">
          <i class="fa fa-clock-o"></i> 待审核
        </button>
      </div>
    </div>
    
    <div class="content-main" v-if="activeTab === 'data'">
      <div class="editor-header">
        <h2>数据总览</h2>
      </div>
      <div class="data-overview">
        <!-- 文章统计卡片 -->
        <div class="overview-cards">
          <div class="overview-card">
            <div class="card-content">
              <h3>总文章数</h3>
              <p class="card-value">{{ userStats.articles }}</p>
              <p class="card-trend">本月新增: <span class="trend-up">+5</span></p>
            </div>
            <div class="card-icon articles-icon">
              <i class="fa fa-file-text"></i>
            </div>
          </div>
          <div class="overview-card">
            <div class="card-content">
              <h3>总阅读量</h3>
              <p class="card-value">{{ userStats.reads || 0 }}</p>
              <p class="card-trend">本月新增: <span class="trend-up">+120</span></p>
            </div>
            <div class="card-icon reads-icon">
              <i class="fa fa-eye"></i>
            </div>
          </div>
          <div class="overview-card">
            <div class="card-content">
              <h3>总点赞数</h3>
              <p class="card-value">{{ userStats.likes }}</p>
              <p class="card-trend">本月新增: <span class="trend-up">+35</span></p>
            </div>
            <div class="card-icon likes-icon">
              <i class="fa fa-thumbs-up"></i>
            </div>
          </div>
          <div class="overview-card">
            <div class="card-content">
              <h3>总评论数</h3>
              <p class="card-value">{{ userStats.comments || 0 }}</p>
              <p class="card-trend">本月新增: <span class="trend-up">+22</span></p>
            </div>
            <div class="card-icon comments-icon">
              <i class="fa fa-comments"></i>
            </div>
          </div>
        </div>
        <!-- 文章数据图表 -->
        <div class="data-charts">
          <div class="chart-card">
            <h3>文章阅读趋势</h3>
            <div class="chart-placeholder">
              <i class="fa fa-line-chart"></i>
              <p>阅读趋势图表</p>
            </div>
          </div>
          <div class="chart-card">
            <h3>文章分类统计</h3>
            <div class="chart-placeholder">
              <i class="fa fa-pie-chart"></i>
              <p>分类统计图表</p>
            </div>
          </div>
        </div>
        <!-- 最近文章数据 -->
        <div class="recent-articles-data">
          <h3>最近文章数据</h3>
          <div class="articles-data-list">
            <div class="article-data-item" v-for="article in recentArticles" :key="article.id">
              <div class="article-info">
                <h4>{{ article.title }}</h4>
                <p class="article-meta">{{ article.createTime }}</p>
              </div>
              <div class="article-stats">
                <span class="stat-item"><i class="fa fa-eye"></i> {{ article.reads || 0 }}</span>
                <span class="stat-item"><i class="fa fa-thumbs-up"></i> {{ article.likes || 0 }}</span>
                <span class="stat-item"><i class="fa fa-comments"></i> {{ article.comments || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="content-main" v-else-if="activeTab === 'create'">
      <div class="editor-header">
        <h2>撰写新文章</h2>
        <div class="header-actions">
          <button class="action-button secondary" @click="saveDraft" :disabled="isSaving">
            <i class="fa fa-save"></i> {{ isSaving ? '保存中...' : '保存草稿' }}
          </button>
          <button class="action-button primary" @click="publishArticle" :disabled="isPublishing || !canPublish">
            <i class="fa fa-paper-plane"></i> {{ isPublishing ? '发布中...' : '发布文章' }}
          </button>
        </div>
      </div>
      
      <div class="editor-form">
        <div class="form-group">
          <label for="article-title">文章标题</label>
          <input 
            type="text" 
            id="article-title" 
            v-model="article.title" 
            placeholder="请输入吸引人的标题..."
            class="title-input"
          >
        </div>
        
        <div class="form-group">
          <label for="article-category">文章分类</label>
          <select 
            id="article-category" 
            v-model="article.category" 
            class="category-select"
          >
            <option value="">请选择分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}
            </option>
          </select>
        </div>
        
        <div class="form-group">
          <label for="article-tags">标签</label>
          <div class="tags-input-container">
            <input 
              type="text" 
              id="article-tags" 
              disabled
              placeholder="请从下方选择预设标签"
              class="tags-input"
            >
            <div class="tags-container">
              <span 
                v-for="(tag, index) in article.tags" 
                :key="tag.id" 
                class="tag"
              >
                {{ tag.name }}
                <button class="tag-remove" @click="removeTag(index)">&times;</button>
              </span>
            </div>
            <!-- 预设标签列表 -->
            <div class="preset-tags">
              <span 
                v-for="preset in presetTags" 
                :key="preset.id" 
                class="preset-tag" 
                :class="{ 'selected': article.tags.some(tag => tag.id === preset.id) }"
                @click="togglePresetTag(preset.id, preset.name)"
              >
                {{ preset.name }}
              </span>
            </div>
          </div>
        </div>
        
        <div class="form-group">
          <label for="article-content">文章内容</label>
          <editor
            api-key="no-api-key"
            v-model="article.content"
            :init="{
              height: 700,
              menubar: true,
              language: 'zh_CN',
              plugins: [
                'advlist autolink lists link image charmap print preview anchor',
                'searchreplace visualblocks code fullscreen',
                'insertdatetime media table paste code help wordcount',
                'codesample table hr'
              ],
              toolbar: 'undo redo | formatselect | bold italic backcolor | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | removeformat | help | link image media | codesample table hr',
              content_style: 'body { font-family:Helvetica,Arial,sans-serif; font-size:16px; max-width: 900px; margin: 0; padding: 20px; text-align: left; }',
              codesample_languages: [
                {text: 'JavaScript', value: 'javascript'},
                {text: 'HTML/XML', value: 'markup'},
                {text: 'CSS', value: 'css'},
                {text: 'Java', value: 'java'},
                {text: 'Python', value: 'python'},
                {text: 'PHP', value: 'php'},
                {text: 'C++', value: 'cpp'},
                {text: 'C#', value: 'csharp'}
              ],
              // 启用本地化
              // 启用自动保存功能
              autosave_ask_before_unload: true,
              autosave_interval: '30s',
              autosave_prefix: '{path}{query}-{id}-',
              autosave_restore_when_empty: false,
              autosave_retention: '2m'
            }"
          ></editor>
        </div>
        
        <div class="form-group">
          <label for="article-summary">文章摘要</label>
          <textarea 
            id="article-summary" 
            v-model="article.summary" 
            placeholder="简要描述你的文章内容（最多200字）"
            class="summary-input"
            maxlength="200"
            rows="3"
          ></textarea>
          <div class="char-count">{{ article.summary.length }}/200</div>
        </div>
        
        <div class="form-group">
          <label for="article-cover">封面图片</label>
          <div class="cover-upload">
            <input 
              type="file" 
              id="article-cover" 
              accept="image/*" 
              @change="handleCoverUpload"
              class="cover-input"
            >
            <label for="article-cover" class="cover-label">
              <i class="fa fa-upload"></i>
              <span v-if="!article.coverImageUrl">上传封面图片</span>
              <span v-else>更换封面图片</span>
            </label>
            <div v-if="previewCoverImage" class="cover-preview">
              <img :src="previewCoverImage" alt="封面预览">
              <button class="remove-cover" @click="removeCover">&times;</button>
            </div>
          </div>
        </div>
        
        <div class="form-group">
          <label class="publish-settings-label">
            <input type="checkbox" v-model="article.allowComments">
            允许评论
          </label>
        </div>
      </div>
    </div>
    
    <div class="content-main" v-else-if="activeTab === 'drafts'">
      <div class="section-header">
        <h2>我的草稿</h2>
      </div>
      <div class="search-container">
        <div class="search-group">
          <input type="text" class="search-input" placeholder="按标题搜索..." v-model="searchTitle">
          <button class="search-button" @click="searchByTitle">
            <i class="fa fa-search"></i> 标题搜索
          </button>
        </div>
        <div class="search-group">
          <input type="date" class="search-input" v-model="searchDate">
          <button class="search-button" @click="searchByDate">
            <i class="fa fa-calendar"></i> 时间搜索
          </button>
        </div>
        <div class="search-group">
          <select class="search-input" v-model="searchTag">
            <option value="">请选择标签...</option>
            <option v-for="tag in presetTags" :key="tag.id" :value="tag.name">{{ tag.name }}</option>
          </select>
          <button class="search-button" @click="searchByTag">
            <i class="fa fa-tags"></i> 标签搜索
          </button>
        </div>
      </div>
      <div class="drafts-list" v-if="drafts.length > 0">
        <div 
          v-for="draft in drafts" 
          :key="draft.id" 
          class="draft-item"
        >
          <div class="item-cover">
            <img v-if="draft.coverImageUrl" :src="draft.coverImageUrl" alt="封面" class="cover-image">
            <div v-else class="item-icon">
              <i class="fa fa-file-text-o"></i>
            </div>
          </div>
          <div class="item-content">
            <h3>{{ draft.title || '无标题草稿' }}</h3>
            <p class="draft-meta">
              <span> 更新时间: {{ draft.updateTime ? new Date(draft.updateTime).toLocaleString() : (draft.createTime ? new Date(draft.createTime).toLocaleString() : '未知') }} <span v-if="draft.createTime"> | 创建时间: {{ new Date(draft.createTime).toLocaleString() }}</span> </span>
              <span v-if="draft.status === 1"><i class="fa fa-pencil"></i> 未发布</span>
              <span v-if="draft.categoryName">{{ draft.categoryName }}</span>
            </p>
            <div class="draft-actions">
              <button class="action-button small" @click="editDraft(draft)">
                <i class="fa fa-edit"></i> 编辑
              </button>
              <button class="action-button small secondary" @click="deleteDraft(draft.id)">
                <i class="fa fa-trash"></i> 删除
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <i class="fa fa-file-text-o"></i>
        <p v-if="searchEmpty">当前没有查到相关文章</p>
        <p v-else>你还没有保存任何草稿</p>
        <button class="action-button" @click="showCreateForm">开始创作</button>
      </div>
    </div>
    
    <div class="content-main" v-else-if="activeTab === 'published'">
      <div class="section-header">
        <h2>已发布文章</h2>
      </div>
      <div class="search-container">
        <div class="search-group">
          <input type="text" class="search-input" placeholder="按标题搜索..." v-model="searchTitle">
          <button class="search-button" @click="searchByTitle">
            <i class="fa fa-search"></i> 标题搜索
          </button>
        </div>
        <div class="search-group">
          <input type="date" class="search-input" v-model="searchDate">
          <button class="search-button" @click="searchByDate">
            <i class="fa fa-calendar"></i> 时间搜索
          </button>
        </div>
        <div class="search-group">
          <select class="search-input" v-model="searchTag">
            <option value="">请选择标签...</option>
            <option v-for="tag in presetTags" :key="tag.id" :value="tag.name">{{ tag.name }}</option>
          </select>
          <button class="search-button" @click="searchByTag">
            <i class="fa fa-tags"></i> 标签搜索
          </button>
        </div>
      </div>
      <div class="published-list" v-if="publishedArticles.length > 0">
        <div 
          v-for="article in publishedArticles" 
          :key="article.id" 
          class="published-item"
        >
          <div class="item-cover">
            <img v-if="article.coverImageUrl" :src="article.coverImageUrl" alt="封面" class="cover-image">
            <div v-else class="item-icon">
              <i class="fa fa-newspaper-o"></i>
            </div>
          </div>
          <div class="item-content">
            <h3>{{ article.title }}</h3>
            <p class="article-meta">
              <span>{{ article.categoryName }}</span>
              <span> 更新时间: {{ article.updateTime || article.updatedAt || article.createTime || article.createdAt || article.publishTime || '未知' }} <span v-if="article.createTime || article.createdAt"> | 创建时间: {{ article.createTime || article.createdAt }}</span> </span>
              <span><i class="fa fa-eye"></i> {{ article.viewCount || article.views || 0 }} 浏览量</span>
              <span><i class="fa fa-heart"></i> {{ article.loveCount || article.likes || 0 }} 点赞数</span>
            </p>
            <div class="article-actions">
              <button class="action-button small" @click="viewArticle(article.id)">
                <i class="fa fa-eye"></i> 查看
              </button>
              <button class="action-button small" @click="editArticle(article)">
                <i class="fa fa-edit"></i> 编辑
              </button>
              <button class="action-button small secondary" @click="deleteArticle(article.id)">
                <i class="fa fa-trash"></i> 删除
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <i class="fa fa-newspaper-o"></i>
        <p v-if="searchEmpty">当前没有查到相关文章</p>
        <p v-else>你还没有发布任何文章</p>
        <button class="action-button" @click="showCreateForm">开始创作</button>
      </div>
    </div>
    
    <div class="content-main" v-else-if="activeTab === 'pending'">
      <div class="section-header">
        <h2>待审核文章</h2>
      </div>
      <div class="search-container">
        <div class="search-group">
          <input type="text" class="search-input" placeholder="按标题搜索..." v-model="searchTitle">
          <button class="search-button" @click="searchByTitle">
            <i class="fa fa-search"></i> 标题搜索
          </button>
        </div>
        <div class="search-group">
          <input type="date" class="search-input" v-model="searchDate">
          <button class="search-button" @click="searchByDate">
            <i class="fa fa-calendar"></i> 时间搜索
          </button>
        </div>
        <div class="search-group">
          <select class="search-input" v-model="searchTag">
            <option value="">请选择标签...</option>
            <option v-for="tag in presetTags" :key="tag.id" :value="tag.name">{{ tag.name }}</option>
          </select>
          <button class="search-button" @click="searchByTag">
            <i class="fa fa-tags"></i> 标签搜索
          </button>
        </div>
      </div>
      <div class="drafts-list" v-if="pendingArticles.length > 0">
        <div 
          v-for="article in pendingArticles" 
          :key="article.id" 
          class="draft-item"
        >
          <div class="item-cover">
            <img v-if="article.coverImageUrl" :src="article.coverImageUrl" alt="封面" class="cover-image">
            <div v-else class="item-icon">
              <i class="fa fa-clock-o"></i>
            </div>
          </div>
          <div class="item-content">
            <h3>{{ article.title }}</h3>
            <p class="draft-meta">
              <span>
                更新时间: {{ article.updateTime || article.updatedAt || article.createTime || article.createdAt || article.publishTime || '未知' }}
                <span v-if="article.createTime || article.createdAt"> | 创建时间: {{ article.createTime || article.createdAt }}</span>
              </span>
              <span><i class="fa fa-clock-o"></i> 审核中</span>
            </p>
            <div class="draft-actions">
              <button class="action-button small" @click="viewArticle(article.id)">
                <i class="fa fa-eye"></i> 查看
              </button>
              <button class="action-button small secondary" @click="deleteArticle(article.id)">
                <i class="fa fa-trash"></i> 删除
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <i class="fa fa-clock-o"></i>
        <p v-if="searchEmpty">当前没有查到相关文章</p>
        <p v-else>你没有待审核的文章</p>
        <button class="action-button" @click="showCreateForm">开始创作</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import Editor from '@tinymce/tinymce-vue'

export default {
  name: 'CreateContentView',
  components: {
    Editor
  },
    
  data() {
    return {
      activeTab: 'data',
      isSaving: false,
      isPublishing: false,
      previewCoverImage: null,
      recentArticles: [], // 最近文章数据
      article: {
        id: null,
        title: '',
        content: '',
        summary: '',
        category: '',
        tags: [], // 现在存储{id, name}对象
        coverImageUrl: null,
        allowComments: true
      },

      newTag: '',
      presetTags: [
        { id: 1, name: 'java' },
        { id: 2, name: 'JavaScript' },
        { id: 3, name: 'c++' },
        { id: 4, name: 'c#' },
        { id: 5, name: 'python' },
        { id: 6, name: '后端开发' },
        { id: 7, name: 'ai' },
        { id: 8, name: '前端开发' },
        { id: 9, name: '数据库开发' },
        { id: 10, name: '爬虫' },
        { id: 11, name: 'Node.js' },
        { id: 12, name: 'html' },
        { id: 13, name: 'css' },
        { id: 14, name: '人工智能' }
      ],
      categories: [
        { id: 1, name: '前端开发' },
        { id: 2, name: '后端开发' },
        { id: 3, name: '移动开发' },
        { id: 4, name: '人工智能' },
        { id: 5, name: 'DevOps' },
        { id: 6, name: '数据库' },
        { id: 7, name: '开发工具' },
        { id: 8, name: '其他' }
      ],
      userStats: {
        articles: 0,
        drafts: 0,
        likes: 0
      },
      drafts: [],
      publishedArticles: [],
      pendingArticles: [],
      // 搜索相关变量
      searchTitle: '',
      searchDate: '',
      searchTag: '',
      // 搜索空状态标识
      searchEmpty: false
    }
  },
  computed: {
    canPublish() {
      return this.article.title.trim() && 
             this.article.content.trim() && 
             this.article.category
    }
  },
  
  watch: {
    // 监听路由变化，当路径或查询参数改变时更新activeTab并重新获取对应的数据
    '$route': function(route) {
      // 更新activeTab
      const path = route.path
      const tabQuery = route.query.tab
      
      if (path.includes('/drafts')) {
        this.activeTab = 'drafts'
      } else if (path.includes('/published')) {
        this.activeTab = 'published'
      } else if (path.includes('/pending')) {
        this.activeTab = 'pending'
      } else if (tabQuery) {
        // 如果有tab查询参数，优先使用
        this.activeTab = tabQuery
      } else {
        this.activeTab = 'create'
      }
      
      // 根据当前activeTab重新获取数据
      this.fetchUserStats()
      this.fetchDrafts()
      this.fetchPublishedArticles()
      this.fetchPendingArticles()
    }
  },
  mounted() {
    // 设置网页标题为萌技术社区 - 创作中心
    document.title = '萌技术社区 - 创作中心'
    // 检查并设置认证token
    const token = localStorage.getItem('token') || sessionStorage.getItem('token')
    const isLoggedIn = localStorage.getItem('isLoggedIn') || sessionStorage.getItem('isLoggedIn')
    
    // 如果未登录，跳转到登录页面
    if (!isLoggedIn || !token) {
      alert('请先登录后再访问创作中心')
      this.$router.push('/login')
      return
    }
    
    // 设置请求头
    if (token) {
      // 移除可能存在的'Bearer '前缀（如果后端已包含）
      const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
      axios.defaults.headers.common['Authorization'] = `Bearer ${cleanToken}`
      console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`)
    }
    
    // 根据当前路径确定要显示的tab
      const path = this.$route.path
      const tabQuery = this.$route.query.tab
      
      if (path.includes('/drafts')) {
        this.activeTab = 'drafts'
      } else if (path.includes('/published')) {
        this.activeTab = 'published'
      } else if (path.includes('/pending')) {
        this.activeTab = 'pending'
      } else if (tabQuery) {
        // 如果有tab查询参数，优先使用
        this.activeTab = tabQuery
      } else {
        this.activeTab = 'create'
      }
    
    this.fetchUserStats()
    this.fetchDrafts()
    this.fetchPublishedArticles()
    this.fetchPendingArticles()
    this.fetchRecentArticles() // 获取最近文章数据
  },
  methods: {
    // 切换标签页 - 使用路由跳转
    showDataOverview() {
      this.$router.push('/articles/create?tab=data')
    },
    
    showCreateForm() {
      this.$router.push('/articles/create?tab=create')
    },
    
    showDrafts() {
      this.$router.push('/articles/create/drafts')
    },
    
    showPublished() {
      this.$router.push('/articles/create/published')
    },
    
    showPendingArticles() {
      this.$router.push('/articles/create/pending')
    },
    
    // 搜索相关方法
    async searchByTitle() {
      // 根据当前激活的标签页搜索相应类型的文章
      console.log(`按标题搜索${this.getTabName()}文章:`, this.searchTitle);
      
      // 移除空搜索验证，支持搜索框为空时查询全部文章
      try {
        // 根据当前激活的标签页确定状态码
        // 1: 未发布(草稿), 2: 待审核, 3: 已发布
        let status = 1;
        if (this.activeTab === 'pending') {
          status = 2;
        } else if (this.activeTab === 'published') {
          status = 3;
        }
        
        // 调用后端接口，根据不同标签页调用对应的搜索功能
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const response = await axios.get('http://localhost:8081/api/create-center/by-title', {
          params: {
            title: this.searchTitle,
            status: status
          },
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        
        console.log('按标题搜索响应:', response.data);
        
        // 根据当前激活的标签页更新对应的文章列表
        const searchResult = response.data.data || response.data || [];
        
        if (this.activeTab === 'drafts') {
          this.drafts = searchResult;
        } else if (this.activeTab === 'published') {
          this.publishedArticles = searchResult;
        } else if (this.activeTab === 'pending') {
          this.pendingArticles = searchResult;
        }
        
            // 设置搜索空状态标识
        this.searchEmpty = !searchResult || searchResult.length === 0;
        console.log('搜索空状态:', this.searchEmpty, '当前标签页:', this.activeTab);
        
      } catch (error) {
        console.error('按标题搜索失败:', error);
        alert('搜索失败，请重试');
      }
    },
    
    searchByDate() {
      // 根据当前激活的标签页搜索相应类型的文章
      console.log(`按时间搜索${this.getTabName()}文章:`, this.searchDate);
      
      // 根据activeTab确定status参数
      let status = 1; // 默认为已发布
      if (this.activeTab === 'drafts') {
        status = 0; // 草稿
      } else if (this.activeTab === 'pending') {
        status = 2; // 待审核
      }
      
      try {
        // 调用后端接口
        axios.get('http://localhost:8081/api/create-center/by-date', {
          params: {
            date: this.searchDate,
            status: status
          },
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token') || sessionStorage.getItem('token')}`
          }
        }).then(response => {
          console.log('按时间搜索响应:', response.data);
          
          // 正确处理响应数据，与searchByTitle方法保持一致
          const searchResult = response.data.data || response.data || [];
          
          // 根据当前标签页更新对应的文章列表
          if (this.activeTab === 'drafts') {
            this.drafts = searchResult;
          } else if (this.activeTab === 'published') {
            this.publishedArticles = searchResult;
          } else if (this.activeTab === 'pending') {
            this.pendingArticles = searchResult;
          }
          
          // 设置搜索空状态标识
          this.searchEmpty = !searchResult || searchResult.length === 0;
          console.log('搜索空状态:', this.searchEmpty, '当前标签页:', this.activeTab);
        }).catch(error => {
          console.error('按时间搜索请求失败:', error);
          alert('搜索失败，请重试');
        });
      } catch (error) {
        console.error('按时间搜索失败:', error);
        alert('搜索失败，请重试');
      }
    },
    
    searchByTag() {
      // 根据当前激活的标签页搜索相应类型的文章
      console.log(`按标签搜索${this.getTabName()}文章:`, this.searchTag);
      // 这里可以调用后端接口，例如：
      // if (this.activeTab === 'drafts') {
      //   this.fetchDraftsByTag(this.searchTag);
      // } else if (this.activeTab === 'published') {
      //   this.fetchPublishedArticlesByTag(this.searchTag);
      // } else if (this.activeTab === 'pending') {
      //   this.fetchPendingArticlesByTag(this.searchTag);
      // }
    },
    
    // 获取当前标签页的中文名称
    getTabName() {
      switch (this.activeTab) {
        case 'drafts': return '草稿';
        case 'published': return '已发布';
        case 'pending': return '待审核';
        default: return '';
      }
    },
    
    // 标签管理 - 已禁用手动输入，只能从预设标签选择
    addTag() {
      // 功能已禁用
    },
    
    removeTag(index) {
      this.article.tags.splice(index, 1)
    },
    
    // 切换预设标签
    togglePresetTag(tagId, tagName) {
      const index = this.article.tags.findIndex(tag => tag.id === tagId)
      if (index > -1) {
        // 如果已存在，则移除
        this.article.tags.splice(index, 1)
      } else if (this.article.tags.length < 5) {
        // 如果不存在且未达到上限，则添加
        this.article.tags.push({ id: tagId, name: tagName })
      }
    },
    

    
    // 封面图片处理
    handleCoverUpload(e) {
      console.log('handleCoverUpload被调用，事件:', e)
      const file = e.target.files[0]
      console.log('选择的文件:', file)
      if (file) {
        // 存储文件对象用于后续上传
        this.article.coverImageUrl = file
        console.log('已将文件对象存储到this.article.coverImageUrl:', this.article.coverImageUrl)
        // 生成预览
        const reader = new FileReader()
        reader.onload = (event) => {
          this.previewCoverImage = event.target.result
          console.log('已生成预览图片，previewCoverImage:', this.previewCoverImage)
        }
        reader.readAsDataURL(file)
      }
    },
    
    removeCover() {
      this.article.coverImageUrl = null
      this.previewCoverImage = null
      document.getElementById('article-cover').value = ''
    },
    
    // 上传图片到MinIO
    async uploadImageToMinIO(file) {
      try {
        console.log('开始上传图片到MinIO，文件:', file)
        const formData = new FormData()
        formData.append('file', file)
        
        console.log('FormData创建成功，准备发送请求到:', 'http://localhost:8081/api/create-center/upload/image')
        // 调用上传图片API
        const response = await axios.post('http://localhost:8081/api/create-center/upload/image', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        // 根据后端返回的数据结构调整
    // 如果Result对象包含data字段
    let imageUrl = response.data.data || response.data
    
    console.log('图片上传成功，返回的URL:', imageUrl)
    
    // 清理URL中可能存在的不正确后缀/update-images
    if (imageUrl && imageUrl.endsWith('/update-images')) {
      imageUrl = imageUrl.replace('/update-images', '')
      console.log('清理后的图片URL:', imageUrl)
    }
    return imageUrl
  } catch (error) {
    console.error('上传图片到MinIO失败:', error)
    throw error
  }
    },
    
    // 保存草稿
    async saveDraft() {
      this.isSaving = true
      try {
        // 准备草稿数据，适配ArticleDTO格式
        const draftData = {
          title: this.article.title,
          categoryId: this.article.category ? parseInt(this.article.category) : null,
          label: this.article.tags.map(tag => tag.id),
          content: this.article.content,
          summary: this.article.summary,
          isCommented: this.article.allowComments ? 1 : 0
        }
        
        // 如果有封面图片，上传到MinIO
        console.log('检查是否需要上传封面图片:', this.article.coverImageUrl, '是否为File对象:', this.article.coverImageUrl instanceof File)
        if (this.article.coverImageUrl && this.article.coverImageUrl instanceof File) {
          console.log('开始上传封面图片到MinIO')
          draftData.coverImageUrl = await this.uploadImageToMinIO(this.article.coverImageUrl)
          console.log('封面图片上传完成，URL:', draftData.coverImageUrl)
        } else {
          draftData.coverImageUrl = this.article.coverImageUrl // 可能是已有的URL或null
          console.log('使用已有的封面图片URL或null:', draftData.coverImageUrl)
        }
        
        // 发送请求前检查完整的草稿数据
        console.log('准备发送到后端的草稿完整数据:', JSON.stringify(draftData, null, 2))
        // 调用保存草稿API（端口8081）
        const response = await axios.post('http://localhost:8081/api/create-center/drafts', draftData)
        console.log('保存草稿响应:', response.data)
        
        alert('草稿保存成功!')
        this.fetchDrafts()
      } catch (error) {
        console.error('保存草稿失败:', error)
        alert('保存草稿失败，请重试')
      } finally {
        this.isSaving = false
      }
    },
    
    // 发布文章
    async publishArticle() {
  if (!this.canPublish) {
    alert('请填写标题、内容并选择分类')
    return
  }
  
  this.isPublishing = true
  try {
    // 准备文章数据，适配ArticleDTO格式
    const articleData = {
      id: this.article.id ? Number(this.article.id) : null, // 添加id字段，转换为数字类型
      title: this.article.title,
      categoryId: this.article.category ? Number(this.article.category) : null, // 只有当category有值时才转换为Number
      label: this.article.tags.map(tag => tag.id),
      content: this.article.content,
      summary: this.article.summary,
      isCommented: this.article.allowComments ? 1 : 0
    }
    
    // 处理封面图片
    if (this.article.coverImageUrl && this.article.coverImageUrl instanceof File) {
      console.log('开始上传封面图片到MinIO')
      articleData.coverImageUrl = await this.uploadImageToMinIO(this.article.coverImageUrl)
      console.log('封面图片上传完成，URL:', articleData.coverImageUrl)
    } else if (this.article.coverImageUrl) {
      // 直接使用已有的URL
      articleData.coverImageUrl = this.article.coverImageUrl
      console.log('使用已有的封面图片URL:', articleData.coverImageUrl)
    } else {
      // 没有封面图片
      articleData.coverImageUrl = null
    }
    
    // 发送请求前检查完整的文章数据
    console.log('准备发送到后端的文章完整数据:', JSON.stringify(articleData, null, 2))
    
    const response = await axios.post('http://localhost:8081/api/create-center', articleData)
    console.log('发布文章响应:', response.data)
    
    alert('文章已提交审核，审核通过后将自动发布!')
    // 重置表单
    this.resetForm()
    this.fetchPendingArticles()
    this.fetchUserStats()
  } catch (error) {
    console.error('发布文章失败:', error)
    alert('发布文章失败，请重试')
  } finally {
    this.isPublishing = false
  }
},
    
    // 重置表单
    resetForm() {
      this.article = {
        title: '',
        content: '',
        summary: '',
        category: '',
        tags: [],
        coverImageUrl: null,
        allowComments: true
      }
      this.previewCoverImage = null
      this.newTag = ''
    },
    
    // 获取用户统计数据
    async fetchUserStats() {
      try {
        // 使用已获取的真实文章数据计算数量
        this.userStats = {
          articles: this.publishedArticles.length,
          drafts: this.drafts.length,
          likes: 0 // 点赞数设置为0
        }
      } catch (error) {
        console.error('获取统计数据失败:', error)
      }
    },
    
    // 获取草稿列表
    async fetchDrafts() {
      try {
        // 调用实际API获取草稿列表
        const response = await axios.get('http://localhost:8081/api/create-center/drafts')
        console.log('获取草稿列表响应:', response.data)
        // 根据后端返回的数据结构调整
        this.drafts = response.data.data || response.data || []
        // 重置搜索空状态
        this.searchEmpty = false
        console.log('草稿列表更新，搜索空状态重置为:', this.searchEmpty)
        // 更新统计数据
        this.fetchUserStats()
      } catch (error) {
        console.error('获取草稿失败:', error)
        alert('获取草稿失败，请重试')
      }
    },
    
    // 获取已发布文章
    async fetchPublishedArticles() {
      try {
        // 调用真实API获取已发布文章列表
        const response = await axios.get('http://localhost:8081/api/create-center/published')
        console.log('获取已发布文章响应:', response.data)
        // 根据后端返回的数据结构调整
        this.publishedArticles = response.data.data || response.data || []
        // 重置搜索空状态
        this.searchEmpty = false
        console.log('已发布文章列表更新，搜索空状态重置为:', this.searchEmpty)
        // 更新统计数据
        this.fetchUserStats()
      } catch (error) {
        console.error('获取已发布文章失败:', error)
      }
    },
    
    // 获取待审核文章列表
    async fetchPendingArticles() {
      try {
        // 调用真实API获取待审核文章列表
        const response = await axios.get('http://localhost:8081/api/create-center/pending')
        console.log('获取待审核文章响应:', response.data)
        // 根据后端返回的数据结构调整
        this.pendingArticles = response.data.data || response.data || []
        // 重置搜索空状态
        this.searchEmpty = false
        console.log('待审核文章列表更新，搜索空状态重置为:', this.searchEmpty)
      } catch (error) {
        console.error('获取待审核文章失败:', error)
      }
    },
    
    // 获取最近文章数据
    async fetchRecentArticles() {
      try {
        // 调用API获取最近文章数据
        const response = await axios.get('http://localhost:8081/api/create-center/recent')
        console.log('获取最近文章数据响应:', response.data)
        // 根据后端返回的数据结构调整
        this.recentArticles = response.data.data || response.data || []
      } catch (error) {
        console.error('获取最近文章数据失败:', error)
        // 使用模拟数据
        this.recentArticles = [
          {
            id: 1,
            title: '时间的镜像：《葬送的芙莉莲》与迟来的理解',
            createTime: '2023-12-23 14:30',
            reads: 150,
            likes: 25,
            comments: 8
          },
          {
            id: 2,
            title: '无限的乌托邦:为何现在需要轻少女',
            createTime: '2023-12-22 09:15',
            reads: 95,
            likes: 12,
            comments: 3
          },
          {
            id: 3,
            title: '现代前端开发的最佳实践',
            createTime: '2023-12-20 16:45',
            reads: 230,
            likes: 42,
            comments: 15
          }
        ]
      }
    },
    
    // 编辑草稿
    async editDraft(draft) {
      try {
        // 调用后端接口获取草稿详情（使用PUT请求）
        const response = await axios.put(`http://localhost:8081/api/create-center/drafts/${draft.id}`)
        console.log('获取草稿详情响应:', response.data)
        
        // 获取返回的草稿详情数据
        const draftDetail = response.data.data || response.data
        
        // 设置基本信息
        this.article.id = draftDetail.id ? Number(draftDetail.id) : null
        this.article.title = draftDetail.title || ''
        this.article.category = draftDetail.categoryId || ''
        this.article.content = draftDetail.content || ''
        this.article.summary = draftDetail.summary || ''
        
        // 设置默认评论状态
        this.article.allowComments = draftDetail.allowComments !== undefined ? draftDetail.allowComments : true
        
        // 标签处理 - 根据返回的标签ID数组从presetTags中查找对应的标签对象
        if (draftDetail.label && Array.isArray(draftDetail.label)) {
          this.article.tags = draftDetail.label
            .map(tagId => {
              // 从预设标签中查找对应的标签对象，确保tagId转换为数字类型
              return this.presetTags.find(tag => tag.id === Number(tagId));
            })
            .filter(tag => tag !== undefined); // 过滤掉未找到的标签
        } else {
          this.article.tags = [];
        }
        
        // 处理封面图片
        if (draftDetail.coverImageUrl) {
          // 设置封面图片URL
          this.article.coverImageUrl = draftDetail.coverImageUrl
          // 设置预览图片
          this.previewCoverImage = draftDetail.coverImageUrl
          console.log('已加载草稿封面图片:', this.previewCoverImage)
        } else {
          this.article.coverImageUrl = null
          this.previewCoverImage = null
        }
        
        this.activeTab = 'create'
      } catch (error) {
        console.error('获取草稿详情失败:', error)
        alert('获取草稿详情失败，请重试')
      }
    },
    
    // 编辑已发布文章
    async editArticle(article) {
      try {
        // 调用后端接口获取文章详情（使用PUT请求，与编辑草稿使用相同的接口）
        const response = await axios.put(`http://localhost:8081/api/create-center/drafts/${article.id}`)
        console.log('获取文章详情响应:', response.data)
        
        // 获取返回的文章详情数据
        const articleDetail = response.data.data || response.data
        
        // 设置基本信息
        this.article.id = articleDetail.id ? Number(articleDetail.id) : null
        this.article.title = articleDetail.title || ''
        this.article.category = articleDetail.categoryId || ''
        this.article.content = articleDetail.content || ''
        this.article.summary = articleDetail.summary || ''
        
        // 设置默认评论状态
        this.article.allowComments = articleDetail.allowComments !== undefined ? articleDetail.allowComments : true
        
        // 标签处理 - 根据返回的标签ID数组从presetTags中查找对应的标签对象
        if (articleDetail.label && Array.isArray(articleDetail.label)) {
          this.article.tags = articleDetail.label
            .map(tagId => {
              // 从预设标签中查找对应的标签对象，确保tagId转换为数字类型
              return this.presetTags.find(tag => tag.id === Number(tagId));
            })
            .filter(tag => tag !== undefined); // 过滤掉未找到的标签
        } else {
          this.article.tags = [];
        }
        
        // 处理封面图片
        if (articleDetail.coverImageUrl) {
          // 设置封面图片URL
          this.article.coverImageUrl = articleDetail.coverImageUrl
          // 设置预览图片
          this.previewCoverImage = articleDetail.coverImageUrl
          console.log('已加载文章封面图片:', this.previewCoverImage)
        } else {
          this.article.coverImageUrl = null
          this.previewCoverImage = null
        }
        
        this.activeTab = 'create'
      } catch (error) {
        console.error('获取文章详情失败:', error)
        alert('获取文章详情失败，请重试')
      }
    },
    
    // 删除草稿
    async deleteDraft(draftId) {
      if (confirm('确定要删除这个草稿吗？')) {
        try {
          // 调用后端删除草稿接口
          const response = await axios.delete(`http://localhost:8081/api/create-center/drafts/${draftId}`)
          
          console.log('删除草稿响应:', response.data)
          
          // 更新本地草稿列表
          this.drafts = this.drafts.filter(draft => draft.id !== draftId)
          
          // 刷新用户统计数据
          this.fetchUserStats()
          
          alert('草稿删除成功！')
        } catch (error) {
          console.error('删除草稿失败:', error)
          alert('删除失败，请重试')
        }
      }
    },
    
    // 删除已发布文章
    async deleteArticle(articleId) {
      if (confirm('确定要删除这篇文章吗？删除后不可恢复。')) {
        try {
          // 获取token
          const token = localStorage.getItem('token') || sessionStorage.getItem('token')
          // 准备请求头
          const headers = {}
          
          // 如果有token，添加到请求头
          if (token && typeof token === 'string') {
            const cleanToken = token.trim()
            headers['Authorization'] = `Bearer ${cleanToken}`
          }
          
          // 调用后端删除已发布文章接口，并保存响应
          const response = await axios.delete(`http://localhost:8081/api/create-center/${articleId}`, { headers })
          
          console.log('删除文章响应:', response.data)

          // 更新本地文章列表
          this.publishedArticles = this.publishedArticles.filter(article => article.id !== articleId)
          // 刷新用户统计数据
          this.fetchUserStats()
          
          alert('文章删除成功！')
        } catch (error) {
          console.error('删除文章失败:', error)
          alert('删除失败，请重试')
        }
      }
    },
    
    // 查看文章
    viewArticle(articleId) {
      // 跳转到作者专用文章详情页
      this.$router.push(`/article/author/${articleId}`)
    }
  }
}
</script>

<style scoped>
.preset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.preset-tag {
  padding: 8px 16px;
  background-color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 1);
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  color: #000000 !important;
  font-weight: 600 !important;
  opacity: 1 !important;
  display: inline-block !important;
  min-width: 60px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.preset-tag:hover {
  background-color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 1);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.preset-tag.selected {
  background-color: var(--accent-color);
  color: var(--bg-primary);
  border-color: var(--accent-color);
  box-shadow: 0 2px 8px rgba(242, 163, 58, 0.4);
}

.preset-tag.selected:hover {
  background-color: #e69a2e;
  transform: translateY(-1px);
}
</style>

<style scoped>
.create-content-container {
  display: flex;
  min-height: 100vh;
  background-color: var(--bg-primary);
  font-family: 'Inter', sans-serif;
}

/* 侧边栏样式 */
.content-sidebar {
  width: 260px;
  background-color: var(--card-bg);
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
  padding: 20px;
  position: fixed;
  height: 100vh;
  overflow-y: auto;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header {
  margin-bottom: 30px;
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h3 {
  color: var(--text-primary);
  margin-bottom: 8px;
  font-size: 20px;
}

.sidebar-header p {
  color: var(--text-secondary);
  font-size: 14px;
}

.sidebar-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 30px;
  padding: 20px;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.stat-item {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: var(--accent-color);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background-color: rgba(255, 255, 255, 0.15);
  color: #FFFFFF !important;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

.nav-button:first-child {
  background-color: rgba(255, 255, 255, 0.15);
  color: #FFFFFF !important;
  border-left: 3px solid var(--accent-color);
}

.nav-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

.nav-button.active {
  background-color: rgba(255, 255, 255, 0.15);
  color: #FFFFFF !important;
  border-left: 3px solid var(--accent-color);
}

.nav-button i {
  width: 20px;
  text-align: center;
}

/* 主内容区域样式 */
.content-main {
  flex: 1;
  margin-left: 260px;
  padding: 30px;
}

/* 数据总览样式 */
.data-overview {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-lg);
}

.overview-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
}

.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card-content h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.card-value {
  margin: var(--spacing-xs) 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--accent-primary);
}

.card-trend {
  margin: 0;
  font-size: 12px;
  color: var(--text-tertiary);
}

.trend-up {
  color: var(--accent-success);
}

.trend-down {
  color: var(--accent-error);
}

.card-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.articles-icon {
  background-color: rgba(255, 152, 0, 0.1);
  color: #FF9800;
}

.reads-icon {
  background-color: rgba(33, 150, 243, 0.1);
  color: #2196F3;
}

.likes-icon {
  background-color: rgba(76, 175, 80, 0.1);
  color: #4CAF50;
}

.comments-icon {
  background-color: rgba(156, 39, 176, 0.1);
  color: #9C27B0;
}

.data-charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--spacing-lg);
}

.chart-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.chart-card h3 {
  margin: 0 0 var(--spacing-lg) 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-placeholder {
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: var(--border-radius);
}

.chart-placeholder i {
  font-size: 48px;
  margin-bottom: var(--spacing-md);
}

.recent-articles-data {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.recent-articles-data h3 {
  margin: 0 0 var(--spacing-lg) 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.articles-data-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.article-data-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md);
  border-radius: var(--border-radius);
  background-color: rgba(0, 0, 0, 0.1);
  transition: all var(--transition-normal);
}

.article-data-item:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

.article-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 400px;
}

.article-meta {
  margin: var(--spacing-xs) 0 0 0;
  font-size: 12px;
  color: var(--text-tertiary);
}

.article-stats {
  display: flex;
  gap: var(--spacing-lg);
}

.article-stats .stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
  color: var(--text-secondary);
}

.article-stats .stat-item i {
  color: var(--accent-primary);
}

.editor-header, .section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 15px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  border-bottom: none;
}

.editor-header h2, .section-header h2 {
  color: #FFFFFF;
  font-size: 24px;
  margin: 0;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-button i {
  font-size: 14px;
}

.action-button.primary {
  background-color: var(--accent-color);
  color: var(--bg-primary);
}

.action-button.primary:hover:not(:disabled) {
  background-color: rgba(242, 163, 58, 0.9);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(242, 163, 58, 0.3);
}

.action-button.secondary {
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-secondary);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.action-button.secondary:hover:not(:disabled) {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

.action-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.action-button.small {
  padding: 6px 12px;
  font-size: 12px;
}

/* 表单样式 */
.editor-form {
  background-color: var(--card-bg);
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-primary);
  font-size: 14px;
}

.title-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}

.title-input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px rgba(242, 163, 58, 0.2);
  background-color: rgba(255, 255, 255, 0.1);
}

.category-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  font-size: 14px;
  background-color: rgba(255, 255, 255, 0.05);
  color: #FFFFFF; /* 改为白色以适应深色背景 */
  cursor: pointer;
  transition: border-color 0.3s ease;
}

.category-select option {
  background-color: var(--bg-secondary);
  color: #FFFFFF;
}

.category-select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px rgba(242, 163, 58, 0.2);
  background-color: rgba(255, 255, 255, 0.1);
}

/* 标签样式 */
.tags-input-container {
  position: relative;
}

.tags-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s ease;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}

.tags-input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px rgba(242, 163, 58, 0.2);
  background-color: rgba(255, 255, 255, 0.1);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: rgba(242, 163, 58, 0.1);
  color: #000000; /* 修改为黑色 */
  border-radius: 16px;
  font-size: 12px;
  border: 1px solid rgba(242, 163, 58, 0.3);
}

.tag-remove {
  background: none;
  border: none;
  color: var(--accent-color);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  padding: 0;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s ease;
}

.tag-remove:hover {
  background-color: var(--accent-color);
  color: var(--bg-primary);
}

/* Quill编辑器已自带工具栏，无需额外的工具栏样式 */

.content-editor {
  width: 100%;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-bottom-left-radius: 6px;
  border-bottom-right-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  font-family: 'Courier New', Courier, monospace;
  min-height: 300px;
  transition: border-color 0.3s ease;
  background-color: var(--card-bg);
  color: var(--text-primary);
}

.content-editor:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(242, 163, 58, 0.2);
}

.summary-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  transition: border-color 0.3s ease;
}

.summary-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.char-count {
  text-align: right;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 封面上传样式 */
.cover-upload {
  position: relative;
}

.cover-input {
  display: none;
}

.cover-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 30px;
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-secondary);
}

.cover-label:hover {
  border-color: var(--accent-color);
  background-color: rgba(242, 163, 58, 0.05);
  color: var(--accent-color);
}

.cover-label i {
  font-size: 32px;
}

.cover-preview {
  position: relative;
  margin-top: 16px;
  max-width: 300px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.cover-preview img {
  width: 100%;
  height: auto;
  display: block;
}

.remove-cover {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.remove-cover:hover {
  background-color: var(--accent-color);
  color: var(--bg-primary);
  border-color: var(--accent-color);
}

.publish-settings-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: normal;
  color: #666;
}

.publish-settings-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #3b82f6;
}

/* 草稿和已发布文章列表样式 */
.drafts-list, .published-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.draft-item, .published-item {
  background-color: var(--card-bg);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.draft-item:hover, .published-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.item-cover {
  width: 80px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 16px;
  background-color: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.item-content {
  flex: 1;
}

.draft-item h3, .published-item h3 {
  margin: 0 0 12px 0;
  color: var(--text-primary);
  font-size: 18px;
}

.draft-meta, .article-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.article-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.draft-actions, .article-actions {
  display: flex;
  gap: 8px;
}

/* 搜索区域样式 */
.search-container {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  flex-wrap: wrap;
}

.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
  font-size: 14px;
  width: 200px;
}

.search-input::placeholder {
  color: var(--text-secondary);
}

.search-input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 下拉选择框样式优化 */
.search-input[type="select"],
.search-input[role="select"],
select.search-input {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='white' d='M6 9l-4-4h8l-4 4z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 30px;
  cursor: pointer;
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.search-input[type="select"]:focus,
.search-input[role="select"]:focus,
select.search-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 下拉选项样式 */
select.search-input option {
  background-color: rgba(30, 30, 30, 0.9);
  color: #ffffff;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.5;
  border: none;
}

select.search-input option:checked {
  background-color: #409eff;
  color: #ffffff;
  font-weight: 500;
}

select.search-input option:hover {
  background-color: rgba(64, 158, 255, 0.8);
  color: #ffffff;
}

/* 移除了特殊标签的样式，所有标签现在使用统一的样式 */

.search-button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: background-color 0.2s ease;
}

.search-button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.search-button i {
  font-size: 12px;
}

/* 空状态样式 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.empty-state i {
  font-size: 48px;
  color: rgba(255, 255, 255, 0.3);
  margin-bottom: 16px;
}

.empty-state p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .create-content-container {
    flex-direction: column;
  }
  
  .content-sidebar {
    width: 100%;
    position: static;
    height: auto;
    padding: 16px;
    box-shadow: none;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }
  
  .content-main {
    margin-left: 0;
    padding: 16px;
  }
  
  .sidebar-stats {
    margin-bottom: 20px;
  }
  
  .editor-header, .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .editor-form {
    padding: 20px;
  }
  
  .content-editor {
    min-height: 200px;
  }
  
  .cover-preview {
    max-width: 100%;
  }
  
  .draft-meta, .article-meta {
    flex-wrap: wrap;
  }
}
</style>