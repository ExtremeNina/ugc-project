<template>
  <div class="content-management-container">
    <div class="page-header">
      <h2>内容管理</h2>
      <p>管理社区所有帖子和评论内容</p>
    </div>
    
    <!-- 标签页切换 -->
    <el-card class="content-card">
      <el-tabs v-model="activeTab" class="content-tabs">
        <!-- 帖子管理 -->
        <el-tab-pane label="帖子管理" name="posts">
          <!-- 帖子搜索区域 -->
          <el-form :inline="true" :model="postSearchForm" class="search-form">
            <el-form-item label="帖子ID">
              <el-input v-model="postSearchForm.postId" placeholder="请输入帖子ID"></el-input>
            </el-form-item>
            <el-form-item label="标题">
              <el-input v-model="postSearchForm.title" placeholder="请输入标题关键词"></el-input>
            </el-form-item>
            <el-form-item label="作者">
              <el-input v-model="postSearchForm.author" placeholder="请输入作者名"></el-input>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="postSearchForm.status" placeholder="请选择状态">
                <el-option label="全部" value=""></el-option>
                <el-option label="待审核" value="pending"></el-option>
                <el-option label="已发布" value="published"></el-option>
                <el-option label="已驳回" value="rejected"></el-option>
                <el-option label="已删除" value="deleted"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handlePostSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button @click="handlePostReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
          
          <!-- 帖子列表 -->
          <el-table 
            :data="postList" 
            style="width: 100%" 
            stripe
            @selection-change="handlePostSelectionChange"
          >
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column prop="id" label="帖子ID" width="100"></el-table-column>
            <el-table-column prop="title" label="标题" min-width="300">
              <template #default="{ row }">
                <div class="post-title">{{ row.title }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="author" label="作者" width="120"></el-table-column>
            <el-table-column prop="category" label="分类" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="publishTime" label="发布时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.publishTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="views" label="浏览量" width="80"></el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #header-cell="scope">
                <div style="text-align: center;">{{ scope.column.label }}</div>
              </template>
              <template #default="{ row }">
                <div style="display: flex; align-items: center; justify-content: flex-start; gap: 8px; height: 36px; width: 100%; box-sizing: border-box;">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="handleViewPostDetail(row)"
                    style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                  >
                    详情
                  </el-button>
                  <template v-if="row.status === 'pending'">
                    <el-button 
                      type="success" 
                      size="small" 
                      @click="handleApprovePost(row)"
                      style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                    >
                      批准
                    </el-button>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="handleRejectPost(row)"
                      style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                    >
                      驳回
                    </el-button>
                  </template>
                  <template v-else>
                    <el-button 
                      type="danger" 
                      size="small" 
                      @click="handleDeletePost(row)"
                      style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                    >
                      删除
                    </el-button>
                  </template>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 帖子分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="postPagination.currentPage"
              v-model:page-size="postPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="postPagination.total"
              @size-change="handlePostSizeChange"
              @current-change="handlePostCurrentChange"
            />
          </div>
        </el-tab-pane>
        
        <!-- 待审核管理 -->
        <el-tab-pane label="待审核管理" name="pending">
          <!-- 待审核搜索区域 -->
          <el-form :inline="true" :model="pendingSearchForm" class="search-form">
            <el-form-item label="内容ID">
              <el-input v-model="pendingSearchForm.contentId" placeholder="请输入内容ID"></el-input>
            </el-form-item>
            <el-form-item label="内容类型">
              <el-select v-model="pendingSearchForm.type" placeholder="请选择类型">
                <el-option label="全部" value=""></el-option>
                <el-option label="帖子" value="post"></el-option>
                <el-option label="评论" value="comment"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="作者">
              <el-input v-model="pendingSearchForm.author" placeholder="请输入作者名"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handlePendingSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button @click="handlePendingReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
          
          <!-- 待审核列表 -->
          <el-table 
            :data="pendingList" 
            style="width: 100%" 
            stripe
            @selection-change="handlePendingSelectionChange"
          >
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column prop="id" label="内容ID" width="100"></el-table-column>
            <el-table-column prop="type" label="内容类型" width="100">
              <template #default="{ row }">
                <el-tag>{{ row.type === 'post' ? '帖子' : '评论' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题/内容" min-width="300">
              <template #default="{ row }">
                <div class="pending-content">{{ row.title || row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="author" label="作者" width="120"></el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.submitTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #header-cell="scope">
                <div style="text-align: center;">{{ scope.column.label }}</div>
              </template>
              <template #default="{ row }">
                <div style="display: flex; align-items: center; justify-content: flex-start; gap: 8px; height: 36px; width: 100%; box-sizing: border-box;">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="handleViewPendingDetail(row)"
                    style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                  >
                    详情
                  </el-button>
                  <el-button 
                    type="success" 
                    size="small" 
                    @click="handleApprovePending(row)"
                    style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                  >
                    批准
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="handleRejectPending(row)"
                    style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                  >
                    驳回
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 待审核分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pendingPagination.currentPage"
              v-model:page-size="pendingPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pendingPagination.total"
              @size-change="handlePendingSizeChange"
              @current-change="handlePendingCurrentChange"
            />
          </div>
        </el-tab-pane>

        <!-- 评论管理 -->
        <el-tab-pane label="评论管理" name="comments">
          <!-- 评论搜索区域 -->
          <el-form :inline="true" :model="commentSearchForm" class="search-form">
            <el-form-item label="评论ID">
              <el-input v-model="commentSearchForm.commentId" placeholder="请输入评论ID"></el-input>
            </el-form-item>
            <el-form-item label="评论内容">
              <el-input v-model="commentSearchForm.content" placeholder="请输入评论关键词"></el-input>
            </el-form-item>
            <el-form-item label="评论者">
              <el-input v-model="commentSearchForm.commenter" placeholder="请输入评论者名"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleCommentSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleCommentReset">
                <el-icon><Refresh /></el-icon>
                重置
              </el-button>
            </el-form-item>
          </el-form>
          
          <!-- 评论列表 -->
          <el-table 
            :data="commentList" 
            style="width: 100%" 
            stripe
            @selection-change="handleCommentSelectionChange"
          >
            <el-table-column type="selection" width="55"></el-table-column>
            <el-table-column prop="id" label="评论ID" width="100"></el-table-column>
            <el-table-column prop="content" label="评论内容" min-width="400">
              <template #default="{ row }">
                <div class="comment-content">{{ row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="commenter" label="评论者" width="120"></el-table-column>
            <el-table-column prop="postId" label="所属帖子" width="100">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="handleViewRelatedPost(row.postId)">
                  {{ row.postId }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="commentTime" label="评论时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.commentTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #header-cell="scope">
                <div style="text-align: center;">{{ scope.column.label }}</div>
              </template>
              <template #default="{ row }">
                <div style="display: flex; align-items: center; justify-content: flex-start; height: 36px; width: 100%; box-sizing: border-box;">
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="handleDeleteComment(row)"
                    style="width: 65px; height: 28px; padding: 0; display: inline-flex; align-items: center; justify-content: center; line-height: 1; margin: 0; vertical-align: middle; box-sizing: border-box; font-size: 14px;"
                  >
                    删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 评论分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="commentPagination.currentPage"
              v-model:page-size="commentPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="commentPagination.total"
              @size-change="handleCommentSizeChange"
              @current-change="handleCommentCurrentChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <!-- 帖子详情对话框 -->
    <el-dialog
      v-model="postDetailVisible"
      title="帖子详情"
      width="800px"
      :before-close="handleClosePostDetail"
    >
      <div v-if="currentPost" class="post-detail">
        <h2 class="detail-title">{{ currentPost.title }}</h2>
        <div class="detail-meta">
          <span>作者：{{ currentPost.author }}</span>
          <span>分类：{{ currentPost.category }}</span>
          <span>发布时间：{{ formatDate(currentPost.publishTime) }}</span>
          <span>浏览量：{{ currentPost.views }}</span>
        </div>
        <div class="detail-content">
          {{ currentPost.content }}
        </div>
        <div class="detail-images" v-if="currentPost.images && currentPost.images.length > 0">
          <el-image 
            v-for="(image, index) in currentPost.images" 
            :key="index"
            :src="image"
            :preview-src-list="currentPost.images"
            fit="cover"
            class="detail-image"
          ></el-image>
        </div>
      </div>
      <template #footer>
        <el-button @click="handleClosePostDetail">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'ContentManagement',
  components: {
    Search,
    Refresh
  },
  setup() {
    // 激活的标签页
    const activeTab = ref('posts')
    
    // 帖子搜索表单
    const postSearchForm = reactive({
      postId: '',
      title: '',
      author: '',
      status: ''
    })
    
    // 帖子分页配置
    const postPagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 100
    })
    
    // 选中的帖子
    const selectedPosts = ref([])
    
    // 待审核搜索表单
    const pendingSearchForm = reactive({
      contentId: '',
      type: '',
      author: ''
    })
    
    // 待审核列表数据
    const pendingList = ref([])
    
    // 待审核分页配置
    const pendingPagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })
    
    // 选中的待审核项
    const selectedPendingItems = ref([])
    
    // 评论搜索表单
    const commentSearchForm = reactive({
      commentId: '',
      content: '',
      commenter: ''
    })
    
    // 评论分页配置
    const commentPagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 200
    })
    
    // 选中的评论
    const selectedComments = ref([])
    
    // 帖子详情对话框
    const postDetailVisible = ref(false)
    const currentPost = ref(null)
    
    // 模拟帖子数据
    const postList = ref([
      {
        id: 1,
        title: '关于新番《原神》的一些看法和推荐',
        content: '《原神》作为一款开放世界动作RPG游戏，以其精美的画面和丰富的剧情吸引了大量玩家...',
        author: '二次元爱好者',
        category: '游戏讨论',
        publishTime: Date.now() - 2 * 24 * 60 * 60 * 1000,
        views: 1256,
        status: 'published',
        images: ['https://example.com/image1.jpg', 'https://example.com/image2.jpg']
      },
      {
        id: 2,
        title: '分享我的手办收藏展示',
        content: '收集手办已经有5年了，今天给大家分享一下我的收藏展示柜...',
        author: '手办达人',
        category: '周边分享',
        publishTime: Date.now() - 1 * 24 * 60 * 60 * 1000,
        views: 3456,
        status: 'pending',
        images: ['https://example.com/image3.jpg', 'https://example.com/image4.jpg', 'https://example.com/image5.jpg']
      },
      {
        id: 3,
        title: '动漫《鬼灭之刃》第二季观后有感',
        content: '期待已久的《鬼灭之刃》第二季终于开播了，炭治郎的成长令人感动...',
        author: '动漫控',
        category: '动画讨论',
        publishTime: Date.now() - 3 * 24 * 60 * 60 * 1000,
        views: 5678,
        status: 'published',
        images: []
      },
      {
        id: 4,
        title: '【违规内容】请管理员审核',
        content: '这是一条测试用的违规内容，应该被驳回...',
        author: '测试用户',
        category: '其他',
        publishTime: Date.now() - 4 * 24 * 60 * 60 * 1000,
        views: 123,
        status: 'pending',
        images: []
      },
      {
        id: 5,
        title: '日本动漫产业发展趋势分析',
        content: '近年来，日本动漫产业呈现出全球化发展的趋势，流媒体平台的兴起为动漫产业带来了新的机遇...',
        author: '资深宅',
        category: '行业分析',
        publishTime: Date.now() - 5 * 24 * 60 * 60 * 1000,
        views: 7890,
        status: 'published',
        images: ['https://example.com/image6.jpg']
      }
    ])
    
    // 模拟评论数据
    const commentList = ref([
      {
        id: 1,
        content: '写得真好，非常赞同作者的观点！',
        commenter: '动漫爱好者A',
        postId: 1,
        commentTime: Date.now() - 2 * 60 * 60 * 1000
      },
      {
        id: 2,
        content: '手办收藏太厉害了，请问是在哪里购买的？',
        commenter: '手办控B',
        postId: 2,
        commentTime: Date.now() - 5 * 60 * 60 * 1000
      },
      {
        id: 3,
        content: '期待后续更新，加油！',
        commenter: '支持者C',
        postId: 3,
        commentTime: Date.now() - 1 * 60 * 60 * 1000
      },
      {
        id: 4,
        content: '【测试违规评论】',
        commenter: '测试用户',
        postId: 1,
        commentTime: Date.now() - 3 * 60 * 60 * 1000
      },
      {
        id: 5,
        content: '分析得很到位，学习了！',
        commenter: '学习者D',
        postId: 5,
        commentTime: Date.now() - 12 * 60 * 60 * 1000
      }
    ])
    
    // 格式化日期
    const formatDate = (timestamp) => {
      return new Date(timestamp).toLocaleString('zh-CN')
    }
    
    // 获取状态类型
    const getStatusType = (status) => {
      const typeMap = {
        pending: 'warning',
        published: 'success',
        rejected: 'danger',
        deleted: 'info'
      }
      return typeMap[status] || 'info'
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      const textMap = {
        pending: '待审核',
        published: '已发布',
        rejected: '已驳回',
        deleted: '已删除'
      }
      return textMap[status] || status
    }
    
    // 帖子搜索
    const handlePostSearch = () => {
      ElMessage.success('帖子搜索功能已触发')
    }
    
    // 重置帖子搜索
    const handlePostReset = () => {
      Object.keys(postSearchForm).forEach(key => {
        postSearchForm[key] = ''
      })
    }
    
    // 评论搜索
    const handleCommentSearch = () => {
      ElMessage.success('评论搜索功能已触发')
    }
    
    // 重置评论搜索
    const handleCommentReset = () => {
      Object.keys(commentSearchForm).forEach(key => {
        commentSearchForm[key] = ''
      })
    }
    
    // 处理帖子选择变化
    const handlePostSelectionChange = (selection) => {
      selectedPosts.value = selection
    }
    
    // 处理评论选择变化
    const handleCommentSelectionChange = (selection) => {
        selectedComments.value = selection
      }

      // 处理待审核选择变化
      const handlePendingSelectionChange = (selection) => {
        selectedPendingItems.value = selection
      }

      // 处理待审核搜索
      const handlePendingSearch = () => {
        // 重置分页
        pendingPagination.currentPage = 1
        loadPendingList()
      }

      // 处理待审核重置
      const handlePendingReset = () => {
        pendingSearchForm.contentId = ''
        pendingSearchForm.type = ''
        pendingSearchForm.author = ''
        pendingPagination.currentPage = 1
        loadPendingList()
      }

      // 加载待审核列表
      const loadPendingList = () => {
        // 这里应该是实际的API调用
        // 模拟数据
        pendingList.value = [
          {
            id: '1001',
            type: 'post',
            title: '这是一个待审核的帖子标题',
            author: 'user1',
            submitTime: new Date().toISOString()
          },
          {
            id: '2001',
            type: 'comment',
            content: '这是一个待审核的评论内容',
            author: 'user2',
            submitTime: new Date().toISOString()
          }
        ]
        pendingPagination.total = pendingList.value.length
      }

      // 查看待审核详情
      const handleViewPendingDetail = (row) => {
        // 根据类型跳转到对应的详情查看
        if (row.type === 'post') {
          handleViewPostDetail(row)
        } else {
          handleViewCommentDetail(row)
        }
      }

      // 批准待审核项
      const handleApprovePending = (row) => {
        ElMessageBox.confirm('确定要批准该内容吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 这里应该是实际的API调用
          ElMessage.success('批准成功')
          loadPendingList()
        }).catch(() => {
          ElMessage.info('已取消操作')
        })
      }

      // 驳回待审核项
      const handleRejectPending = (row) => {
        ElMessageBox.confirm('确定要驳回该内容吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 这里应该是实际的API调用
          ElMessage.success('驳回成功')
          loadPendingList()
        }).catch(() => {
          ElMessage.info('已取消操作')
        })
      }

      // 处理待审核页面大小变化
      const handlePendingSizeChange = (size) => {
        pendingPagination.pageSize = size
        loadPendingList()
      }

      // 处理待审核当前页码变化
      const handlePendingCurrentChange = (current) => {
        pendingPagination.currentPage = current
        loadPendingList()
      }

      // 处理评论页面大小变化查看帖子详情
    const handleViewPostDetail = (post) => {
      currentPost.value = { ...post }
      postDetailVisible.value = true
    }
    
    // 关闭帖子详情
    const handleClosePostDetail = () => {
      postDetailVisible.value = false
      currentPost.value = null
    }
    
    // 批准帖子
    const handleApprovePost = (post) => {
      ElMessageBox.confirm(
        `确定要批准帖子「${post.title}」吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'success'
        }
      ).then(() => {
        post.status = 'published'
        ElMessage.success('批准成功')
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    // 驳回帖子
    const handleRejectPost = (post) => {
      ElMessageBox.confirm(
        `确定要驳回帖子「${post.title}」吗？`,
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        post.status = 'rejected'
        ElMessage.success('驳回成功')
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    // 删除帖子
    const handleDeletePost = (post) => {
      ElMessageBox.confirm(
        `确定要删除帖子「${post.title}」吗？此操作不可撤销。`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'danger'
        }
      ).then(() => {
        const index = postList.value.findIndex(p => p.id === post.id)
        if (index !== -1) {
          postList.value.splice(index, 1)
        }
        ElMessage.success('删除成功')
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    // 删除评论
    const handleDeleteComment = (comment) => {
      ElMessageBox.confirm(
        `确定要删除这条评论吗？`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'danger'
        }
      ).then(() => {
        const index = commentList.value.findIndex(c => c.id === comment.id)
        if (index !== -1) {
          commentList.value.splice(index, 1)
        }
        ElMessage.success('删除成功')
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    // 查看关联帖子
    const handleViewRelatedPost = (postId) => {
      const post = postList.value.find(p => p.id === postId)
      if (post) {
        handleViewPostDetail(post)
      } else {
        ElMessage.warning('帖子不存在')
      }
    }
    
    // 帖子分页处理
    const handlePostSizeChange = (size) => {
      postPagination.pageSize = size
    }
    
    const handlePostCurrentChange = (current) => {
      postPagination.currentPage = current
    }
    
    // 评论分页处理
    const handleCommentSizeChange = (size) => {
      commentPagination.pageSize = size
    }
    
    const handleCommentCurrentChange = (current) => {
      commentPagination.currentPage = current
    }
    
    return {
        activeTab,
        pendingSearchForm,
        pendingList,
        pendingPagination,
        selectedPendingItems,
        postSearchForm,
        postPagination,
        postList,
        selectedPosts,
        commentSearchForm,
        commentPagination,
        commentList,
        selectedComments,
      postDetailVisible,
      currentPost,
      formatDate,
      getStatusType,
      getStatusText,
      handlePostSearch,
      handlePostReset,
      handleCommentSearch,
      handleCommentReset,
      handlePostSelectionChange,
      handleCommentSelectionChange,
      handleViewPostDetail,
      handleClosePostDetail,
      handleApprovePost,
      handleRejectPost,
      handleDeletePost,
      handleDeleteComment,
      handleViewRelatedPost,
      handlePostSizeChange,
      handlePostCurrentChange,
      handleCommentSizeChange,
        handleCommentCurrentChange,
        handlePendingSelectionChange,
        handlePendingSearch,
        handlePendingReset,
        handleViewPendingDetail,
        handleApprovePending,
        handleRejectPending,
        handlePendingSizeChange,
        handlePendingCurrentChange
    }
  }
}
</script>

<style scoped>
.content-management-container {
  height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.page-header p {
  font-size: 14px;
  color: #666;
}

.content-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.post-title {
  line-height: 1.4;
}

.comment-content {
  line-height: 1.4;
  color: #333;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 帖子详情样式 */
.post-detail {
  padding: 10px 0;
}

.detail-title {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #333;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  color: #666;
  font-size: 14px;
}

.detail-content {
  line-height: 1.8;
  color: #333;
  margin-bottom: 20px;
  white-space: pre-wrap;
}

.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-image {
  width: 200px;
  height: 150px;
  cursor: pointer;
}



/* 待审核内容样式 */
.pending-content {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: break-all;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-form {
    display: flex;
    flex-direction: column;
  }
  
  .detail-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .detail-image {
    width: 100%;
    height: auto;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 5px;
  }
  
  .action-button {
    width: 100%;
    margin-bottom: 5px;
  }
}
</style>