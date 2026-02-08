<template>
  <div class="user-management-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <p>管理社区所有用户账号</p>
    </div>
    
    <!-- 搜索和筛选区域 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID"></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="用户状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态">
            <el-option label="全部" value=""></el-option>
            <el-option label="正常" value="normal"></el-option>
            <el-option label="封禁" value="banned"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 用户列表 -->
    <el-card class="table-card">
      <div class="table-header">
        <span>用户列表</span>
        <div class="table-actions">
          <el-button type="primary" @click="handleAddUser">插入用户</el-button>
          <el-button type="danger" :disabled="selectedUsers.length === 0" @click="handleBatchBan">批量封禁</el-button>
          <el-button type="success" :disabled="selectedUsers.length === 0" @click="handleBatchUnban">批量解封</el-button>
        </div>
      </div>
      
      <el-table 
        :data="userList" 
        style="width: 100%" 
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="用户ID" width="100"></el-table-column>
        <el-table-column prop="formattedInfo" label="用户信息" min-width="500">
          <template v-slot:default="{ row }">
            <div class="user-info">
              <el-avatar size="small" :src="row.avatar"></el-avatar>
              <pre style="white-space: pre-wrap; margin: 0; font-size: 12px;">{{ row.formattedInfo }}</pre>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180">
          <template v-slot:default="{ row }">
            {{ row.lastLoginTime || '未登录' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template v-slot:default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'danger'">
              {{ row.status === 'normal' ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'primary' : 'info'">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; flex-wrap: wrap;">
              <el-button 
                type="primary" 
                size="small" 
                @click="handleViewDetail(row)"
                style="margin-right: 8px; margin-bottom: 5px; width: 50px; text-align: center; flex-shrink: 0"
              >
                详情
              </el-button>
              <el-button 
                :type="row.status === 'normal' ? 'danger' : 'success'" 
                size="small" 
                @click="handleToggleStatus(row)"
                style="margin-bottom: 5px; width: 50px; text-align: center; flex-shrink: 0"
              >
                {{ row.status === 'normal' ? '封禁' : '解封' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
    
    <!-- 用户详情显示区域 -->
    <el-card v-if="currentUser" class="detail-card">
      <div class="card-header">
        <h3>用户详情</h3>
        <el-button type="primary" size="small" @click="handleCloseDetail">关闭详情</el-button>
      </div>
      <div class="user-detail">
        <div class="detail-header">
          <el-avatar :size="80" :src="currentUser.avatar"></el-avatar>
          <div class="detail-info">
            <h3>{{ currentUser.formattedInfo }}</h3>
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 添加用户对话框 -->
    <el-dialog
      v-model="addUserVisible"
      title="插入用户"
      width="500px"
      :before-close="handleCloseAddUser"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userFormRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
      </el-form>
      <template v-slot:footer>
        <el-button @click="handleCloseAddUser">取消</el-button>
        <el-button type="primary" @click="handleSubmitAddUser">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, put, post } from '../api/request'

// 搜索表单
const searchForm = reactive({
  userId: '',
  username: '',
  status: ''
})

// 分页配置
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 选中的用户
const selectedUsers = ref([])

// 当前显示的用户详情
const currentUser = ref(null)

// 添加用户对话框
const addUserVisible = ref(false)
const userForm = reactive({
  username: '',
  password: ''
})
const userFormRef = ref(null)
const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 用户列表
const userList = ref([])

// 获取用户列表
const fetchUserList = async () => {
  console.log('开始获取用户列表...')
  userList.value = []
  try {
    // 构建查询参数
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    
    if (searchForm.userId) params.userId = searchForm.userId
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.status) params.status = searchForm.status
    
    console.log('查询参数:', params)
    const url = 'http://localhost:8081/admin/user'
    console.log('请求URL:', url)
    
    // 获取响应数据
    const response = await get(url, params)
    console.log('响应数据:', response)
    
    // 适配多种后端返回格式
    let resultData = []
    let totalCount = 0
    
    // 检查是否为成功响应
    let isSuccess = true
    // 检查各种可能的错误响应格式
    if (response.code === 0 || response.error || response.success === false) {
      isSuccess = false
    }
    
    if (!isSuccess) {
      ElMessage.error(response.msg || response.message || '获取用户列表失败')
      return
    }
    
    // 优先检查data.records格式（Result<PageResult>）
    if (response.data && response.data.records && Array.isArray(response.data.records)) {
      resultData = response.data.records
      totalCount = response.data.total !== undefined ? response.data.total : response.data.records.length
    } 
    // 检查是否直接是数组格式
    else if (response.data && Array.isArray(response.data)) {
      resultData = response.data
      totalCount = response.data.length
    }
    // 检查是否直接返回了records数组
    else if (response.records && Array.isArray(response.records)) {
      resultData = response.records
      totalCount = response.total !== undefined ? response.total : response.records.length
    }
    // 检查response本身是否是数组
    else if (Array.isArray(response)) {
      resultData = response
      totalCount = response.length
    }
    // 尝试直接使用response作为数据
    else {
      console.warn('响应格式不匹配预期，尝试直接使用response:', response)
      // 如果response是对象且有id等字段，可能是单个用户对象
      if (response && typeof response === 'object' && response.id) {
        resultData = [response]
        totalCount = 1
      }
    }
    
    // 如果没有找到数据，显示警告
    if (resultData.length === 0) {
      console.warn('未找到用户数据', response)
      ElMessage.warning('未找到用户数据')
    }
    
    total.value = totalCount
    userList.value = resultData.map(user => ({
      id: user.id || '',
      username: user.username || '未知用户',
      status: user.status || 'normal',
      email: user.email || user.mailbox || '',
      mailbox: user.email || user.mailbox || '',
      avatar: user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      role: user.role || '普通用户',
      createTime: user.createTime || '',
      updateTime: user.updateTime || user.createTime || '',
      lastLoginTime: user.lastLoginTime || '未登录',
      formattedInfo: `用户名: ${user.username || '未知用户'}\nID: ${user.id || ''}`
    }))
    
    console.log('处理后的用户列表数据:', userList.value)
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败: ' + (error.message || '网络错误'))
  }
}

// 搜索用户
const handleSearch = async () => {
  // 重置为第一页
  currentPage.value = 1;
  await fetchUserList();
}

// 重置搜索条件
const handleReset = () => {
  Object.keys(searchForm).forEach(key => {
    searchForm[key] = '';
  });
  // 重置为第一页
  currentPage.value = 1;
  fetchUserList();
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

// 查看用户详情
const handleViewDetail = (user) => {
  currentUser.value = { ...user }
}

// 关闭详情显示
const handleCloseDetail = () => {
  currentUser.value = null
}

// 切换用户状态
const handleToggleStatus = (user) => {
  const action = user.status === 'normal' ? '封禁' : '解封'
  ElMessageBox.confirm(
    `确定要${action}用户「${user.username}」吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: user.status === 'normal' ? 'warning' : 'info'
    }).then(async () => {
    try {
      const url = `http://localhost:8081/admin/user/${user.id}/status`;
      const data = {
        status: user.status === 'normal' ? 'banned' : 'normal'
      };
      
      const response = await put(url, data);
      
      // 适配后端不同的响应格式
      let success = false;
      // 检查各种可能的成功响应格式
      if (response && (response.code === 1 || response.success === true || response.status === 'success')) {
        success = true;
      }
      // 如果没有明确的成功标识，也认为操作可能成功
      else if (!response || !response.code || !response.error) {
        success = true;
      }
      
      if (success) {
        const index = userList.value.findIndex(u => u.id === user.id)
        if (index !== -1) {
          userList.value[index].status = user.status === 'normal' ? 'banned' : 'normal'
        }
        ElMessage.success(`${action}成功`)
      } else {
        const errorMsg = response.message || response.error || `${action}失败`;
        ElMessage.error(errorMsg);
      }
    } catch (error) {
      console.error(`${action}失败:`, error);
      ElMessage.error(`${action}失败，请稍后重试`);
    }
  }).catch(() => {
    ElMessage.info('操作已取消')
  })
}

// 批量封禁
const handleBatchBan = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请选择要封禁的用户')
    return
  }
  
  const normalUsers = selectedUsers.value.filter(user => user.status === 'normal')
  if (normalUsers.length === 0) {
    ElMessage.warning('所选用户均已封禁')
    return
  }
  
  ElMessageBox.confirm(
    `确定要封禁选中的${normalUsers.length}个用户吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
    try {
      const url = 'http://localhost:8081/admin/user/batch/ban';
      const data = {
        ids: normalUsers.map(user => user.id)
      };
      
      await put(url, data);
      
      normalUsers.forEach(user => {
        const index = userList.value.findIndex(u => u.id === user.id)
        if (index !== -1) {
          userList.value[index].status = 'banned'
        }
      })
      selectedUsers.value = []
      ElMessage.success('批量封禁成功')
    } catch (error) {
      console.error('批量封禁失败:', error);
    }
  }).catch(() => {
    ElMessage.info('操作已取消')
  })
}

// 批量解封
const handleBatchUnban = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请选择要解封的用户')
    return
  }
  
  const bannedUsers = selectedUsers.value.filter(user => user.status === 'banned')
  if (bannedUsers.length === 0) {
    ElMessage.warning('所选用户均已解封')
    return
  }
  
  ElMessageBox.confirm(
    `确定要解封选中的${bannedUsers.length}个用户吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }).then(async () => {
    try {
      const url = 'http://localhost:8081/admin/user/batch/unban';
      const data = {
        ids: bannedUsers.map(user => user.id)
      };
      
      await put(url, data);
      
      bannedUsers.forEach(user => {
        const index = userList.value.findIndex(u => u.id === user.id)
        if (index !== -1) {
          userList.value[index].status = 'normal'
        }
      })
      selectedUsers.value = []
      ElMessage.success('批量解封成功')
    } catch (error) {
      console.error('批量解封失败:', error);
    }
  }).catch(() => {
    ElMessage.info('操作已取消')
  })
}

// 当前页码变化处理函数
const handleCurrentChange = (newPage) => {
  console.log('页码变更为:', newPage);
  // 直接更新页码
  currentPage.value = newPage;
  // 立即获取数据
  fetchUserList();
}

// 每页数量变化处理函数
const handleSizeChange = (size) => {
  console.log('每页条数变更为:', size);
  // 更新每页条数
  pageSize.value = size;
  // 重置为第一页
  currentPage.value = 1;
  // 重新获取数据
  fetchUserList();
}

// 打开添加用户对话框
const handleAddUser = () => {
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
  userForm.username = ''
  userForm.password = ''
  addUserVisible.value = true
}

// 关闭添加用户对话框
const handleCloseAddUser = () => {
  addUserVisible.value = false
}

// 提交添加用户表单
const handleSubmitAddUser = async () => {
  userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const url = 'http://localhost:8081/admin/user/add';
        await post(url, userForm);
        
        ElMessage.success('用户添加成功')
        addUserVisible.value = false
        await fetchUserList()
      } catch (error) {
        console.error('用户添加失败:', error);
      }
    }
  })
}

// 组件挂载时获取用户列表
onMounted(async () => {
  await fetchUserList()
})
</script>

<style scoped>
.user-management-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin-bottom: 8px;
  color: #333;
}

.page-header p {
  color: #666;
  margin: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
}

.table-card {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: bold;
}

.table-actions {
  display: flex;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  font-weight: 500;
}

/* 详情卡片样式 */
.detail-card {
  margin-top: 20px;
  border-top: 3px solid #409eff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
}

.card-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.user-detail {
  padding: 10px 0;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.detail-info h3 {
  margin: 0 0 5px 0;
  color: #333;
}

.detail-info p {
  margin: 0;
  color: #666;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.detail-item .label {
  width: 100px;
  color: #606266;
  font-weight: 500;
}

.detail-item .value {
  color: #303133;
}
</style>