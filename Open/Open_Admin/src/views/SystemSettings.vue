<template>
  <div class="system-settings-container">
    <div class="page-header">
      <h2>系统设置</h2>
      <p>管理系统权限和敏感词过滤</p>
    </div>
    
    <!-- 标签页切换 -->
    <el-card class="settings-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- 权限配置 -->
        <el-tab-pane label="权限配置" name="permissions">
          <div class="permissions-content">
            <div class="permission-actions">
              <el-button type="primary" @click="handleAddRole">
                <el-icon><Plus /></el-icon>
                添加角色
              </el-button>
            </div>
            
            <!-- 角色列表 -->
            <el-table 
              :data="roleList" 
              style="width: 100%" 
              stripe
              @selection-change="handleRoleSelectionChange"
            >
              <el-table-column type="selection" width="55"></el-table-column>
              <el-table-column prop="id" label="角色ID" width="100"></el-table-column>
              <el-table-column prop="name" label="角色名称" width="150"></el-table-column>
              <el-table-column prop="description" label="角色描述" min-width="200"></el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="240" fixed="right" class-name="action-column">
                <template #default="{ row }">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="handleEditRole(row)"
                  :disabled="row.isDefault"
                >
                  编辑
                </el-button>
                <el-button 
                  type="success" 
                  size="small" 
                  @click="handleSetPermissions(row)"
                >
                  设置权限
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="handleDeleteRole(row)"
                  :disabled="row.isDefault"
                >
                  删除
                </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        
        <!-- 敏感词管理 -->
        <el-tab-pane label="敏感词管理" name="sensitive-words">
          <div class="sensitive-words-content">
            <div class="word-actions">
              <el-button type="primary" @click="handleAddWord">
                <el-icon><Plus /></el-icon>
                添加敏感词
              </el-button>
              <el-button type="success" @click="handleImportWords">
                <el-icon><Upload /></el-icon>
                批量导入
              </el-button>
              <el-button type="warning" @click="handleExportWords">
                <el-icon><Download /></el-icon>
                导出敏感词
              </el-button>
            </div>
            
            <!-- 搜索区域 -->
            <el-form :inline="true" :model="wordSearchForm" class="search-form">
              <el-form-item label="敏感词">
                <el-input v-model="wordSearchForm.word" placeholder="请输入敏感词"></el-input>
              </el-form-item>
              <el-form-item label="类型">
                <el-select v-model="wordSearchForm.type" placeholder="请选择类型">
                  <el-option label="全部" value=""></el-option>
                  <el-option label="政治敏感" value="political"></el-option>
                  <el-option label="色情" value="pornographic"></el-option>
                  <el-option label="暴力" value="violent"></el-option>
                  <el-option label="辱骂" value="abusive"></el-option>
                  <el-option label="广告" value="advertisement"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearchWords">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </el-form-item>
              <el-form-item>
                <el-button @click="handleResetWords">
                  <el-icon><Refresh /></el-icon>
                  重置
                </el-button>
              </el-form-item>
            </el-form>
            
            <!-- 敏感词列表 -->
            <el-table 
              :data="sensitiveWords" 
              style="width: 100%" 
              stripe
              @selection-change="handleWordSelectionChange"
            >
              <el-table-column type="selection" width="55"></el-table-column>
              <el-table-column prop="id" label="ID" width="80"></el-table-column>
              <el-table-column prop="word" label="敏感词" width="150"></el-table-column>
              <el-table-column prop="type" label="类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getWordType(row.type)">{{ getWordTypeText(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="replaceWith" label="替换为" width="100">
                <template #default="{ row }">
                  <code>{{ row.replaceWith || '*' }}</code>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="添加时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right" class-name="action-column">
                  <template #default="{ row }">
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="handleEditWord(row)"
                  >
                    编辑
                  </el-button>
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="handleDeleteWord(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="wordPagination.currentPage"
                v-model:page-size="wordPagination.pageSize"
                :page-sizes="[20, 50, 100, 200]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="wordPagination.total"
                @size-change="handleWordSizeChange"
                @current-change="handleWordCurrentChange"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
    
    <!-- 添加/编辑角色对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      :title="roleDialogType === 'add' ? '添加角色' : '编辑角色'"
      width="500px"
      :before-close="handleCloseRoleDialog"
    >
      <el-form ref="roleForm" :model="roleForm" :rules="roleFormRules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称"></el-input>
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input 
            v-model="roleForm.description" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入角色描述"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseRoleDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmitRole">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 设置权限对话框 -->
    <el-dialog
      v-model="permissionDialogVisible"
      title="设置角色权限"
      width="600px"
      :before-close="handleClosePermissionDialog"
    >
      <div v-if="currentRole">
        <h3 style="margin-bottom: 20px;">{{ currentRole.name }} - 权限配置</h3>
        <el-tree
          :data="permissionTree"
          show-checkbox
          node-key="id"
          ref="permissionTreeRef"
          :default-checked-keys="currentPermissions"
          :props="defaultProps"
        ></el-tree>
      </div>
      <template #footer>
        <el-button @click="handleClosePermissionDialog">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions">保存权限</el-button>
      </template>
    </el-dialog>
    
    <!-- 添加/编辑敏感词对话框 -->
    <el-dialog
      v-model="wordDialogVisible"
      :title="wordDialogType === 'add' ? '添加敏感词' : '编辑敏感词'"
      width="500px"
      :before-close="handleCloseWordDialog"
    >
      <el-form ref="wordForm" :model="wordForm" :rules="wordFormRules" label-width="100px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="wordForm.word" placeholder="请输入敏感词"></el-input>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="wordForm.type" placeholder="请选择敏感词类型">
            <el-option label="政治敏感" value="political"></el-option>
            <el-option label="色情" value="pornographic"></el-option>
            <el-option label="暴力" value="violent"></el-option>
            <el-option label="辱骂" value="abusive"></el-option>
            <el-option label="广告" value="advertisement"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="替换为" prop="replaceWith">
          <el-input v-model="wordForm.replaceWith" placeholder="请输入替换文本，默认为*" clearable></el-input>
          <div class="form-tip">留空则默认替换为星号</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseWordDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmitWord">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'SystemSettings',
  components: {
    Plus,
    Upload,
    Download,
    Search,
    Refresh
  },
  setup() {
    // 激活的标签页，默认选中权限配置
    const activeTab = ref('permissions')
    
    // 角色相关
    const roleList = ref([
      {
        id: 1,
        name: '超级管理员',
        description: '系统最高权限，可以管理所有功能',
        createdAt: Date.now() - 30 * 24 * 60 * 60 * 1000,
        isDefault: true,
        permissions: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
      },
      {
        id: 2,
        name: '内容审核员',
        description: '负责审核用户发布的内容',
        createdAt: Date.now() - 20 * 24 * 60 * 60 * 1000,
        isDefault: false,
        permissions: [3, 4, 5]
      },
      {
        id: 3,
        name: '普通管理员',
        description: '基础管理权限',
        createdAt: Date.now() - 15 * 24 * 60 * 60 * 1000,
        isDefault: false,
        permissions: [1, 2, 3, 6, 7]
      }
    ])
    const selectedRoles = ref([])
    
    // 角色对话框
    const roleDialogVisible = ref(false)
    const roleDialogType = ref('add')
    const roleForm = reactive({
      id: '',
      name: '',
      description: ''
    })
    
    const roleFormRef = ref(null)
    const roleFormRules = {
      name: [
        { required: true, message: '请输入角色名称', trigger: 'blur' },
        { min: 2, max: 20, message: '角色名称长度在 2 到 20 个字符', trigger: 'blur' }
      ],
      description: [
        { required: true, message: '请输入角色描述', trigger: 'blur' },
        { min: 5, max: 200, message: '角色描述长度在 5 到 200 个字符', trigger: 'blur' }
      ]
    }
    
    // 权限树
    const permissionTree = ref([
      {
        id: 1,
        label: '用户管理',
        children: [
          { id: 11, label: '查看用户列表' },
          { id: 12, label: '编辑用户信息' },
          { id: 13, label: '封禁/解封用户' }
        ]
      },
      {
        id: 2,
        label: '内容管理',
        children: [
          { id: 21, label: '查看内容列表' },
          { id: 22, label: '审核帖子' },
          { id: 23, label: '删除内容' }
        ]
      },
      {
        id: 3,
        label: '动漫库管理',
        children: [
          { id: 31, label: '查看动漫列表' },
          { id: 32, label: '添加动漫' },
          { id: 33, label: '编辑动漫' },
          { id: 34, label: '删除动漫' }
        ]
      },
      {
        id: 4,
        label: '数据统计',
        children: [
          { id: 41, label: '查看数据报表' },
          { id: 42, label: '导出数据' }
        ]
      },
      {
        id: 5,
        label: '系统设置',
        children: [
          { id: 51, label: '角色管理' },
          { id: 52, label: '权限配置' },
          { id: 53, label: '敏感词管理' }
        ]
      }
    ])
    const permissionTreeRef = ref(null)
    const defaultProps = {
      children: 'children',
      label: 'label'
    }
    
    // 权限对话框
    const permissionDialogVisible = ref(false)
    const currentRole = ref(null)
    const currentPermissions = ref([])
    
    // 敏感词相关
    const sensitiveWords = ref([
      { id: 1, word: '敏感词1', type: 'political', replaceWith: '***', createdAt: Date.now() - 10 * 24 * 60 * 60 * 1000 },
      { id: 2, word: '敏感词2', type: 'pornographic', replaceWith: '***', createdAt: Date.now() - 8 * 24 * 60 * 60 * 1000 },
      { id: 3, word: '敏感词3', type: 'violent', replaceWith: '***', createdAt: Date.now() - 5 * 24 * 60 * 60 * 1000 },
      { id: 4, word: '敏感词4', type: 'abusive', replaceWith: '***', createdAt: Date.now() - 3 * 24 * 60 * 60 * 1000 },
      { id: 5, word: '敏感词5', type: 'advertisement', replaceWith: '***', createdAt: Date.now() - 1 * 24 * 60 * 60 * 1000 }
    ])
    const selectedWords = ref([])
    const wordSearchForm = reactive({
      word: '',
      type: ''
    })
    
    const wordPagination = reactive({
      currentPage: 1,
      pageSize: 20,
      total: 150
    })
    
    // 敏感词对话框
    const wordDialogVisible = ref(false)
    const wordDialogType = ref('add')
    const wordForm = reactive({
      id: '',
      word: '',
      type: 'political',
      replaceWith: ''
    })
    
    const wordFormRef = ref(null)
    const wordFormRules = {
      word: [
        { required: true, message: '请输入敏感词', trigger: 'blur' },
        { min: 1, max: 50, message: '敏感词长度在 1 到 50 个字符', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择敏感词类型', trigger: 'change' }
      ],
      replaceWith: [
        { max: 20, message: '替换文本长度不超过 20 个字符', trigger: 'blur' }
      ]
    }
    
    // 格式化日期
    const formatDate = (timestamp) => {
      return new Date(timestamp).toLocaleString('zh-CN')
    }
    
    // 获取敏感词类型样式
    const getWordType = (type) => {
      const typeMap = {
        political: 'danger',
        pornographic: 'warning',
        violent: 'primary',
        abusive: 'info',
        advertisement: 'success'
      }
      return typeMap[type] || 'info'
    }
    
    // 获取敏感词类型文本
    const getWordTypeText = (type) => {
      const textMap = {
        political: '政治敏感',
        pornographic: '色情',
        violent: '暴力',
        abusive: '辱骂',
        advertisement: '广告'
      }
      return textMap[type] || type
    }
    
    // 角色管理方法
    const handleAddRole = () => {
      roleDialogType.value = 'add'
      roleForm.id = ''
      roleForm.name = ''
      roleForm.description = ''
      roleDialogVisible.value = true
    }
    
    const handleEditRole = (role) => {
      roleDialogType.value = 'edit'
      Object.assign(roleForm, role)
      roleDialogVisible.value = true
    }
    
    const handleDeleteRole = (role) => {
      ElMessageBox.confirm(
        `确定要删除角色「${role.name}」吗？`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'danger'
        }
      ).then(() => {
        // 从列表中删除角色
        const index = roleList.value.findIndex(item => item.id === role.id)
        if (index !== -1) {
          roleList.value.splice(index, 1)
          ElMessage.success('删除成功')
        }
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    const handleCloseRoleDialog = () => {
      roleDialogVisible.value = false
    }
    
    const handleSubmitRole = () => {
      roleFormRef.value.validate((valid) => {
        if (valid) {
          if (roleDialogType.value === 'add') {
            // 添加新角色
            roleList.value.push({
              ...roleForm,
              id: Date.now(),
              createdAt: Date.now(),
              isDefault: false,
              permissions: []
            })
            ElMessage.success('添加成功')
          } else {
            // 编辑现有角色
            const index = roleList.value.findIndex(item => item.id === roleForm.id)
            if (index !== -1) {
              roleList.value[index] = { ...roleForm }
              ElMessage.success('编辑成功')
            }
          }
          roleDialogVisible.value = false
        }
      })
    }
    
    const handleRoleSelectionChange = (selection) => {
      selectedRoles.value = selection
    }
    
    // 权限管理方法
    const handleSetPermissions = (role) => {
      currentRole.value = { ...role }
      currentPermissions.value = [...role.permissions]
      permissionDialogVisible.value = true
      // 等待DOM更新后设置选中状态
      setTimeout(() => {
        if (permissionTreeRef.value) {
          permissionTreeRef.value.setCheckedKeys(currentPermissions.value)
        }
      }, 100)
    }
    
    const handleClosePermissionDialog = () => {
      permissionDialogVisible.value = false
      currentRole.value = null
      currentPermissions.value = []
    }
    
    const handleSavePermissions = () => {
      if (permissionTreeRef.value && currentRole.value) {
        const checkedKeys = permissionTreeRef.value.getCheckedKeys()
        
        // 更新角色权限
        const roleIndex = roleList.value.findIndex(item => item.id === currentRole.value.id)
        if (roleIndex !== -1) {
          roleList.value[roleIndex].permissions = checkedKeys
          ElMessage.success('权限设置成功')
        }
        
        handleClosePermissionDialog()
      }
    }
    
    // 敏感词管理方法
    const handleAddWord = () => {
      wordDialogType.value = 'add'
      wordForm.id = ''
      wordForm.word = ''
      wordForm.type = 'political'
      wordForm.replaceWith = ''
      wordDialogVisible.value = true
    }
    
    const handleEditWord = (word) => {
      wordDialogType.value = 'edit'
      Object.assign(wordForm, word)
      wordDialogVisible.value = true
    }
    
    const handleDeleteWord = (word) => {
      ElMessageBox.confirm(
        `确定要删除敏感词「${word.word}」吗？`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'danger'
        }
      ).then(() => {
        // 从列表中删除敏感词
        const index = sensitiveWords.value.findIndex(item => item.id === word.id)
        if (index !== -1) {
          sensitiveWords.value.splice(index, 1)
          wordPagination.total--
          ElMessage.success('删除成功')
        }
      }).catch(() => {
        ElMessage.info('已取消操作')
      })
    }
    
    const handleImportWords = () => {
      // TODO: 实现文件上传逻辑
      ElMessage.success('导入功能已触发')
    }
    
    const handleExportWords = () => {
      // TODO: 实现导出逻辑
      ElMessage.success('导出功能已触发')
    }
    
    const handleSearchWords = () => {
      // TODO: 实现搜索逻辑
      wordPagination.currentPage = 1
      ElMessage.success('搜索成功')
    }
    
    const handleResetWords = () => {
      Object.keys(wordSearchForm).forEach(key => {
        wordSearchForm[key] = ''
      })
      // 重置后重新加载
      handleSearchWords()
    }
    
    const handleWordSelectionChange = (selection) => {
      selectedWords.value = selection
    }
    
    const handleCloseWordDialog = () => {
      wordDialogVisible.value = false
    }
    
    const handleSubmitWord = () => {
      wordFormRef.value.validate((valid) => {
        if (valid) {
          if (wordDialogType.value === 'add') {
            // 添加新敏感词
            sensitiveWords.value.push({
              ...wordForm,
              id: Date.now(),
              createdAt: Date.now()
            })
            wordPagination.total++
            ElMessage.success('添加成功')
          } else {
            // 编辑现有敏感词
            const index = sensitiveWords.value.findIndex(item => item.id === wordForm.id)
            if (index !== -1) {
              sensitiveWords.value[index] = { ...wordForm }
              ElMessage.success('编辑成功')
            }
          }
          wordDialogVisible.value = false
        }
      })
    }
    
    // 敏感词分页
    const handleWordSizeChange = (size) => {
      wordPagination.pageSize = size
    }
    
    const handleWordCurrentChange = (current) => {
      wordPagination.currentPage = current
    }
    
    return {
      activeTab,
      roleList,
      selectedRoles,
      roleDialogVisible,
      roleDialogType,
      roleForm,
      roleFormRef,
      roleFormRules,
      permissionTree,
      permissionTreeRef,
      defaultProps,
      permissionDialogVisible,
      currentRole,
      currentPermissions,
      sensitiveWords,
      selectedWords,
      wordSearchForm,
      wordPagination,
      wordDialogVisible,
      wordDialogType,
      wordForm,
      wordFormRef,
      wordFormRules,
      formatDate,
      getWordType,
      getWordTypeText,
      handleAddRole,
      handleEditRole,
      handleDeleteRole,
      handleCloseRoleDialog,
      handleSubmitRole,
      handleRoleSelectionChange,
      handleSetPermissions,
      handleClosePermissionDialog,
      handleSavePermissions,
      handleAddWord,
      handleEditWord,
      handleDeleteWord,
      handleImportWords,
      handleExportWords,
      handleSearchWords,
      handleResetWords,
      handleWordSelectionChange,
      handleCloseWordDialog,
      handleSubmitWord,
      handleWordSizeChange,
      handleWordCurrentChange
    }
  }
}
</script>

<style scoped>
/* 容器样式 */
.system-settings-container {
  height: 100%;
  padding: 20px;
  background-color: #f5f7fa;
}

/* 页面头部 */
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

/* 卡片样式 */
.settings-card {
  margin-bottom: 20px;
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

/* 标签页样式 */
.settings-tabs .el-tabs__nav-wrap {
  padding: 0;
}

.settings-tabs .el-tabs__nav {
  padding: 0 20px;
  margin: 0;
}

.settings-tabs .el-tabs__tab {
  padding: 12px 20px;
  font-size: 14px;
  color: #606266;
  transition: all 0.3s ease;
}

.settings-tabs .el-tabs__tab:hover {
  color: #409eff;
}

.settings-tabs .el-tabs__tab.is-active {
  color: #409eff;
  font-weight: 500;
}

.settings-tabs .el-tabs__active-bar {
  background-color: #409eff;
  height: 3px;
  bottom: 0;
  transition: all 0.3s ease;
}

/* 权限配置样式 */
.permissions-content {
  padding: 20px;
  background-color: #fff;
}

/* 添加角色按钮容器 */
.permission-actions {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-start;
}

/* 表格样式 */
.el-table {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.el-table--striped .el-table__row--striped {
  background-color: #f5f7fa;
}

.el-table__row:hover {
  background-color: #ecf5ff !important;
  transition: background-color 0.2s ease;
}

.el-table__header th {
  background-color: #fafafa;
  font-weight: 500;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
}

/* 操作按钮样式 */
  .action-column .el-button {
    margin-right: 8px;
    padding: 6px 12px;
    font-size: 12px;
    border-radius: 3px;
  }

  .action-column .el-button:last-child {
    margin-right: 0;
  }

  /* 按钮悬停效果 */
  .action-column .el-button:hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  /* 按钮激活效果 */
  .action-column .el-button:focus {
    outline: none;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }

/* 敏感词管理样式 */
.sensitive-words-content {
  padding: 20px;
  background-color: #fff;
}

.word-actions {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .system-settings-container {
    padding: 10px;
  }
  
  .word-actions {
    flex-direction: column;
  }
  
  .search-form {
    display: flex;
    flex-direction: column;
  }
  
  .permissions-content,
  .sensitive-words-content {
    padding: 10px;
  }
  
  .el-table {
    font-size: 12px;
  }
}
</style>