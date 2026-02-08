<template>
  <div class="websocket-test-container">
    <h2>WebSocket连接测试</h2>
    <div class="test-controls">
      <button @click="testWebSocketConnection" :disabled="isConnecting">
        {{ isConnecting ? '正在连接...' : '测试连接' }}
      </button>
      <button @click="testSendMessage" :disabled="!isConnected">发送测试消息</button>
      <button @click="clearMessages">清空消息</button>
    </div>
    <div class="connection-status">
      <p>连接状态: <span :class="{ 'connected': isConnected, 'disconnected': !isConnected }">
        {{ isConnected ? '已连接' : '未连接' }}
      </span></p>
    </div>
    <div class="message-log">
      <h3>消息日志</h3>
      <div class="messages">
        <div v-for="message in messages" :key="message.id" class="message-item">
          <span class="message-time">{{ message.time }}</span>
          <span class="message-content">{{ JSON.stringify(message.content) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import WebSocketService from '../services/websocketService.js'

export default {
  name: 'WebSocketTest',
  data() {
    return {
      isConnected: false,
      isConnecting: false,
      messages: [],
      messageSubscription: null
    }
  },
  mounted() {
    // 订阅消息事件
    this.messageSubscription = WebSocketService.subscribe('message', (data) => {
      this.messages.push({
        id: Date.now(),
        time: new Date().toLocaleTimeString(),
        content: data
      })
    })
    
    // 定期检查连接状态
    this.statusInterval = setInterval(() => {
      this.isConnected = WebSocketService.getIsConnected()
    }, 1000)
  },
  beforeUnmount() {
    // 取消订阅
    if (this.messageSubscription) {
      this.messageSubscription()
    }
    
    // 清除定时器
    clearInterval(this.statusInterval)
  },
  methods: {
    testWebSocketConnection() {
      this.isConnecting = true
      
      // 获取当前用户的token
      const token = localStorage.getItem('token') || sessionStorage.getItem('token')
      if (!token) {
        alert('请先登录获取token')
        this.isConnecting = false
        return
      }
      
      try {
        // 尝试连接
        WebSocketService.connect(token)
        
        // 2秒后检查连接状态
        setTimeout(() => {
          this.isConnected = WebSocketService.isConnected()
          this.isConnecting = false
          
          if (this.isConnected) {
            this.messages.push({
              id: Date.now(),
              time: new Date().toLocaleTimeString(),
              content: 'WebSocket连接成功'
            })
          } else {
            this.messages.push({
              id: Date.now(),
              time: new Date().toLocaleTimeString(),
              content: 'WebSocket连接失败'
            })
          }
        }, 2000)
      } catch (error) {
        this.messages.push({
          id: Date.now(),
          time: new Date().toLocaleTimeString(),
          content: `WebSocket连接出错: ${error.message}`
        })
        this.isConnecting = false
      }
    },
    testSendMessage() {
      // 获取当前用户的userId
      const userId = localStorage.getItem('userId') || sessionStorage.getItem('userId')
      if (!userId) {
        alert('请先登录获取userId')
        return
      }
      
      // 测试发送消息
      const messageData = {
        senderId: parseInt(userId),
        receiveId: 16, // 测试接收者ID
        content: '测试实时消息接收功能',
        senderIcon: 'test-icon',
        receiveName: '测试用户'
      }
      
      const success = WebSocketService.send('message', messageData)
      if (success) {
        this.messages.push({
          id: Date.now(),
          time: new Date().toLocaleTimeString(),
          content: `发送消息成功: ${JSON.stringify(messageData)}`
        })
      } else {
        this.messages.push({
          id: Date.now(),
          time: new Date().toLocaleTimeString(),
          content: '发送消息失败'
        })
      }
    },
    clearMessages() {
      this.messages = []
    }
  }
}
</script>

<style scoped>
.websocket-test-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background-color: #f9f9f9;
}

h2 {
  margin-bottom: 20px;
  color: #333;
}

.test-controls {
  margin-bottom: 20px;
}

button {
  margin-right: 10px;
  padding: 8px 16px;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #369d6f;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.connection-status {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #e8f5e9;
  border-radius: 4px;
}

.connected {
  color: #2e7d32;
  font-weight: bold;
}

.disconnected {
  color: #c62828;
  font-weight: bold;
}

.message-log {
  margin-top: 20px;
}

.message-log h3 {
  margin-bottom: 10px;
  color: #333;
}

.messages {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  background-color: white;
}

.message-item {
  margin-bottom: 10px;
  padding: 8px;
  border-bottom: 1px solid #eee;
}

.message-time {
  display: inline-block;
  width: 120px;
  font-size: 12px;
  color: #666;
}

.message-content {
  display: inline-block;
  font-family: monospace;
  font-size: 14px;
}
</style>