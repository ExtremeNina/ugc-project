// 获取用户列表
const fetchUserList = async () => {
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
    
    const url = 'http://localhost:8081/admin/user'
    const response = await get(url, params)
    
    // 适配后端返回的 {data: {records: Array}} 格式
    let resultData = []
    let totalCount = 0
    
    // 优先检查data.records格式
    if (response && response.data && response.data.records && Array.isArray(response.data.records)) {
      resultData = response.data.records
      totalCount = response.data.records.length
    } else if (response && Array.isArray(response.data)) {
      resultData = response.data
      totalCount = response.data.length
    } else if (Array.isArray(response)) {
      resultData = response
      totalCount = response.length
    }
    
    total.value = totalCount
    userList.value = resultData.map(user => ({
      id: user.id || '',
      username: user.username || '未知用户',
      status: user.status || 'normal',
      email: user.email || user.mailbox || '',
      mailbox: user.email || user.mailbox || '',
      avatar: user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      role: user.role || 'user',
      createTime: user.createTime || '',
      updateTime: user.updateTime || '',
      lastLoginTime: user.lastLoginTime || '',
      formattedInfo: `用户名: ${user.username || '未知用户'}\nID: ${user.id || ''}`
    }))
    
    console.log('用户列表数据:', userList.value)
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  }
}