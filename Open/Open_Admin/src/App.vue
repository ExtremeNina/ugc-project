<template>
  <div class="app-container">
    <!-- 路由视图 - 登录页和其他页面直接渲染 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script>

export default {
  name: 'App',
  setup() {
    // 基础应用设置，主要的布局和状态管理现在在各个视图组件中处理
    return {}
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

#app {
  height: 100%;
}

/* 管理后台样式 */

.app-container {
  height: 100vh;
  overflow: hidden;
}

.main-container {
  height: 100%;
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