<template>
  <div class="dashboard-container">
    <div class="page-header">
      <h2>数据统计</h2>
      <p>实时监控社区运营数据</p>
    </div>
    
    <!-- 数据卡片区域 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon primary">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ userCount }}</div>
            <div class="stat-label">总用户数</div>
          </div>
          <div class="stat-change positive">+12%</div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon success">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ postCount }}</div>
            <div class="stat-label">总帖子数</div>
          </div>
          <div class="stat-change positive">+8%</div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon warning">
            <el-icon><ChatRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ commentCount }}</div>
            <div class="stat-label">总评论数</div>
          </div>
          <div class="stat-change positive">+15%</div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon danger">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-number">{{ animeCount }}</div>
            <div class="stat-label">动漫作品数</div>
          </div>
          <div class="stat-change positive">+5%</div>
        </div>
      </el-card>
    </div>
    
    <!-- 图表区域 -->
    <div class="charts-container">
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>用户增长趋势</span>
          </div>
        </template>
        <div ref="userGrowthChart" class="chart"></div>
      </el-card>
      
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>内容活跃度统计</span>
          </div>
        </template>
        <div ref="contentActivityChart" class="chart"></div>
      </el-card>
    </div>
    
    <!-- 最近动态 -->
    <el-card class="activity-card">
      <template #header>
        <div class="card-header">
          <span>最近动态</span>
        </div>
      </template>
      <el-table :data="recentActivities" stripe style="width: 100%">
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'user' ? 'primary' : row.type === 'post' ? 'success' : 'warning'">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200"></el-table-column>
        <el-table-column prop="user" label="用户" width="120"></el-table-column>
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.time) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { User, Document, ChatRound, Collection } from '@element-plus/icons-vue'

export default {
  name: 'Dashboard',
  components: {
    User,
    Document,
    ChatRound,
    Collection
  },
  setup() {
    // 统计数据
    const userCount = ref(5421)
    const postCount = ref(12568)
    const commentCount = ref(89734)
    const animeCount = ref(1234)
    
    // 图表引用
    const userGrowthChart = ref(null)
    const contentActivityChart = ref(null)
    let userChart = null
    let contentChart = null
    
    // 最近动态数据
    const recentActivities = ref([
      { id: 1, type: 'user', content: '用户注册', user: '新用户123456', time: Date.now() - 1000 * 60 * 5 },
      { id: 2, type: 'post', content: '发布了新帖子《关于新番的一些看法》', user: '二次元爱好者', time: Date.now() - 1000 * 60 * 15 },
      { id: 3, type: 'comment', content: '评论了帖子《动漫推荐》', user: '动漫控', time: Date.now() - 1000 * 60 * 25 },
      { id: 4, type: 'user', content: '用户登录', user: '资深宅', time: Date.now() - 1000 * 60 * 35 },
      { id: 5, type: 'post', content: '发布了新帖子《分享我的手办收藏》', user: '手办达人', time: Date.now() - 1000 * 60 * 45 }
    ])
    
    // 初始化图表
    const initCharts = () => {
      // 用户增长趋势图
      userChart = echarts.init(userGrowthChart.value)
      userChart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: ['1月', '2月', '3月', '4月', '5月', '6月']
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: '新增用户',
          type: 'line',
          stack: '总量',
          data: [120, 132, 101, 134, 90, 230],
          smooth: true,
          lineStyle: {
            color: '#1890ff'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.1)' }
            ])
          }
        }]
      })
      
      // 内容活跃度统计图
      contentChart = echarts.init(contentActivityChart.value)
      contentChart.setOption({
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [{
          name: '内容类型',
          type: 'pie',
          radius: '50%',
          data: [
            { value: 35, name: '动画讨论' },
            { value: 25, name: '漫画分享' },
            { value: 20, name: '手办周边' },
            { value: 15, name: '声优相关' },
            { value: 5, name: '其他' }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      })
    }
    
    // 处理窗口大小变化
    const handleResize = () => {
      if (userChart) userChart.resize()
      if (contentChart) contentChart.resize()
    }
    
    // 获取类型名称
    const getTypeName = (type) => {
      const typeMap = {
        user: '用户',
        post: '帖子',
        comment: '评论'
      }
      return typeMap[type] || type
    }
    
    // 格式化时间
    const formatTime = (timestamp) => {
      const now = Date.now()
      const diff = now - timestamp
      const minutes = Math.floor(diff / 60000)
      const hours = Math.floor(diff / 3600000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (minutes < 60) return `${minutes}分钟前`
      if (hours < 24) return `${hours}小时前`
      if (days < 30) return `${days}天前`
      
      return new Date(timestamp).toLocaleDateString()
    }
    
    onMounted(() => {
      // 延迟初始化图表，确保DOM已渲染
      setTimeout(() => {
        initCharts()
      }, 100)
      
      // 监听窗口大小变化
      window.addEventListener('resize', handleResize)
    })
    
    onUnmounted(() => {
      // 销毁图表实例
      if (userChart) userChart.dispose()
      if (contentChart) contentChart.dispose()
      
      // 移除事件监听
      window.removeEventListener('resize', handleResize)
    })
    
    return {
      userCount,
      postCount,
      commentCount,
      animeCount,
      userGrowthChart,
      contentActivityChart,
      recentActivities,
      getTypeName,
      formatTime
    }
  }
}
</script>

<style scoped>
.dashboard-container {
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

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: #fff;
}

.stat-icon.primary {
  background-color: #1890ff;
}

.stat-icon.success {
  background-color: #52c41a;
}

.stat-icon.warning {
  background-color: #faad14;
}

.stat-icon.danger {
  background-color: #f5222d;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 500;
  color: #333;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

.stat-change {
  font-size: 14px;
  font-weight: 500;
}

.stat-change.positive {
  color: #52c41a;
}

.stat-change.negative {
  color: #f5222d;
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  height: 350px;
}

.chart {
  height: calc(100% - 50px);
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-card {
  margin-bottom: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .charts-container {
    grid-template-columns: 1fr;
  }
  
  .chart-card {
    height: 300px;
  }
}
</style>