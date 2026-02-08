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

        <!-- 用户资料模块 -->
        <div class="sidebar-card user-profile-card">
          <div v-if="article && article.userProfileVO" class="user-profile">
            <div class="avatar-container">
              <div class="avatar">
                <img :src="article.userProfileVO.icon" :alt="article.userProfileVO.username || '作者头像'" class="profile-avatar" @error="handleAvatarError">
                <div class="avatar-border"></div>
              </div>
            </div>
            
            <div class="user-details">
              <h1 class="username">{{ article.userProfileVO.username || article.author }} <span class="user-icon">☀️</span></h1>
              <div class="user-meta">
                <span class="level-badge">Lv.{{ article.userProfileVO.level || 1 }}</span>
                <span class="joined-date">{{ formatDate(article.userProfileVO.joinTime || article.userProfileVO.createdAt) }}</span>
              </div>
              <p class="bio" v-if="article.userProfileVO.description">{{ article.userProfileVO.description }}</p>
              
              <!-- 统计信息 -->
              <div class="stats-row">
                <div class="stat-item">
                  <span class="stat-number">{{ article.userProfileVO.fan || 0 }}</span>
                  <span class="stat-label">粉丝</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-number">{{ article.userProfileVO.following || 0 }}</span>
                  <span class="stat-label">关注</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-number">{{ article.userProfileVO.love || 0 }}</span>
                  <span class="stat-label">获赞</span>
                </div>
              </div>
              
              <div class="action-buttons">
                <button class="action-btn follow-btn" :class="{ 'followed': article.userProfileVO.isFollow }" @click.stop="handleFollow">
                  <i class="fa" :class="article.userProfileVO.isFollow ? 'fa-user-check' : 'fa-user-plus'"></i> {{ article.userProfileVO.isFollow ? '已关注' : '关注' }}
                </button>
              </div>
            </div>
          </div>
          <div v-else class="no-profile">
            <p>暂无作者信息</p>
          </div>
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
        <template v-else-if="article">
          <div class="article-content">
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
                  <span>{{ article.viewCount || 0 }} 浏览量</span>
                </div>
                <div class="stat-item">
                  <i class="fa fa-thumbs-up" :class="{ 'text-red-500': isLiked }"></i>
                  <span>{{ article.likeCount !== undefined ? article.likeCount : 0 }} 点赞</span>
                </div>
                <div class="stat-item">
                  <i class="fa fa-comment"></i>
                  <span>{{ article.commentCount !== undefined ? article.commentCount : 0 }} 评论</span>
                </div>
              </div>
              <div class="action-buttons">
                <button class="action-btn like-btn" @click="handleLike" :class="{ 'liked': isLiked }">
                  <i class="fa fa-thumbs-up" :class="{ 'text-red-500': isLiked }"></i> {{ isLiked ? '已点赞' : '点赞' }}
                </button>
                <button class="action-btn collect-btn">
                  <i class="fa fa-star"></i> 收藏
                </button>
              </div>
            </div>

            <!-- 评论模块 -->
            <div class="comments-section">
              <h2 class="comments-title">评论区</h2>
              
              <!-- 评论输入框 -->
              <div class="comment-input-section" v-if="checkLoginStatus()">
                <div class="comment-input-header">
                  <div class="user-avatar-small">
                    <img :src="currentUserAvatar || '/default-avatar.png'" alt="当前用户头像" class="avatar-img-small">
                  </div>
                  <div class="comment-input-content">
                    <textarea 
                      v-model="newCommentContent" 
                      placeholder="写下你的评论..." 
                      class="comment-textarea"
                      rows="3"
                    ></textarea>
                    <div class="comment-input-footer">
                      <span class="comment-word-count">{{ newCommentContent.length }}/{{ maxCommentLength }}</span>
                      <button 
                        class="submit-comment-btn"
                        @click="submitComment"
                        :disabled="!newCommentContent.trim() || submittingComment"
                      >
                        {{ submittingComment ? '发布中...' : '发布评论' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 未登录提示 -->
              <div class="login-prompt" v-else>
                <p>请先登录后再发表评论</p>
                <button class="login-btn">去登录</button>
              </div>
              
              <!-- 评论列表 -->
              <div class="comments-list">
                <div 
                  v-for="comment in comments" 
                  :key="comment.id" 
                  class="comment-item"
                >
                  <div class="comment-avatar">
                    <img :src="comment.userProfileVO?.icon || comment.icon || '/default-avatar.png'" alt="用户头像" class="avatar-img-small">
                  </div>
                  <div class="comment-content-wrapper">
                    <div class="comment-header">
                        <span class="comment-username">{{ comment.userProfileVO?.username || comment.username || '匿名用户' }}</span>
                        <span class="comment-time">{{ formatDate(comment.createTime || comment.createdAt) }}</span>
                      </div>
                    <div class="comment-body">
                      {{ comment.content }}
                    </div>
                    <div class="comment-actions">
                      <button 
                        class="comment-action-btn"
                        @click="toggleCommentLike(comment)"
                        :class="{ 'liked': comment.isLove }"
                      >
                        <i class="fa fa-thumbs-up" :class="{ 'text-red-500': comment.isLove }"></i> 
                        <span>{{ comment.love || 0 }}</span>
                      </button>
                      <button class="comment-action-btn" @click="toggleReply(comment)">
                        <i class="fa fa-reply"></i> 
                        <span>回复</span>
                      </button>
                      <button class="comment-action-btn" @click="toggleReplies(comment)" v-if="comment.replyList && comment.replyList.length > 0">
                        <i :class="['fa', expandedReplies.includes(comment.id) ? 'fa-chevron-up' : 'fa-chevron-down']"></i> 
                        <span>{{ expandedReplies.includes(comment.id) ? '收起回复' : '展开回复' }}</span>
                        <span class="reply-count">({{ comment.replyList.length }})</span>
                      </button>
                    </div>
                    
                    <!-- 回复输入框 -->
                    <div class="reply-input-section" v-if="showReplyInput === comment.id">
                      <div class="reply-input-content">
                        <textarea 
                          v-model="replyContent" 
                          placeholder="写下你的回复..." 
                          class="reply-textarea"
                          rows="2"
                        ></textarea>
                        <div class="reply-input-footer">
                          <button class="cancel-reply-btn" @click="showReplyInput = null">取消</button>
                          <button 
                            class="submit-reply-btn"
                            @click="submitReply(comment)"
                            :disabled="!replyContent.trim() || submittingReply"
                          >
                            {{ submittingReply ? '回复中...' : '回复' }}
                          </button>
                        </div>
                      </div>
                    </div>
                    
                    <!-- 回复列表 -->
                    <div class="replies-list" v-if="comment.replyList && comment.replyList.length > 0 && expandedReplies.includes(comment.id)">
                      <div 
                        v-for="reply in comment.replyList" 
                        :key="reply.id" 
                        class="reply-item"
                      >
                        <div class="reply-avatar">
                          <img :src="reply.userProfileVO?.icon || reply.icon || '/default-avatar.png'" alt="用户头像" class="avatar-img-small">
                        </div>
                        <div class="reply-content-wrapper">
                          <div class="reply-header">
                            <span class="reply-username">{{ reply.userProfileVO?.username || reply.username || '匿名用户' }}</span>
                            <span class="reply-time">{{ formatDate(reply.createTime || reply.createdAt) }}</span>
                          </div>
                          <div class="reply-body">
                            <span class="reply-to">@{{ reply.replyName || '用户' }}：</span>
                            {{ reply.content }}
                          </div>
                          <div class="reply-actions">
                            <button 
                              class="reply-action-btn"
                              @click="toggleReplyLike(reply)"
                              :class="{ 'liked': reply.isLove }"
                            >
                              <i class="fa fa-thumbs-up"></i> 
                              <span>{{ reply.love || 0 }}</span>
                            </button>
                            <button class="reply-action-btn" @click="toggleReplyTo(reply, comment)">
                              <i class="fa fa-reply"></i> 
                              <span>回复</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 无评论提示 -->
                <div class="no-comments" v-if="comments.length === 0">
                  <p>暂无评论，快来发表第一条评论吧！</p>
                </div>
              </div>
            </div>
          </div>
        </template>

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
import LoveService from '../services/loveService.js'

export default {
  name: 'ArticleDetailPublicView',
  data() {
    return {
      article: null,
      tags: [],
      loading: true, // 初始值设为true，避免显示"文章数据不存在"
      error: null,
      sections: [],
      activeSection: '',
      scrollTimer: null,
      isLiked: false, // 是否已点赞
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
      },
      // 评论相关状态
      comments: [], // 评论列表
      newCommentContent: '', // 新评论内容
      maxCommentLength: 500, // 最大评论长度
      submittingComment: false, // 提交评论的加载状态
      currentUserAvatar: '', // 当前用户头像
      currentUserId: null, // 当前用户ID
      currentUsername: '', // 当前用户名
      showReplyInput: null, // 显示回复输入框的评论ID
      replyContent: '', // 回复内容
      submittingReply: false, // 提交回复的加载状态
      expandedReplies: [], // 跟踪展开的回复列表
      replyingToReply: null // 跟踪正在回复的子评论
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
      this.loading = false;
      this.error = '文章ID不存在'
    }
    
    // 保存来源页面信息，如果是从搜索页面跳转过来的
    if (this.$route.query.from === 'search' && this.$route.query.q) {
      // 保存搜索关键词到sessionStorage
      sessionStorage.setItem('lastSearchQuery', this.$route.query.q);
      console.log('保存搜索关键词:', this.$route.query.q);
    }
    
    // 监听滚动事件，更新当前激活的导航项
    window.addEventListener('scroll', this.handleScroll)
    
    // 获取当前用户信息
    this.fetchCurrentUserInfo();
    
    // 将获取评论列表的调用移到fetchArticleDetail成功后，确保articleId可用
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
    
    // 获取当前用户信息（头像和用户名）
    async fetchCurrentUserInfo() {
      if (!this.checkLoginStatus()) return;
      
      try {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        const headers = {
          'Content-Type': 'application/json'
        };
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 调用获取当前用户信息的接口
        const response = await axios.get('http://localhost:8081/api/users/me', { headers });
        
        if (response.data && response.data.data) {
          const userInfo = response.data.data;
          // 保存当前用户的头像
          this.currentUserAvatar = userInfo.icon || '/default-avatar.png';
          // 保存当前用户的ID和用户名，用于评论时使用
          this.currentUserId = userInfo.userId;
          this.currentUsername = userInfo.username;
          console.log('当前用户信息:', { avatar: this.currentUserAvatar, username: this.currentUsername });
        } else if (response.data) {
          // 兼容不同的数据格式
          const userInfo = response.data;
          this.currentUserAvatar = userInfo.icon || '/default-avatar.png';
          this.currentUserId = userInfo.userId;
          this.currentUsername = userInfo.username;
          console.log('当前用户信息(兼容格式):', { avatar: this.currentUserAvatar, username: this.currentUsername });
        }
      } catch (error) {
        console.error('获取当前用户信息失败:', error);
        // 如果获取失败，使用默认头像
        this.currentUserAvatar = '/default-avatar.png';
      }
    },
    
    // 处理点赞/取消点赞操作
    async handleLike() {
      // 检查登录状态
      if (!this.checkLoginStatus()) {
        alert('请先登录后再进行点赞操作');
        return;
      }
      
      // 防止重复点击
      const likeButton = document.querySelector('.like-btn');
      if (likeButton) {
        likeButton.disabled = true;
      }
      
      try {
        // 准备点赞数据
        // 从路由参数直接获取articleId，确保id不为null
        const articleId = this.$route.params.id;
        const loveData = {
          id: articleId, // 直接使用路由参数中的文章ID作为实体id
          loveTypeId: 1 // 文章点赞类型
        };
        console.log('点赞请求数据:', loveData);
        
        // 使用toggleLove方法进行点赞/取消点赞操作
        await LoveService.toggleLove(loveData);
        
        // 根据当前状态更新UI和显示提示信息
        if (this.isLiked) {
          // 取消点赞
          this.isLiked = false;
          // 更新点赞数
          if (this.article.likeCount !== undefined) {
            this.article.likeCount = Math.max(0, this.article.likeCount - 1);
          } else {
            this.article.likeCount = 0;
          }
          // 显示取消点赞成功提示
          alert('取消点赞成功');
        } else {
          // 点赞
          this.isLiked = true;
          // 更新点赞数
          if (this.article.likeCount !== undefined) {
            this.article.likeCount += 1;
          } else {
            this.article.likeCount = 1;
          }
          // 显示点赞成功提示
          alert('点赞成功');
        }
        
      } catch (error) {
        console.error('点赞/取消点赞操作失败:', error);
      } finally {
        // 恢复按钮可用状态
        if (likeButton) {
          likeButton.disabled = false;
        }
      }
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
        console.log('请求URL:', `http://localhost:8081/api/articles/${articleId}`)
        console.log('请求头信息:', headers)
        
        // 调用后端接口获取文章详情
        const response = await axios.get(
            `http://localhost:8081/api/articles/${articleId}`,
            { headers }
          )
        
        console.log('获取文章详情响应状态:', response.status)
        console.log('获取文章详情响应:', response.data)
        
        // 根据接口返回的数据结构，提取文章数据和标签
        try {
          console.log('响应数据结构类型:', typeof response.data)
          console.log('响应数据完整内容:', JSON.stringify(response.data))
          
          // 初始化数据
          this.article = null
          this.tags = []
          
          // 检查响应数据是否存在且为对象
          if (response.data && typeof response.data === 'object') {
            console.log('响应数据键列表:', Object.keys(response.data))
            
            // 支持两种格式：直接在data中或在data.data中
            const resultData = response.data.data || response.data;
            
            // 适配ArticleDetailVO数据结构
            if (resultData.title !== undefined || resultData.content !== undefined) {
              // 直接使用响应数据作为article，无需转换键名
              this.article = resultData;
              console.log('成功提取文章数据:', this.article);
              
              // 处理标签数据
              if (resultData.label && Array.isArray(resultData.label)) {
                this.tags = resultData.label;
                console.log('找到标签数据:', this.tags);
              }
              
              // 设置点赞状态
              this.isLiked = resultData.isLoved || false;
              
              // 设置统计数据
              this.article.likeCount = resultData.loveCount !== undefined ? resultData.loveCount : 0;
              this.article.viewCount = resultData.viewCount !== undefined ? resultData.viewCount : 0;
              // 支持comments字段（ArticleDetailVO中定义）和commentCount字段
              this.article.commentCount = resultData.comments !== undefined ? resultData.comments : 
                                        (resultData.commentCount !== undefined ? resultData.commentCount : 0);
              // 支持其他可能的字段名格式
              if (resultData.view_count !== undefined) {
                this.article.viewCount = resultData.view_count;
              }
              if (resultData.comment_count !== undefined) {
                this.article.commentCount = resultData.comment_count;
              }
            } else {
              console.warn('未找到符合ArticleDetailVO结构的数据');
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
        
        // 处理完成后，获取评论列表
        this.fetchComments();
        
        // 分析文章内容，生成导航
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
      // 尝试从sessionStorage获取上一次的搜索关键词
      const lastSearchQuery = sessionStorage.getItem('lastSearchQuery');
      
      if (lastSearchQuery) {
        console.log('使用保存的搜索关键词返回:', lastSearchQuery);
        // 带关键词跳转到搜索页面
        this.$router.push({
          name: 'search',
          query: { q: lastSearchQuery }
        });
      } else {
        // 如果没有保存的搜索关键词，则使用默认的返回上一页
        console.log('没有保存的搜索关键词，使用默认返回');
        this.$router.back();
      }
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
    
    // 处理头像加载错误
    handleAvatarError(event) {
      console.warn('头像加载失败，使用默认头像:', event);
      event.target.src = '/default-avatar.png';
    },
    
    // 直接返回后端传递的日期字符串
    formatDate(dateString) {
      return dateString || '';
    },
    
    // 处理用户点赞
    async handleUserLike() {
      // 检查登录状态
      if (!this.checkLoginStatus()) {
        alert('请先登录后再进行点赞操作');
        return;
      }
      
      try {
        // 准备用户点赞数据
        const userLoveData = {
          id: this.article.userProfileVO.id,
          loveTypeId: 2 // 用户点赞类型
        };
        
        // 调用点赞API
        const result = await LoveService.like(userLoveData);
        
        // 更新用户获赞数
        if (this.article.userProfileVO.love !== undefined) {
          this.article.userProfileVO.love += 1;
        } else {
          this.article.userProfileVO.love = 1;
        }
        
        alert('点赞成功');
      } catch (error) {
        console.error('用户点赞操作失败:', error);
        alert('点赞失败，请稍后重试');
      }
    },
    
    // 处理关注/取消关注操作
    async handleFollow() {
      // 检查登录状态
      if (!this.checkLoginStatus()) {
        alert('请先登录后再进行关注操作');
        return;
      }
      
      try {
        const userId = this.article.userProfileVO.userId || this.article.userProfileVO.id;
        if (!userId) {
          alert('用户ID不存在，无法关注/取消关注');
          return;
        }
        
        // 获取token
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        // 准备请求头
        const headers = {
          'Content-Type': 'application/json'
        };
        
        // 如果有token，添加Authorization头部
        if (token) {
          // 移除可能存在的'Bearer '前缀
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 发送关注/取消关注请求到后端API（同一个接口）
        const response = await fetch(`http://localhost:8081/api/Interaction/follow/${userId}`, {
          method: 'POST',
          headers: headers,
          credentials: 'include' // 包含cookie信息
        });
        
        if (response.ok) {
          const data = await response.json();
          // 根据当前状态更新前端UI
          if (this.article.userProfileVO.isFollow) {
            // 取消关注
            if (this.article.userProfileVO.fan !== undefined) {
              this.article.userProfileVO.fan = Math.max(0, this.article.userProfileVO.fan - 1);
            } else {
              this.article.userProfileVO.fan = 0;
            }
            this.article.userProfileVO.isFollow = false;
            alert('取消关注成功');
          } else {
            // 关注
            if (this.article.userProfileVO.fan !== undefined) {
              this.article.userProfileVO.fan += 1;
            } else {
              this.article.userProfileVO.fan = 1;
            }
            this.article.userProfileVO.isFollow = true;
            alert('关注成功');
          }
        } else {
          const errorData = await response.json().catch(() => ({}));
          throw new Error(errorData.message || '操作失败');
        }
      } catch (error) {
        console.error('关注/取消关注操作失败:', error);
        alert('操作失败，请稍后重试');
      }
    },
    
    // 处理图片加载错误
    handleImageError(event) {
      console.warn('封面图片加载失败:', event);
      // 可以在这里设置默认图片或隐藏图片元素
      // event.target.src = '/default-cover.jpg'; // 如果有默认图片
    },
    
    // 获取评论列表
    async fetchComments() {
      console.log('开始获取评论列表');
      try {
        const articleId = this.$route.params.id;
        console.log('文章ID:', articleId);
        if (!articleId) {
          console.error('文章ID不存在，无法获取评论');
          return;
        }
        
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        const headers = {
          'Content-Type': 'application/json'
        };
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 获取评论列表
        console.log('调用评论接口:', `http://localhost:8081/api/comment/${articleId}`);
        const response = await axios.get(
          `http://localhost:8081/api/comment/${articleId}`,
          { headers }
        );
        
        console.log('评论接口响应:', response);
        console.log('响应数据类型:', typeof response.data);
        console.log('响应数据是否为数组:', Array.isArray(response.data));
        
        // 处理不同的数据格式
        let commentsData = null;
        if (response.data && Array.isArray(response.data)) {
          // 如果直接返回数组
          commentsData = response.data;
          console.log('情况1：直接返回数组');
        } else if (response.data && response.data.data) {
          // 如果返回的是 {data: [...]} 格式或分页格式 {data: {list: [...]}}
          if (Array.isArray(response.data.data)) {
            // 直接返回 {data: [...]} 格式
            commentsData = response.data.data;
            console.log('情况2.1：返回{data: [...]}格式');
          } else if (response.data.data.list && Array.isArray(response.data.data.list)) {
            // 返回分页格式 {data: {list: [...]}}
            commentsData = response.data.data.list;
            console.log('情况2.2：返回分页格式{data: {list: [...]}}');
          } else {
            // 其他 {data: ...} 格式
            commentsData = response.data.data;
            console.log('情况2.3：返回其他{data: ...}格式');
            console.log('data字段类型:', typeof response.data.data);
            console.log('data字段是否为数组:', Array.isArray(response.data.data));
          }
        } else {
          // 其他情况
          commentsData = response.data;
          console.log('情况3：其他格式');
        }
        
        console.log('处理后的commentsData:', commentsData);
        console.log('commentsData类型:', typeof commentsData);
        console.log('commentsData是否为数组:', Array.isArray(commentsData));
        
        if (commentsData && Array.isArray(commentsData)) {
          // 确保每个评论和回复都有必要的字段（适配后端新增的字段名）
          this.comments = commentsData.map(comment => ({
            ...comment,
            love: comment.love !== undefined ? comment.love : 0, // 确保点赞数字段存在
            isLove: comment.isLove || false, // 确保点赞状态字段存在
            replyList: (comment.replyList || []).map(reply => ({
              ...reply,
              love: reply.love !== undefined ? reply.love : 0, // 确保回复的点赞数字段存在
              isLove: reply.isLove || false // 确保回复的点赞状态字段存在
            }))
          }));
          console.log('获取评论成功:', this.comments);
          console.log('评论数量:', this.comments.length);
        } else {
          console.log('评论列表为空或格式不符合预期');
          this.comments = [];
        }
      } catch (error) {
        console.error('获取评论失败:', error);
        if (error.response) {
          console.error('响应状态:', error.response.status);
          console.error('响应数据:', error.response.data);
        } else if (error.request) {
          console.error('请求已发送但没有收到响应:', error.request);
        } else {
          console.error('请求配置错误:', error.message);
        }
        this.comments = [];
      }
    },
    
    // 为评论及其回复的用户获取完整的用户信息
    async fetchUserProfilesForComments() {
      const token = localStorage.getItem('token') || sessionStorage.getItem('token');
      
      const headers = {
        'Content-Type': 'application/json'
      };
      
      if (token) {
        const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
        headers['Authorization'] = `Bearer ${cleanToken}`;
      }
      
      // 收集所有需要获取用户信息的用户ID
      const userIds = new Set();
      
      // 收集评论用户ID
      this.comments.forEach(comment => {
        if (comment.userId) {
          userIds.add(comment.userId);
        }
        
        // 收集回复用户ID
        if (comment.replyList && comment.replyList.length > 0) {
          comment.replyList.forEach(reply => {
            if (reply.userId) {
              userIds.add(reply.userId);
            }
          });
        }
      });
      
      // 如果没有需要获取的用户信息，直接返回
      if (userIds.size === 0) {
        return;
      }
      
      try {
        // 批量获取用户信息
        const userIdsArray = Array.from(userIds);
        const response = await axios.post(
          'http://localhost:8081/api/comment/userProfile',
          { userIds: userIdsArray },
          { headers }
        );
        
        if (response.data && Array.isArray(response.data.data)) {
          // 构建用户信息映射表，只保留需要的字段
          const userProfileMap = {};
          response.data.data.forEach(profile => {
            userProfileMap[profile.userId] = {
              userId: profile.userId,
              icon: profile.icon,
              username: profile.username
            };
          });
          
          // 更新评论的用户信息
          this.comments.forEach(comment => {
            if (comment.userId && userProfileMap[comment.userId]) {
              comment.userProfileVO = userProfileMap[comment.userId];
            }
            
            // 更新回复的用户信息
            if (comment.replyList && comment.replyList.length > 0) {
              comment.replyList.forEach(reply => {
                if (reply.userId && userProfileMap[reply.userId]) {
                  reply.userProfileVO = userProfileMap[reply.userId];
                }
              });
            }
          });
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        // 即使获取用户信息失败，评论列表仍然显示，只是用户信息可能不完整
      }
    },
    
    // 提交评论
    async submitComment() {
      if (!this.newCommentContent.trim()) return;
      
      try {
        this.submittingComment = true;
        const articleId = this.$route.params.id;
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        const headers = {
          'Content-Type': 'application/json'
        };
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        const commentData = {
          articleId: articleId,
          content: this.newCommentContent.trim(),
          isCommented: this.article.isCommented || 0
        };
        
        const response = await axios.post(
          'http://localhost:8081/api/comment/publish',
          commentData,
          { headers }
        );
        
        if (response.data && response.data.data === false) {
          // 文章不允许评论
          alert('该文章不允许评论');
        } else if (response.data && response.data.data) {
          // 添加新评论到列表顶部
          const newComment = {
            ...response.data.data,
            replyList: [] // 确保新评论有replyList字段
          };
          this.comments.unshift(newComment);
          // 清空评论输入框
          this.newCommentContent = '';
          // 更新文章评论数
          if (this.article.commentCount !== undefined) {
            this.article.commentCount += 1;
          } else {
            this.article.commentCount = 1;
          }
        }
      } catch (error) {
        console.error('提交评论失败:', error);
        alert('评论发布失败，请稍后重试');
      } finally {
        this.submittingComment = false;
      }
    },
    
    // 提交回复
    async submitReply(comment) {
      if (!this.replyContent.trim()) return;
      
      try {
        this.submittingReply = true;
        const articleId = this.$route.params.id;
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        const headers = {
          'Content-Type': 'application/json'
        };
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        const replyData = {
          articleId: articleId,
          parentId: this.replyingToReply ? this.replyingToReply.id : comment.id,
          content: this.replyContent.trim(),
          replyToId: this.replyingToReply ? this.replyingToReply.id : null,
          isCommented: this.article.isCommented || 0
        };
        
        const response = await axios.post(
          'http://localhost:8081/api/comment/publish',
          replyData,
          { headers }
        );
        
        if (response.data && response.data.data === false) {
          // 文章不允许评论
          alert('该文章不允许评论');
        } else if (response.data && response.data.data) {
          // 添加新回复到评论的回复列表
          if (!comment.replyList) {
            comment.replyList = [];
          }
          const newReply = response.data.data;
          comment.replyList.push(newReply);
          // 清空回复输入框并隐藏
          this.replyContent = '';
          this.showReplyInput = null;
          this.replyingToReply = null;
        }
      } catch (error) {
        console.error('提交回复失败:', error);
        alert('回复发布失败，请稍后重试');
      } finally {
        this.submittingReply = false;
      }
    },
    
    // 获取当前用户信息并更新到评论或回复
    async fetchCurrentUserProfile(commentOrReply) {
      if (!commentOrReply.userId) return;
      
      try {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        const headers = {
          'Content-Type': 'application/json'
        };
        
        if (token) {
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
        }
        
        // 获取当前用户的信息
        const response = await axios.post(
          'http://localhost:8081/api/comment/userProfile',
          { userIds: [commentOrReply.userId] },
          { headers }
        );
        
        if (response.data && Array.isArray(response.data.data) && response.data.data.length > 0) {
          // 只保留需要的字段
          const userProfile = response.data.data[0];
          commentOrReply.userProfileVO = {
            userId: userProfile.userId,
            icon: userProfile.icon,
            username: userProfile.username
          };
        }
      } catch (error) {
        console.error('获取当前用户信息失败:', error);
        // 即使获取失败，评论/回复仍然显示，只是用户信息可能不完整
      }
    },
    
    // 点赞/取消点赞评论
    async toggleCommentLike(comment) {
      if (!this.checkLoginStatus()) {
        alert('请先登录后再进行点赞操作');
        return;
      }
      
      try {
        // 准备点赞数据
        const loveData = {
          id: comment.id, // 评论ID作为实体id
          loveTypeId: 2 // 评论点赞类型（根据后端定义调整）
        };
        
        // 使用LoveService进行点赞/取消点赞操作
        await LoveService.toggleLove(loveData);
        
        // 更新本地状态
        if (comment.isLove) {
          // 取消点赞
          comment.isLove = false;
          if (comment.love !== undefined) {
            comment.love = Math.max(0, comment.love - 1);
          } else {
            comment.love = 0;
          }
        } else {
          // 点赞
          comment.isLove = true;
          if (comment.love !== undefined) {
            comment.love += 1;
          } else {
            comment.love = 1;
          }
        }
      } catch (error) {
        console.error('评论点赞失败:', error);
      }
    },
    
    // 点赞/取消点赞回复
    async toggleReplyLike(reply) {
      if (!this.checkLoginStatus()) {
        alert('请先登录后再进行点赞操作');
        return;
      }
      
      try {
        // 准备点赞数据
        const loveData = {
          id: reply.id, // 回复ID作为实体id
          loveTypeId: 2 // 回复点赞类型（与评论相同，根据后端定义调整）
        };
        
        // 使用LoveService进行点赞/取消点赞操作
        await LoveService.toggleLove(loveData);
        
        // 更新本地状态
        if (reply.isLove) {
          // 取消点赞
          reply.isLove = false;
          if (reply.love !== undefined) {
            reply.love = Math.max(0, reply.love - 1);
          } else {
            reply.love = 0;
          }
        } else {
          // 点赞
          reply.isLove = true;
          if (reply.love !== undefined) {
            reply.love += 1;
          } else {
            reply.love = 1;
          }
        }
      } catch (error) {
        console.error('回复点赞失败:', error);
      }
    },
    
    // 切换回复输入框显示
    toggleReply(comment) {
      if (this.showReplyInput === comment.id) {
        this.showReplyInput = null;
        this.replyContent = '';
      } else {
        this.showReplyInput = comment.id;
        this.replyContent = '';
      }
    },
    
    // 切换回复列表展开/回收
    toggleReplies(comment) {
      const commentId = comment.id;
      if (this.expandedReplies.includes(commentId)) {
        // 如果已展开，则回收
        this.expandedReplies = this.expandedReplies.filter(id => id !== commentId);
      } else {
        // 如果未展开，则展开
        this.expandedReplies.push(commentId);
      }
    },
    
    // 回复某个回复
    toggleReplyTo(reply, comment) {
      this.showReplyInput = comment.id;
      this.replyingToReply = reply;
      this.replyContent = '';
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
}

/* 评论模块样式 */
.comments-section {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-xl);
  margin-top: var(--spacing-xl);
}

.comments-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--border-color);
}

/* 评论输入框 */
.comment-input-section {
  margin-bottom: var(--spacing-xl);
}

.comment-input-header {
  display: flex;
  gap: var(--spacing-md);
}

.user-avatar-small {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #1a1a1a;
}

.avatar-img-small {
  width: 100%;
  height: 100%;
  min-width: 100%;
  min-height: 100%;
  object-fit: cover;
  object-position: center;
  border-radius: 50%;
  display: block;
}

.comment-input-content {
  flex: 1;
}

.comment-textarea {
  width: 100%;
  min-height: 80px;
  padding: var(--spacing-md);
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 14px;
  resize: vertical;
  transition: all var(--transition-normal);
}

.comment-textarea:focus {
  outline: none;
  border-color: var(--accent-primary);
  background-color: var(--bg-secondary);
}

.comment-input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-sm);
}

.comment-word-count {
  font-size: 12px;
  color: var(--text-tertiary);
}

.submit-comment-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background-color: var(--accent-primary);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.submit-comment-btn:hover:not(:disabled) {
  background-color: var(--accent-secondary);
}

.submit-comment-btn:disabled {
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  cursor: not-allowed;
}

/* 未登录提示 */
.login-prompt {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  padding: var(--spacing-lg);
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.login-prompt p {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--text-secondary);
}

.login-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background-color: var(--accent-primary);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.login-btn:hover {
  background-color: var(--accent-secondary);
}

/* 评论列表 */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.comment-item {
  display: flex;
  gap: var(--spacing-md);
}

.comment-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.comment-content-wrapper {
  flex: 1;
  background-color: var(--bg-primary);
  border-radius: var(--border-radius);
  padding: var(--spacing-md);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xs);
}

.comment-username {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.comment-body {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: var(--spacing-sm);
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: var(--spacing-lg);
}

.comment-action-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  background-color: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-normal);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius);
}

.comment-action-btn:hover {
  background-color: var(--bg-secondary);
  color: var(--accent-primary);
}

.comment-action-btn.liked {
  color: var(--accent-primary);
}

/* 回复输入框 */
.reply-input-section {
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

.reply-input-content {
  display: flex;
  flex-direction: column;
}

.reply-textarea {
  width: 100%;
  min-height: 60px;
  padding: var(--spacing-md);
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-primary);
  font-size: 13px;
  resize: vertical;
  transition: all var(--transition-normal);
}

.reply-textarea:focus {
  outline: none;
  border-color: var(--accent-primary);
  background-color: var(--bg-primary);
}

.reply-input-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-xs);
}

.cancel-reply-btn {
  padding: var(--spacing-xs) var(--spacing-md);
  background-color: var(--bg-tertiary);
  color: var(--text-secondary);
  border: none;
  border-radius: var(--border-radius);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.cancel-reply-btn:hover {
  background-color: var(--bg-secondary);
}

.submit-reply-btn {
  padding: var(--spacing-xs) var(--spacing-md);
  background-color: var(--accent-primary);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.submit-reply-btn:hover:not(:disabled) {
  background-color: var(--accent-secondary);
}

.submit-reply-btn:disabled {
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  cursor: not-allowed;
}

/* 回复列表 */
.replies-list {
  margin-top: var(--spacing-md);
  margin-left: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.reply-item {
  display: flex;
  gap: var(--spacing-md);
}

.reply-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reply-content-wrapper {
  flex: 1;
  background-color: var(--bg-tertiary);
  border-radius: var(--border-radius);
  padding: var(--spacing-sm) var(--spacing-md);
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xs);
}

.reply-username {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.reply-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.reply-body {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: var(--spacing-xs);
  word-break: break-word;
}

.reply-to {
  color: var(--accent-primary);
}

.reply-actions {
  display: flex;
  gap: var(--spacing-lg);
}

.reply-action-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  background-color: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 11px;
  cursor: pointer;
  transition: all var(--transition-normal);
  padding: 2px var(--spacing-sm);
  border-radius: var(--border-radius);
}

.reply-action-btn:hover {
  background-color: var(--bg-secondary);
  color: var(--accent-primary);
}

.reply-action-btn.liked {
  color: var(--accent-primary);
}

/* 回复数量样式 */
.reply-count {
  margin-left: var(--spacing-xs);
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 无评论提示 */
.no-comments {
  text-align: center;
  padding: var(--spacing-xl) 0;
  color: var(--text-tertiary);
  font-size: 14px;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .comments-section {
    padding: var(--spacing-lg);
  }
  
  .comment-input-header {
    flex-direction: column;
  }
  
  .user-avatar-small {
    align-self: flex-start;
  }
  
  .replies-list {
    margin-left: 0;
  }
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

/* 用户资料卡片样式 */
.user-profile-card {
  /* 可以添加特定的样式来区分用户资料卡片 */
}

.user-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.user-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: var(--spacing-md);
  border: 2px solid var(--accent-primary);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  width: 100%;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
}

.user-bio {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-bottom: var(--spacing-md);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

.user-stats .stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.user-profile-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius-large);
  padding: var(--spacing-lg);
  text-align: center;
}

.avatar-container {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-md);
}

.avatar {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #1a1a1a;
  border: 2px solid var(--accent-primary);
}

.profile-avatar {
  width: 100%;
  height: 100%;
  min-width: 100%;
  min-height: 100%;
  object-fit: cover !important;
  object-position: center !important;
  border-radius: 50%;
  display: block;
}

.avatar-border {
  display: none;
}

.user-details {
  width: 100%;
}

.username {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-xs) 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.user-icon {
  font-size: 16px;
  vertical-align: middle;
}

.user-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  font-size: 12px;
}

.level-badge {
  background-color: var(--accent-primary);
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 11px;
}

.joined-date {
  color: var(--text-tertiary);
  font-size: 12px;
}

.bio {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  margin-bottom: var(--spacing-md);
  padding: 0 var(--spacing-xs);
}

.stats-row {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: var(--spacing-md);
  padding: var(--spacing-sm) 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-number {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.stat-divider {
  width: 1px;
  height: 30px;
  background-color: var(--border-color);
}

.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-md);
}

.action-btn {
  width: 100%;
  max-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--border-radius);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-normal);
}

.follow-btn {
  background-color: var(--accent-primary);
  color: white;
  border: 1px solid var(--accent-primary);
}

.follow-btn:hover {
  background-color: var(--accent-secondary);
  border-color: var(--accent-secondary);
}

.user-like-btn {
  background-color: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.user-like-btn:hover {
  background-color: var(--bg-primary);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.no-profile {
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
  padding: var(--spacing-md) 0;
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
  width: auto;
  min-width: 80px;
  text-align: center;
  white-space: nowrap;
}

.collect-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: auto;
  white-space: nowrap;
  height: auto;
  writing-mode: horizontal-tb;
  line-height: normal;
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