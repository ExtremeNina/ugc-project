<template>
  <div class="anime-management-container">
    <div class="page-header">
      <h2>动漫库管理</h2>
      <p>管理社区的动漫资源和标签</p>
    </div>
    
    <!-- 功能按钮 -->
    <div class="action-buttons">
      <el-button type="primary" @click="handleAddAnime">
        <el-icon><Plus /></el-icon>
        添加动漫
      </el-button>
      <el-button type="success" @click="handleImportAnime">
        <el-icon><Upload /></el-icon>
        批量导入
      </el-button>
    </div>
    
    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="动漫名称">
          <el-input v-model="searchForm.title" placeholder="请输入动漫名称"></el-input>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" multiple collapse-tags>
            <el-option 
              v-for="type in animeTypes" 
              :key="type.value" 
              :label="type.label" 
              :value="type.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="年份">
          <el-select v-model="searchForm.year" placeholder="请选择年份">
            <el-option 
              v-for="year in availableYears" 
              :key="year" 
              :label="year + '年'" 
              :value="year"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 动漫列表 -->
    <el-card class="table-card">
      <div class="table-header">
        <span>动漫列表</span>
        <div class="table-actions">
          <el-button 
            type="danger" 
            :disabled="selectedAnimes.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>
      
      <el-table 
        :data="animeList" 
        style="width: 100%" 
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="title" label="动漫名称" min-width="200">
          <template #default="{ row }">
            <div class="anime-info">
              <el-image :src="row.cover" fit="cover" class="anime-cover"></el-image>
              <div class="anime-title">
                <div class="main-title">{{ row.title }}</div>
                <div class="subtitle">{{ row.originalTitle }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="180">
          <template #default="{ row }">
            <el-tag 
              v-for="(type, index) in row.type" 
              :key="index" 
              size="small" 
              style="margin-right: 5px"
            >
              {{ getTypeName(type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="80"></el-table-column>
        <el-table-column prop="episodes" label="集数" width="80"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ongoing' ? 'primary' : 'success'">
              {{ row.status === 'ongoing' ? '连载中' : '已完结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="80">
          <template #default="{ row }">
            <div class="rating">
              <el-rate v-model="row.rating" disabled show-score score-template="{{value}}"></el-rate>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleEditAnime(row)"
              style="margin-right: 8px"
            >
              编辑
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDeleteAnime(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 添加/编辑动漫对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加动漫' : '编辑动漫'"
      width="700px"
      :before-close="handleCloseDialog"
    >
      <el-form ref="animeForm" :model="animeForm" :rules="formRules" label-width="100px">
        <el-form-item label="动漫名称" prop="title">
          <el-input v-model="animeForm.title" placeholder="请输入动漫名称"></el-input>
        </el-form-item>
        <el-form-item label="原名" prop="originalTitle">
          <el-input v-model="animeForm.originalTitle" placeholder="请输入动漫原名（日文名）"></el-input>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="animeForm.type" placeholder="请选择动漫类型" multiple collapse-tags>
            <el-option 
              v-for="type in animeTypes" 
              :key="type.value" 
              :label="type.label" 
              :value="type.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="年份" prop="year">
          <el-select v-model="animeForm.year" placeholder="请选择发行年份">
            <el-option 
              v-for="year in availableYears" 
              :key="year" 
              :label="year + '年'" 
              :value="year"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="集数" prop="episodes">
          <el-input v-model.number="animeForm.episodes" placeholder="请输入集数"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="animeForm.status" placeholder="请选择状态">
            <el-option label="连载中" value="ongoing"></el-option>
            <el-option label="已完结" value="completed"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="animeForm.rating" show-score score-template="{{value}}"></el-rate>
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input 
            v-model="animeForm.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入动漫简介"
          ></el-input>
        </el-form-item>
        <el-form-item label="封面图" prop="cover">
          <el-input v-model="animeForm.cover" placeholder="请输入封面图片URL"></el-input>
          <div v-if="animeForm.cover" class="preview-image">
            <el-image :src="animeForm.cover" fit="cover"></el-image>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Search, Refresh, Delete } from '@element-plus/icons-vue'

export default {
  name: 'AnimeManagement',
  components: {
    Plus,
    Upload,
    Search,
    Refresh,
    Delete
  },
  setup() {
    // 搜索表单
    const searchForm = reactive({
      title: '',
      type: [],
      year: ''
    })
    
    // 分页配置
    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 1234
    })
    
    // 选中的动漫
    const selectedAnimes = ref([])
    
    // 对话框配置
    const dialogVisible = ref(false)
    const dialogType = ref('add')
    const animeForm = reactive({
      id: '',
      title: '',
      originalTitle: '',
      type: [],
      year: '',
      episodes: 0,
      status: 'ongoing',
      rating: 0,
      description: '',
      cover: ''
    })
    
    const animeFormRef = ref(null)
    
    // 表单验证规则
    const formRules = {
      title: [
        { required: true, message: '请输入动漫名称', trigger: 'blur' },
        { min: 1, max: 100, message: '名称长度在 1 到 100 个字符', trigger: 'blur' }
      ],
      originalTitle: [
        { min: 0, max: 100, message: '原名长度不超过 100 个字符', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请至少选择一个类型', trigger: 'change' }
      ],
      year: [
        { required: true, message: '请选择年份', trigger: 'change' }
      ],
      episodes: [
        { required: true, message: '请输入集数', trigger: 'blur' },
        { type: 'number', min: 1, message: '集数必须大于0', trigger: 'blur' }
      ],
      status: [
        { required: true, message: '请选择状态', trigger: 'change' }
      ],
      description: [
        { min: 0, max: 1000, message: '简介长度不超过 1000 个字符', trigger: 'blur' }
      ],
      cover: [
        { required: true, message: '请输入封面图片URL', trigger: 'blur' }
      ]
    }
    
    // 动漫类型
    const animeTypes = [
      { label: '热血', value: 'action' },
      { label: '冒险', value: 'adventure' },
      { label: '喜剧', value: 'comedy' },
      { label: '科幻', value: 'scifi' },
      { label: '奇幻', value: 'fantasy' },
      { label: '恋爱', value: 'romance' },
      { label: '校园', value: 'school' },
      { label: '魔法', value: 'magic' },
      { label: '悬疑', value: 'mystery' },
      { label: '恐怖', value: 'horror' },
      { label: '治愈', value: 'healing' },
      { label: '日常', value: 'daily' },
      { label: '历史', value: 'history' },
      { label: '音乐', value: 'music' },
      { label: '运动', value: 'sports' }
    ]
    
    // 可用年份
    const availableYears = computed(() => {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let i = currentYear; i >= 1990; i--) {
        years.push(i)
      }
      return years
    })
    
    // 模拟动漫数据
    const animeList = ref([
      {
        id: 1,
        title: '原神',
        originalTitle: 'Genshin Impact',
        cover: 'https://example.com/anime1.jpg',
        type: ['fantasy', 'adventure'],
        year: 2020,
        episodes: 12,
        status: 'ongoing',
        rating: 4.8
      },
      {
        id: 2,
        title: '鬼灭之刃',
        originalTitle: '鬼滅の刃',
        cover: 'https://example.com/anime2.jpg',
        type: ['action', 'fantasy'],
        year: 2019,
        episodes: 26,
        status: 'completed',
        rating: 4.9
      },
      {
        id: 3,
        title: '进击的巨人',
        originalTitle: '進撃の巨人',
        cover: 'https://example.com/anime3.jpg',
        type: ['action', 'fantasy'],
        year: 2013,
        episodes: 75,
        status: 'completed',
        rating: 4.7
      },
      {
        id: 4,
        title: '辉夜大小姐想让我告白',
        originalTitle: 'かぐや様は告らせたい',
        cover: 'https://example.com/anime4.jpg',
        type: ['comedy', 'romance', 'school'],
        year: 2019,
        episodes: 39,
        status: 'completed',
        rating: 4.6
      },
      {
        id: 5,
        title: '我的青春恋爱物语果然有问题',
        originalTitle: 'やはり俺の青春ラブコメはまちがっている。',
        cover: 'https://example.com/anime5.jpg',
        type: ['comedy', 'romance', 'school'],
        year: 2013,
        episodes: 38,
        status: 'completed',
        rating: 4.5
      }
    ])
    
    // 获取类型名称
    const getTypeName = (typeValue) => {
      const type = animeTypes.find(t => t.value === typeValue)
      return type ? type.label : typeValue
    }
    
    // 搜索动漫
    const handleSearch = () => {
      ElMessage.success('搜索功能已触发')
    }
    
    // 重置搜索
    const handleReset = () => {
      Object.keys(searchForm).forEach(key => {
        searchForm[key] = key === 'type' ? [] : ''
      })
    }
    
    // 处理选择变化
    const handleSelectionChange = (selection) => {
      selectedAnimes.value = selection
    }
    
    // 添加动漫
    const handleAddAnime = () => {
      dialogType.value = 'add'
      // 重置表单
      if (animeFormRef.value) {
        animeFormRef.value.resetFields()
      }
      // 设置默认值
      animeForm.id = ''
      animeForm.title = ''
      animeForm.originalTitle = ''
      animeForm.type = []
      animeForm.year = ''
      animeForm.episodes = 0
      animeForm.status = 'ongoing'
      animeForm.rating = 0
      animeForm.description = ''
      animeForm.cover = ''
      
      dialogVisible.value = true
    }
    
    // 编辑动漫
    const handleEditAnime = (anime) => {
      dialogType.value = 'edit'
      // 复制动漫数据到表单
      Object.assign(animeForm, anime)
      dialogVisible.value = true
    }
    
    // 删除动漫
    const handleDeleteAnime = (anime) => {
      ElMessageBox.confirm(
        `确定要删除动漫「${anime.title}」吗？`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'danger'
        }
      ).then(() => {
        const index = animeList.value.findIndex(a => a.id === anime.id)
        if (index !== -1) {
          animeList.value.splice(index, 1)
        }
        ElMessage.success('删除成功')
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    // 批量删除
    const handleBatchDelete = () => {
      ElMessageBox.confirm(
        `确定要删除选中的${selectedAnimes.value.length}个动漫吗？`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'danger'
        }
      ).then(() => {
        selectedAnimes.value.forEach(anime => {
          const index = animeList.value.findIndex(a => a.id === anime.id)
          if (index !== -1) {
            animeList.value.splice(index, 1)
          }
        })
        selectedAnimes.value = []
        ElMessage.success('批量删除成功')
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    // 导入动漫
    const handleImportAnime = () => {
      ElMessage.success('导入功能已触发')
    }
    
    // 关闭对话框
    const handleCloseDialog = () => {
      dialogVisible.value = false
    }
    
    // 提交表单
    const handleSubmit = () => {
      animeFormRef.value.validate((valid) => {
        if (valid) {
          if (dialogType.value === 'add') {
            // 添加新动漫
            const newAnime = {
              ...animeForm,
              id: animeList.value.length > 0 ? Math.max(...animeList.value.map(a => a.id)) + 1 : 1
            }
            animeList.value.unshift(newAnime)
            ElMessage.success('添加成功')
          } else {
            // 编辑现有动漫
            const index = animeList.value.findIndex(a => a.id === animeForm.id)
            if (index !== -1) {
              animeList.value[index] = { ...animeForm }
            }
            ElMessage.success('编辑成功')
          }
          dialogVisible.value = false
        } else {
          return false
        }
      })
    }
    
    // 分页处理
    const handleSizeChange = (size) => {
      pagination.pageSize = size
    }
    
    const handleCurrentChange = (current) => {
      pagination.currentPage = current
    }
    
    return {
      searchForm,
      pagination,
      animeList,
      selectedAnimes,
      dialogVisible,
      dialogType,
      animeForm,
      animeFormRef,
      formRules,
      animeTypes,
      availableYears,
      getTypeName,
      handleSearch,
      handleReset,
      handleSelectionChange,
      handleAddAnime,
      handleEditAnime,
      handleDeleteAnime,
      handleBatchDelete,
      handleImportAnime,
      handleCloseDialog,
      handleSubmit,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.anime-management-container {
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

.action-buttons {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.filter-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 0;
}

.table-card {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 500;
}

.anime-info {
  display: flex;
  align-items: center;
}

.anime-cover {
  width: 40px;
  height: 60px;
  margin-right: 12px;
  border-radius: 4px;
}

.anime-title {
  flex: 1;
}

.main-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 12px;
  color: #666;
}

.rating {
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.preview-image {
  margin-top: 10px;
}

.preview-image .el-image {
  width: 200px;
  height: 280px;
  border-radius: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .action-buttons {
    flex-direction: column;
  }
  
  .search-form {
    display: flex;
    flex-direction: column;
  }
  
  .anime-info {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .anime-cover {
    margin-bottom: 8px;
  }
}
</style>