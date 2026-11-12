import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  // 生产构建产物由后端 /api 前缀托管，使用 HTML5 History 路由需绝对 base；开发环境仍走根路径
  base: command === 'build' ? '/api/' : '/',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router'],
          element: ['element-plus', '@element-plus/icons-vue'],
          charts: ['echarts']
        }
      }
    }
  }
}))
