import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'

// 懒加载组件
const CommunityView = () => import('../views/CommunityView.vue')
const RegisterView = () => import('../views/RegisterView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const CreateContentView = () => import('../views/CreateContentView.vue')
const EditProfileView = () => import('../views/EditProfileView.vue')
const SearchView = () => import('../views/SearchView.vue')
const ReviewArticleView = () => import('../views/ReviewArticleView.vue')
const ArticleDetailView = () => import('../views/ArticleDetailView.vue')
const ArticleDetailPublicView = () => import('../views/ArticleDetailPublicView.vue')
const ModeratorView = () => import('../views/ModeratorView.vue')
const DynamicView = () => import('../views/DynamicView.vue')
const NotificationView = () => import('../views/NotificationView.vue')
const HotRankingView = () => import('../views/HotRankingView.vue')
const WebSocketTest = () => import('../components/WebSocketTest.vue')

const routes = [
  {
    path: '/',
    redirect: '/community' // 默认重定向到社区主页
  },
  {
    path: '/oauth2/callback/github',
    name: 'githubCallback',
    component: () => import('../views/GitHubCallbackView.vue'),
    meta: {
      requiresAuth: false // 公开可访问，不需要登录
    }
  },
  {
    path: '/oauth/callback',
    name: 'githubCallbackAlternative',
    component: () => import('../views/GitHubCallbackView.vue'),
    meta: {
      requiresAuth: false // 公开可访问，不需要登录
    }
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/login/qq',
    name: 'loginQQ',
    component: () => import('../views/QQLoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView
  },
  {
    path: '/community',
    name: 'community',
    component: CommunityView,
    meta: {
      requiresAuth: false // 公开可访问，不需要登录
    }
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/profile/edit',
    name: 'editProfile',
    component: EditProfileView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/articles/create',
    name: 'create',
    component: CreateContentView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/articles/create/drafts',
    name: 'createDrafts',
    component: CreateContentView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/articles/create/published',
    name: 'createPublished',
    component: CreateContentView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/articles/create/pending',
    name: 'createPending',
    component: CreateContentView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/search',
    name: 'search',
    component: SearchView,
    meta: {
      requiresAuth: false // 公开可访问，不需要登录
    }
  },
  {
    path: '/article/:id',
    name: 'articleDetail',
    component: ArticleDetailPublicView,
    meta: {
      requiresAuth: false // 公开可访问，不需要登录
    }
  },
  {
    path: '/article/author/:id',
    name: 'articleDetailAuthor',
    component: ArticleDetailView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  // 保留原路由作为兼容
  {
    path: '/review/:id',
    name: 'reviewArticle',
    component: ReviewArticleView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },

  {
    path: '/dynamic',
    name: 'dynamic',
    component: DynamicView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {    
    path: '/moderator',
    name: 'moderator',
    component: ModeratorView,
    meta: {      
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/messages',
    name: 'messages',
    component: NotificationView,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/websocket-test',
    name: 'websocketTest',
    component: WebSocketTest,
    meta: {
      requiresAuth: true // 需要登录才能访问
    }
  },
  {
    path: '/hot-ranking',
    name: 'hotRanking',
    component: HotRankingView,
    meta: {
      requiresAuth: false // 公开可访问，不需要登录
    }
  },
  {    
    path: '/:pathMatch(.*)*',
    redirect: '/community' // 404重定向
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查是否登录
router.beforeEach((to, from, next) => {
  // 检查路由是否需要认证
  if (to.meta.requiresAuth) {
    // 检查是否已登录
    const isLoggedIn = localStorage.getItem('isLoggedIn') || sessionStorage.getItem('isLoggedIn')
    if (isLoggedIn) {
      next() // 已登录，允许访问
    } else {
      next('/login') // 未登录，重定向到登录页
    }
  } else {
    next() // 不需要认证，直接访问
  }
})

export default router