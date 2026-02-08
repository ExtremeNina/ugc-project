import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    proxy: {
      // 将 /admin 开头的请求代理到 http://localhost:8081
      '/admin': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        // 不需要重写路径，因为API路径已经是 /admin/login
        // rewrite: (path) => path.replace(/^\/admin/, '')
      }
    }
  }
})