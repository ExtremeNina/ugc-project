<template>
  <el-container class="main-container">
    <!-- 左侧导航栏 -->
    <el-aside width="240px" class="sidebar-container">
      <div class="logo-container">
        <h2 class="logo-title">二次元社区管理后台</h2>
      </div>
      <el-menu
        :default-active="activePath"
        class="el-menu-vertical"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
        router
      >
        <el-menu-item
          v-for="route in routes"
          :key="route.path"
          :index="route.path"
          :class="{ 'is-active': activePath === route.path }"
        >
          <!-- 确保图标组件能够正确加载 -->
          <component :is="route.meta.icon" class="menu-icon" v-if="typeof route.meta.icon === 'string'" />
          <span>{{ route.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部Header -->
      <el-header class="header-container">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute?.meta?.title">{{ currentRoute.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar size="small" :src="userAvatar"></el-avatar>
              <span class="username">{{ username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item>
                  <el-icon><Setting /></el-icon>
                  账号设置
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><ArrowRight /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="content-container">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Setting, ArrowRight, PieChart, Document, Collection } from '@element-plus/icons-vue'

export default {
  name: 'Layout',
  components: {
    User,
    Setting,
    ArrowRight,
    PieChart,
    Document,
    Collection
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    // 用户信息
    const username = ref('管理员')
    const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')
    
    // 获取所有子路由
    const routes = computed(() => {
      // 找到包含children的主路由
      const mainRoute = router.options.routes.find(r => r.path === '/' && r.children)
      if (mainRoute && mainRoute.children) {
        // 返回所有子路由
        return mainRoute.children
      }
      return []
    })
    
    // 当前激活的路由路径
    const activePath = computed(() => route.path)
    
    // 当前路由信息
    const currentRoute = computed(() => route)
    
    // 退出登录
    const handleLogout = async () => {
      try {
        // 清除本地存储的用户信息
        localStorage.removeItem('userInfo')
        localStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        sessionStorage.removeItem('token')
        
        ElMessage.success('退出登录成功')
        // 跳转到登录页
        router.push('/login')
      } catch (error) {
        console.error('退出登录失败:', error)
        ElMessage.error('退出登录失败')
      }
    }
    
    onMounted(() => {
      // 初始化时检查是否有用户信息
      const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo')
      if (userInfoStr) {
        try {
          const userInfo = JSON.parse(userInfoStr)
          username.value = userInfo.username || '管理员'
          userAvatar.value = userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        } catch (e) {
          console.error('解析用户信息失败:', e)
        }
      }
    })
    
    return {
      routes,
      activePath,
      currentRoute,
      handleLogout,
      username,
      userAvatar
    }
  }
}
</script>

<style scoped>
.main-container {
  height: 100vh;
}

/* 侧边栏样式 */
.sidebar-container {
  height: 100%;
  background-color: #001529;
  overflow-y: auto;
}

.logo-container {
  padding: 20px;
  text-align: center;
  background-color: #002140;
  border-bottom: 1px solid #1f1f1f;
}

.logo-title {
  font-size: 16px;
  color: #fff;
  text-align: center;
  margin: 0;
  padding: 15px 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.el-menu-vertical {
  height: calc(100% - 80px);
  border-right: none;
}

.menu-icon {
  margin-right: 10px;
  font-size: 16px;
  width: 16px;
  height: 16px;
}

/* 头部样式 */
.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  font-size: 14px;
}

/* 内容区域样式 */
.content-container {
  padding: 20px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar-container {
    width: 60px !important;
  }
  
  .logo-container,
  .el-menu-item__content {
    display: none;
  }
  
  .el-menu-item {
    text-align: center;
  }
  
  .menu-icon {
    margin-right: 0;
  }
  
  .header-container {
    padding: 0 10px;
  }
  
  .content-container {
    padding: 10px;
  }
}
</style>