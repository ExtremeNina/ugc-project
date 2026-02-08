import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false
    }
  },
  {
    path: '/',
    component: () => import('../components/Layout.vue'),
    meta: {
      requiresAuth: true
    },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: {
          title: '数据统计',
          icon: 'PieChart',
          requiresAuth: true
        }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/UserManagement.vue'),
        meta: {
          title: '用户管理',
          icon: 'User',
          requiresAuth: true
        }
      },
      {
        path: 'content',
        name: 'Content',
        component: () => import('../views/ContentManagement.vue'),
        meta: {
          title: '内容管理',
          icon: 'Document',
          requiresAuth: true
        }
      },
      {
        path: 'anime',
        name: 'Anime',
        component: () => import('../views/AnimeManagement.vue'),
        meta: {
          title: '动漫库管理',
          icon: 'Collection',
          requiresAuth: true
        }
      },
      {
        path: 'system',
        name: 'System',
        component: () => import('../views/SystemSettings.vue'),
        meta: {
          title: '系统设置',
          icon: 'Setting',
          requiresAuth: true
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由前置守卫，处理登录验证和页面标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title + ' - 二次元社区管理后台'
  }
  
  // 检查是否需要登录
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)
  const isLoggedIn = localStorage.getItem('token') || sessionStorage.getItem('token')
  
  if (requiresAuth && !isLoggedIn) {
    // 需要登录但未登录，重定向到登录页
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (to.path === '/login' && isLoggedIn) {
    // 已登录且要访问登录页，重定向到首页
    next('/dashboard')
  } else {
    // 其他情况正常跳转
    next()
  }
})

export default router