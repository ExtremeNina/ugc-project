/**
 * WebSocket服务类
 * 用于管理WebSocket连接、消息发送和接收
 */
class WebSocketService {
  constructor() {
    this.socket = null;
    this.url = 'ws://localhost:8081/ws'; // WebSocket服务器地址
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 1000; // 初始重连延迟
    this.callbacks = {}; // 存储消息类型对应的回调函数
    this.isConnecting = false;
    this.isConnected = false;
    this.currentToken = null; // 当前用户认证令牌
  }

  /**
   * 建立WebSocket连接
   * @param {string} token - 用户认证令牌
   */
  connect(token) {
    if (this.isConnecting) {
      console.log('WebSocket连接正在建立中');
      return;
    }

    // 如果已有连接但token不同，先关闭现有连接再重新连接
    if (this.isConnected && this.currentToken !== token) {
      console.log('检测到token变化，重新建立WebSocket连接');
      this.disconnect();
    }

    if (this.isConnected) {
      console.log('WebSocket连接已存在');
      return;
    }

    this.currentToken = token;
    this.isConnecting = true;
    
    try {
      // 创建WebSocket连接，在URL中添加token参数
      this.socket = new WebSocket(`${this.url}?token=${token}`);
      
      // 连接打开事件
      this.socket.onopen = () => {
        console.log('WebSocket连接已建立');
        this.isConnecting = false;
        this.isConnected = true;
        this.reconnectAttempts = 0;
        this.reconnectDelay = 1000;
      };

      // 接收消息事件
      this.socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          this.handleMessage(message);
        } catch (error) {
          console.error('解析WebSocket消息失败:', error);
        }
      };

      // 连接关闭事件
      this.socket.onclose = (event) => {
        console.log('WebSocket连接已关闭:', event.code, event.reason);
        this.isConnecting = false;
        this.isConnected = false;
        // 使用当前token重新连接
        this.attemptReconnect(this.currentToken);
      };

      // 连接错误事件
      this.socket.onerror = (error) => {
        console.error('WebSocket连接错误:', error);
        this.isConnecting = false;
        this.isConnected = false;
      };
    } catch (error) {
      console.error('创建WebSocket连接失败:', error);
      this.isConnecting = false;
      this.isConnected = false;
      this.attemptReconnect(this.currentToken);
    }
  }

  /**
   * 尝试重新连接
   * @param {string} token - 用户认证令牌
   */
  attemptReconnect(token) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      this.reconnectDelay *= 2; // 指数退避重连策略
      
      console.log(`尝试第${this.reconnectAttempts}次重新连接，延迟${this.reconnectDelay}ms`);
      
      setTimeout(() => {
        this.connect(token);
      }, this.reconnectDelay);
    } else {
      console.error('WebSocket重连失败，已达到最大重连次数');
    }
  }

  /**
   * 处理接收到的消息
   * @param {Object|string} message - 消息对象或字符串
   */
  handleMessage(message) {
    let type, data;
    
    console.log('收到WebSocket消息:', message);
    
    // 检查消息是否为字符串格式的PrivateDTO
    if (typeof message === 'string' && message.startsWith('PrivateDTO(')) {
      // 解析字符串格式的PrivateDTO
      const privateDTO = {};
      
      // 提取receiveId
      const receiveIdMatch = message.match(/receiveId=(\d+)/);
      if (receiveIdMatch && receiveIdMatch[1]) {
        privateDTO.receiveId = parseInt(receiveIdMatch[1]);
      }
      
      // 提取content
      const contentMatch = message.match(/content=(.+?)\)/);
      if (contentMatch && contentMatch[1]) {
        privateDTO.content = contentMatch[1];
      }
      
      // 提取其他可能的字段
      const senderIdMatch = message.match(/senderId=(\d+)/);
      if (senderIdMatch && senderIdMatch[1]) {
        privateDTO.senderId = parseInt(senderIdMatch[1]);
      }
      
      // 兼容处理time和dateTime字段
      const timeMatch = message.match(/time=(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?)/);
      if (timeMatch && timeMatch[1]) {
        privateDTO.time = timeMatch[1];
        privateDTO.dateTime = timeMatch[1]; // 兼容dateTime字段
      }
      
      const dateTimeMatch = message.match(/dateTime=(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?)/);
      if (dateTimeMatch && dateTimeMatch[1]) {
        privateDTO.dateTime = dateTimeMatch[1];
        privateDTO.time = dateTimeMatch[1]; // 兼容time字段
      }
      
      type = 'message';
      data = privateDTO;
    } else if (message && message.type) {
      // 标准格式: { type, data }
      type = message.type;
      data = message.data;
    } else if (message && (message.content || message.receiveId || message.senderId)) {
      // 私信消息格式: 直接返回PrivateDTO或PrivateMessageVO
      type = 'message';
      data = message;
      
      // 确保time和dateTime字段兼容
      if (message.dateTime && !message.time) {
        data.time = message.dateTime; // 兼容旧的time字段
      } else if (message.time && !message.dateTime) {
        data.dateTime = message.time; // 兼容新的dateTime字段
      }
    } else {
      // 默认类型
      type = 'default';
      data = message;
    }
    
    // 触发对应的回调函数
    if (this.callbacks[type]) {
      this.callbacks[type].forEach(callback => {
        try {
          callback(data);
        } catch (error) {
          console.error(`执行${type}类型消息回调失败:`, error);
        }
      });
    }
  }

  /**
   * 发送消息
   * @param {string} type - 消息类型
   * @param {Object} data - 消息数据
   */
  send(type, data) {
    if (!this.isConnected || !this.socket) {
      console.error('WebSocket连接未建立，无法发送消息');
      return false;
    }

    try {
      let message;
      // 私信功能需要直接发送data对象，符合后端PrivateDTO格式
      if (type === 'message' && data && data.receiveId && data.content) {
        message = JSON.stringify(data);
      } else {
        // 其他类型消息使用{ type, data }格式
        message = JSON.stringify({ type, data });
      }
      this.socket.send(message);
      console.log('发送WebSocket消息:', message);
      return true;
    } catch (error) {
      console.error('发送WebSocket消息失败:', error);
      return false;
    }
  }

  /**
   * 订阅消息类型
   * @param {string} type - 消息类型
   * @param {Function} callback - 回调函数
   */
  subscribe(type, callback) {
    if (!this.callbacks[type]) {
      this.callbacks[type] = [];
    }
    this.callbacks[type].push(callback);
    
    // 返回取消订阅函数
    return () => {
      this.unsubscribe(type, callback);
    };
  }

  /**
   * 取消订阅消息类型
   * @param {string} type - 消息类型
   * @param {Function} callback - 回调函数
   */
  unsubscribe(type, callback) {
    if (this.callbacks[type]) {
      this.callbacks[type] = this.callbacks[type].filter(cb => cb !== callback);
      
      // 如果没有回调函数了，删除该消息类型
      if (this.callbacks[type].length === 0) {
        delete this.callbacks[type];
      }
    }
  }

  /**
   * 关闭WebSocket连接
   */
  disconnect() {
    if (this.socket) {
      this.socket.close(1000, '用户主动关闭连接');
      this.socket = null;
      this.isConnecting = false;
      this.isConnected = false;
      this.callbacks = {};
      this.reconnectAttempts = 0;
      console.log('WebSocket连接已关闭');
    }
  }

  /**
   * 检查连接状态
   * @returns {boolean} 是否已连接
   */
  getIsConnected() {
    return this.isConnected;
  }
}

// 导出单例实例
export default new WebSocketService();