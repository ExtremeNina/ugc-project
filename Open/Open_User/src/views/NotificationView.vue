<template>
  <div class="message-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>萌技术社区 - 私信</h1>
    </div>

    <div class="message-wrapper">
      <!-- 左侧对话列表 -->
      <aside class="conversation-list" :class="{ collapsed: isMobile && !showSidebar }">
        <div class="search-bar">
          <input type="text" placeholder="搜索对话" v-model="searchQuery">
        </div>
        
        <ul class="conversations">
          <li 
            v-for="conversation in filteredConversations" 
            :key="conversation.id"
            class="conversation-item"
            :class="{ active: selectedConversationId === conversation.id, unread: conversation.unread }"
            @click="selectConversation(conversation)"
          >
            <div class="avatar-container">
              <div class="user-avatar">
                <img 
                  v-if="conversation.user.avatar && conversation.user.avatar.startsWith('http')" 
                  :src="conversation.user.avatar" 
                  :alt="conversation.user.name"
                  class="avatar-image"
                />
                <span v-else>{{ conversation.user.avatar }}</span>
              </div>
              <div class="online-status" :class="conversation.user.status"></div>
            </div>
            <div class="conversation-info">
              <div class="conversation-header">
                <span class="user-name">{{ conversation.user.name }}</span>
                <div class="time-and-count">
                  <div v-if="conversation.lastMessage.time" class="list-message-time">{{ formatTime(conversation.lastMessage.time) }}</div>
                  <div v-if="conversation.notReadCount > 0" class="unread-count">{{ conversation.notReadCount }}</div>
                </div>
              </div>
              <div class="last-message">
                <span class="message-text">{{ conversation.lastMessage.text }}</span>
                <span class="unread-dot" v-if="conversation.unread"></span>
              </div>
            </div>
          </li>
        </ul>
      </aside>

      <!-- 右侧聊天窗口 -->
      <main class="chat-window">
        <!-- 聊天窗口头部 -->
        <div class="chat-header">
          <button class="mobile-menu-btn" @click="toggleSidebar" v-if="isMobile">
            <i class="fa fa-bars"></i>
          </button>
          <div v-if="selectedConversation" class="chat-header-info">
            <div class="user-avatar">
              <img 
                v-if="selectedConversation.user.avatar && selectedConversation.user.avatar.startsWith('http')" 
                :src="selectedConversation.user.avatar" 
                :alt="selectedConversation.user.name"
                class="avatar-image"
              />
              <span v-else>{{ selectedConversation.user.avatar }}</span>
            </div>
            <div class="user-info">
              <span class="user-name">{{ selectedConversation.user.name }}</span>
              <span class="user-status">{{ selectedConversation.user.status === 'online' ? '在线' : '离线' }}</span>
            </div>
          </div>
          <div v-else class="empty-state">
            <p>选择一个对话开始聊天</p>
          </div>
        </div>

        <!-- 聊天消息区域 -->
        <div class="chat-messages" ref="messagesContainer">
          <div v-if="selectedConversation" class="messages">
              <!-- 时间分组显示 -->
              <template v-for="(messageGroup, groupIndex) in groupedMessages" :key="groupIndex">
              <!-- 时间显示在消息组上方 -->
              <p class="message-time">{{ messageGroup.time }}</p>
              
              <!-- 消息组内的消息 -->
              <div 
                v-for="message in messageGroup.messages" 
                :key="message.id"
                class="message"
                :class="{ sent: message.sentByMe }"
              >
                <!-- 接收消息的头像 -->
                <div v-if="!message.sentByMe" class="message-avatar">
                  <div class="user-avatar">
                    <div 
                      v-if="selectedConversation.user.avatar && (selectedConversation.user.avatar.startsWith('http') || selectedConversation.user.avatar.startsWith('/'))" 
                      class="avatar-image"
                      :style="{ backgroundImage: `url(${selectedConversation.user.avatar})` }"
                    ></div>
                    <span v-else>{{ selectedConversation.user.avatar }}</span>
                  </div>
                </div>
                
                <div class="message-bubble">
                  <p class="message-text">{{ message.text }}</p>
                </div>
                
                <!-- 发送消息的头像 -->
                <div v-if="message.sentByMe" class="message-avatar">
                  <div class="user-avatar">
                    <div v-if="message.avatar && (message.avatar.startsWith('http') || message.avatar.startsWith('/'))" class="avatar-image" :style="{ backgroundImage: `url(${message.avatar})` }"></div>
                    <span v-else>{{ message.avatar || 'M' }}</span>
                  </div>
                </div>
              </div>
            </template>
          </div>
          <div v-else class="empty-messages">
            <p>请选择一个好友开始聊天</p>
          </div>
        </div>

        <!-- 聊天输入区域 -->
        <div v-if="selectedConversation" class="chat-input-area">
          <div class="input-toolbar">
            <button class="toolbar-btn" @click="attachImage">
              <i class="fa fa-image"></i>
            </button>
            <button class="toolbar-btn" @click="openEmojiPicker">
              <i class="fa fa-smile"></i>
            </button>
          </div>
          
          <!-- 表情包选择器 -->
          <div v-if="showEmojiPicker" class="emoji-picker">
            <div class="emoji-header">
              <h4>表情</h4>
              <button class="emoji-close" @click="closeEmojiPicker">×</button>
            </div>
            <div class="emoji-container">
              <button 
                v-for="(emoji, index) in emojis" 
                :key="index"
                class="emoji-item"
                @click="selectEmoji(emoji)"
              >
                {{ emoji }}
              </button>
            </div>
          </div>
          
          <div class="input-wrapper">
            <div
              class="editable-input"
              contenteditable="true"
              :data-placeholder="!newMessage ? '请输入消息内容' : ''"
              @input="handleInput"
              @compositionstart="isComposing = true"
              @compositionend="onCompositionEnd"
              @keyup.enter.ctrl="sendMessage"
            ></div>
            <div class="input-footer">
              <span class="char-count">{{ newMessage.length }}/500</span>
              <button class="send-button" @click="sendMessage" :disabled="!newMessage.trim() || newMessage.length > 500">
                发送
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import WebSocketService from '../services/websocketService.js';

export default {
  name: 'MessageView',
  data() {
    return {
      conversations: [], // 初始化为空数组，将从API获取数据
      selectedConversationId: null,
      selectedConversation: null,
      newMessage: '',
      searchQuery: '',
      isMobile: false,
      showSidebar: false,
      isComposing: false, // 输入法状态管理
      showEmojiPicker: false, // 表情包选择器显示状态
      currentUserId: null, // 当前用户ID，在组件初始化时获取并保存，避免被其他组件修改
      currentUser: { // 当前用户信息，包括头像
        avatar: 'M' // 默认头像为用户名称首字母
      },
      emojis: [ // 常用真实表情包
        '😀', '😃', '😄', '😁', '😆', '😅', '😂', '🤣', '😊', '😇',
        '🙂', '🙃', '😉', '😌', '😍', '🥰', '😘', '😗', '😙', '😚',
        '😋', '😛', '😝', '😜', '🤪', '🤨', '🧐', '🤓', '😎', '🤩',
        '🥳', '😏', '😒', '😞', '😔', '😟', '😕', '🙁', '☹️', '😣',
        '😖', '😫', '😩', '🥺', '😢', '😭', '😤', '😠', '😡', '🤬',
        '🤯', '😳', '🥵', '🥶', '😱', '😨', '😰', '😥', '😓', '🤗',
        '🤔', '🤭', '🤫', '🤥', '😶', '😐', '😑', '😬', '🙄', '😯',
        '😦', '😧', '😮', '😲', '🥱', '😴', '🤤', '😪', '😵', '🤐',
        '🥴', '🤢', '🤮', '🤧', '😷', '🤒', '🤕', '🤑', '🤠', '😈',
        '👿', '👹', '👺', '🤡', '💩', '👻', '💀', '☠️', '👽', '👾',
        '🤖', '🎃', '😺', '😸', '😹', '😻', '😼', '😽', '🙀', '😿',
        '😾', '👋', '🤚', '🖐️', '✋', '🖖', '👌', '🤌', '🤏', '✌️',
        '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️',
        '👍', '👎', '👊', '✊', '🤛', '🤜', '👏', '🙌', '👐', '🤲',
        '🤝', '🙏', '✍️', '💅', '🤳', '💪', '🦾', '🦿', '🦵', '🦶',
        '👂', '🦻', '👃', '🧠', '🫀', '🫁', '🦷', '🦴', '👀', '👁️',
        '👅', '👄', '🫦', '💋', '🩸', '❤️', '🧡', '💛', '💚', '💙',
        '💜', '🩷', '🩵', '🩶', '🖤', '🤍', '🤎', '💔', '❣️', '💕',
        '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️',
        '🕉️', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈',
        '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐', '♑', '♒', '♓'
      ],
      // WebSocket相关状态由全局WebSocketService管理
    }
  },
  computed: {
    filteredConversations() {
      if (!this.searchQuery) return this.conversations
      return this.conversations.filter(conv => 
        conv.user.name.toLowerCase().includes(this.searchQuery.toLowerCase())
      )
    },
    // 将消息按时间间隔（10分钟）分组
    groupedMessages() {
      if (!this.selectedConversation || !this.selectedConversation.messages) {
        return [];
      }

      const messages = this.selectedConversation.messages;
      if (messages.length === 0) {
        return [];
      }

      const groups = [];
      let currentGroup = {
        time: messages[0].time,
        messages: [messages[0]]
      };

      for (let i = 1; i < messages.length; i++) {
        const prevDate = new Date(messages[i - 1].time);
        const currentDate = new Date(messages[i].time);
        const diffMinutes = (currentDate - prevDate) / (1000 * 60);

        if (diffMinutes < 10) {
          // 时间间隔小于10分钟，添加到当前组
          currentGroup.messages.push(messages[i]);
        } else {
          // 时间间隔大于等于10分钟，创建新组
          groups.push(currentGroup);
          currentGroup = {
            time: messages[i].time,
            messages: [messages[i]]
          };
        }
      }

      // 添加最后一组
      groups.push(currentGroup);
      return groups;
    }
  },
  mounted() {
    // 设置浏览器标签页标题
    document.title = '萌技术社区 - 私信';
    // 初始化移动端检查
    this.checkMobile()
    window.addEventListener('resize', this.checkMobile)
    
    // 从API获取好友列表
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    // 优先从sessionStorage获取userId，因为从日志看sessionStorage中的userId是正确的16，而localStorage中的是错误的17
    const userId = sessionStorage.getItem('userId') || localStorage.getItem('userId');
    
    console.log('获取到的token:', token);
    console.log('获取到的userId:', userId);
    
    if (!token || !userId) {
      console.error('缺少认证信息');
      return;
    }
    
    // 保存当前用户ID到组件状态，避免被其他组件修改localStorage/sessionStorage影响
    this.currentUserId = Number(userId);
    console.log('组件状态保存的当前用户ID:', this.currentUserId);
    console.log('当前用户ID（数字类型）:', this.currentUserId);
    console.log('sessionStorage中的userId优先级更高，确保获取正确的用户ID');
    
    // 检查localStorage中的其他可能与用户ID相关的字段
    const storedUserId = localStorage.getItem('userId');
    const storedSessionId = sessionStorage.getItem('userId');
    const storedUserProfile = localStorage.getItem('userProfile');
    
    console.log('localStorage中的userId:', storedUserId);
    console.log('sessionStorage中的userId:', storedSessionId);
    
    if (storedUserProfile) {
      try {
        const userProfile = JSON.parse(storedUserProfile);
        console.log('localStorage中的userProfile:', userProfile);
        // 保存当前用户的头像信息
        this.currentUser.avatar = userProfile.avatar || userProfile.icon || (userProfile.nickname || userProfile.username || 'M').charAt(0);
        console.log('当前用户头像信息:', this.currentUser.avatar);
      } catch (e) {
        console.error('解析userProfile失败:', e);
      }
    } else {
      // 尝试从其他可能的localStorage键获取用户信息
      try {
        // 尝试从userInfo获取
        const userInfoStr = localStorage.getItem('userInfo');
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          console.log('从userInfo获取的用户信息:', userInfo);
          this.currentUser.avatar = userInfo.avatar || userInfo.icon || (userInfo.nickname || userInfo.username || 'M').charAt(0);
          console.log('从userInfo获取的当前用户头像:', this.currentUser.avatar);
        } else {
          // 尝试从user获取
          const userStr = localStorage.getItem('user');
          if (userStr) {
            const user = JSON.parse(userStr);
            console.log('从user获取的用户信息:', user);
            this.currentUser.avatar = user.avatar || user.icon || (user.nickname || user.username || 'M').charAt(0);
            console.log('从user获取的当前用户头像:', this.currentUser.avatar);
          }
        }
      } catch (e) {
        console.error('从其他localStorage键解析用户信息失败:', e);
      }
    }
    
    // 移除可能存在的'Bearer '前缀（如果后端已包含）
    const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
    
    console.log('清理后的token:', cleanToken);
    
    // 建立WebSocket连接
    WebSocketService.connect(cleanToken);
    
    // 先调用API获取正确的用户ID和头像
    this.getUserAvatar().then(() => {
      // 使用从API获取的正确用户ID来获取好友列表
      axios.get(`http://localhost:8081/api/private/${this.currentUserId}`, {
          headers: {
            Authorization: `Bearer ${cleanToken}`
        }
      })
      .then(response => {
      console.log('获取好友列表API完整响应:', response);
      console.log('获取好友列表API响应数据:', response.data);
      
      // 检查响应结构，实际数据可能在response.data.data中
      const friendList = response.data.data || response.data;
      console.log('实际好友列表数据:', friendList);
      
      // 将API返回的FriendVO数组转换为组件需要的conversations格式
      this.conversations = friendList.map(friend => {
        console.log('处理好友数据:', friend);
        
        // 确保使用好友的ID而不是当前用户的ID
        // 尝试不同的字段名，兼容API可能返回的不同结构
        const friendId = friend.friendId || friend.id || friend.userId;
        
        // 如果获取到的ID与当前用户ID相同，可能是API返回了错误的字段
        // 这种情况下尝试从其他可能的字段获取好友ID
        let finalFriendId = friendId;
        if (friendId === this.currentUserId) {
          console.warn('检测到好友ID与当前用户ID相同，尝试从其他字段获取好友ID:', friend);
          // 尝试其他可能的字段名
          finalFriendId = friend.toUserId || friend.fromUserId || friend.targetId || friendId;
        }
        
        // 再次检查，如果仍然是当前用户ID，记录详细日志
        if (finalFriendId === this.currentUserId) {
          console.error('获取好友ID失败，当前用户ID:', this.currentUserId, '好友数据:', friend);
        }
        
        return {
          id: finalFriendId,
          user: {
            id: finalFriendId,
            name: friend.userName || friend.name || '未知用户',
            avatar: friend.icon || friend.avatar || (friend.userName || '未知用户').charAt(0), // 使用icon或用户名首字母
            status: friend.isOnline ? 'online' : 'offline' // 使用API返回的isOnline字段
          },
          lastMessage: {
            text: friend.lastText || friend.lastMessage || '',
            time: friend.lastTime || new Date().toISOString().slice(0, 19).replace('T', ' ') // 使用后端提供的lastTime
          },
          notReadCount: friend.notReadCount || 0, // 使用API返回的未读消息数
          messages: [] // 初始消息为空数组，可根据需要从API获取
        }
      });
      
      console.log('转换后的对话列表:', this.conversations);
      
      // 验证并过滤掉ID与当前用户相同的错误好友
      const filteredConversations = this.conversations.filter(conv => {
        if (conv.id === this.currentUserId || conv.user.id === this.currentUserId) {
          console.error('发现ID与当前用户相同的错误好友，已过滤:', conv);
          return false;
        }
        return true;
      });
      
      // 如果过滤后有变化，使用过滤后的列表
      if (filteredConversations.length !== this.conversations.length) {
        console.log('过滤后的对话列表:', filteredConversations);
        this.conversations = filteredConversations;
      }
      

    })
    .catch(error => {
      console.error('获取好友列表失败:', error);
      // 输出完整错误信息
      if (error.response) {
        console.error('错误响应:', error.response.data);
        console.error('错误状态:', error.response.status);
        console.error('错误头信息:', error.response.headers);
      }
    });
  });
    
    // 订阅WebSocket消息
    this.subscribeToWebSocket()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.checkMobile)
    // 取消WebSocket消息订阅
    this.unsubscribeFromWebSocket()
  },
  methods: {
    // 订阅WebSocket消息
    subscribeToWebSocket() {
      // 订阅消息事件
      this.messageSubscription = WebSocketService.subscribe('message', (data) => {
        this.handleReceivedMessage(data);
      });
      
      // 订阅错误事件
      this.errorSubscription = WebSocketService.subscribe('error', (data) => {
        console.error('WebSocket错误消息:', data);
      });
    },
    
    // 取消WebSocket消息订阅
    unsubscribeFromWebSocket() {
      if (this.messageSubscription) {
        this.messageSubscription();
        this.messageSubscription = null;
      }
      
      if (this.errorSubscription) {
        this.errorSubscription();
        this.errorSubscription = null;
      }
    },
    
    // 处理收到的消息
    handleReceivedMessage(messageData) {
      console.log('收到WebSocket消息:', messageData);
      
      // 根据后端返回的PrivateMessageVO对象格式处理消息
      // 提取发送者ID（senderId）
      const senderId = messageData.senderId;
      // 提取接收者ID（receiveId）
      const receiveId = messageData.receiveId;
      // 提取消息内容（content）
      let content = messageData.content || messageData.text;
      
      // 如果content看起来像PrivateDTO格式的字符串，提取实际内容
      if (content && typeof content === 'string' && content.startsWith('PrivateDTO(')) {
        const contentMatch = content.match(/content=(.+?)\)/);
        if (contentMatch && contentMatch[1]) {
          content = contentMatch[1];
        }
      }
      
      // 提取消息时间（使用dateTime字段，兼容旧的time字段）
      const messageTime = messageData.dateTime || messageData.time || new Date().toISOString().slice(0, 19).replace('T', ' ');
      
      // 提取发送者名称和头像
      const senderName = messageData.senderName || messageData.userName || '未知用户';
      const senderIcon = messageData.senderIcon || messageData.icon || messageData.avatar || senderName.charAt(0);
      
      // 查找对应的对话（根据senderId查找，兼容旧的conversationId）
      let conversation = this.conversations.find(c => c.id === senderId || c.id === messageData.conversationId);
      
      // 如果没有找到对应对话，创建新对话
      if (!conversation) {
        console.log('未找到对应对话，创建新对话');
        conversation = {
          id: senderId,
          user: {
            id: senderId,
            name: senderName,
            avatar: senderIcon,
            status: 'online'
          },
          lastMessage: null,
          unread: true,
          messages: []
        };
        
        // 将新对话添加到列表开头
        this.conversations.unshift(conversation);
      } else {
        // 更新现有对话的用户信息（如果有新的信息）
        if (senderName !== '未知用户' && conversation.user.name === '未知用户') {
          conversation.user.name = senderName;
        }
        if (senderIcon !== senderName.charAt(0) && conversation.user.avatar === conversation.user.name.charAt(0)) {
          conversation.user.avatar = senderIcon;
        }
      }
      
      // 添加新消息到对话
      const newMessage = {
        id: messageData.id || Date.now(),
        text: content,
        time: messageTime,
        sentByMe: senderId === this.currentUserId, // 使用组件状态中的当前用户ID判断
        avatar: senderId === this.currentUserId ? this.currentUser.avatar : conversation.user.avatar // 设置头像
      };
      
      conversation.messages.push(newMessage);
      conversation.lastMessage = newMessage;
      
      // 如果不是当前选中的对话，标记为未读
      if (this.selectedConversationId !== conversation.id) {
        conversation.unread = true;
      } else {
        // 如果是当前选中的对话，滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },
    
    checkMobile() {
      this.isMobile = window.innerWidth < 768
      if (this.isMobile) {
        this.showSidebar = false
      }
    },
    toggleSidebar() {
      this.showSidebar = !this.showSidebar
    },
    selectConversation(conversation) {
      this.selectedConversationId = conversation.id
      this.selectedConversation = conversation
      this.selectedConversation.unread = false
      if (this.isMobile) {
        this.showSidebar = false
      }
      
      // 获取聊天历史记录
      this.fetchChatHistory(conversation.id)
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
      
      // 移除自动发送的join消息，只有在发送实际消息时才建立连接
      // console.log('已选择对话:', conversation.id)
    },
    
    // 获取聊天历史记录
    async fetchChatHistory(friendId) {
      try {
        // 获取token
        const token = localStorage.getItem('token') || sessionStorage.getItem('token')
        if (!token) {
          console.error('缺少认证信息')
          return
        }
        
        // 移除可能存在的'Bearer '前缀
        const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
        
        // 调用API获取聊天历史记录
        const response = await axios.get(`http://localhost:8081/api/private/ChatHistory/${friendId}`, {
          headers: {
            Authorization: `Bearer ${cleanToken}`
          }
        })
        
        console.log('获取聊天历史记录API响应:', response.data)
        
        // 检查响应结构
        const chatHistoryList = response.data.data || response.data
        
        // 转换聊天记录格式并添加到当前对话中
        const messages = chatHistoryList.map(message => ({
          id: message.id,
          text: message.content,
          time: message.time,
          sentByMe: message.isOwn,
          senderId: message.senderId,
          receiverId: message.receiverId,
          avatar: message.isOwn ? this.currentUser.avatar : this.selectedConversation.user.avatar
        }))
        
        // 更新当前对话的消息列表
        if (this.selectedConversation) {
          this.selectedConversation.messages = messages
          console.log('更新后的对话消息:', this.selectedConversation.messages)
          
          // 聊天历史加载完成后，滚动到底部显示最新消息
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        console.error('获取聊天历史记录失败:', error)
        // 输出完整错误信息
        if (error.response) {
          console.error('错误响应:', error.response.data)
          console.error('错误状态:', error.response.status)
          console.error('错误头信息:', error.response.headers)
        }
      }
    },
    sendMessage() {
      if (!this.selectedConversation || !this.newMessage.trim()) return
      
      const messageText = this.newMessage.trim()
      const messageTime = new Date().toISOString().slice(0, 19).replace('T', ' ')
      
      // 确保currentUser.avatar有正确的值
      if (!this.currentUser.avatar || this.currentUser.avatar === 'M') {
        try {
          // 再次尝试从localStorage获取头像信息
          const userInfoStr = localStorage.getItem('userInfo') || localStorage.getItem('userProfile') || localStorage.getItem('user');
          if (userInfoStr) {
            const userInfo = JSON.parse(userInfoStr);
            this.currentUser.avatar = userInfo.icon || userInfo.avatar || (userInfo.nickname || userInfo.username || 'M').charAt(0);
            console.log('发送消息时更新的当前用户头像:', this.currentUser.avatar);
          } else {
            // 如果没有用户信息，使用用户名首字母（假设用户名存在）
            // 这里可以根据实际情况调整，比如使用其他方式获取用户名
            const username = localStorage.getItem('username') || sessionStorage.getItem('username') || 'M';
            this.currentUser.avatar = username.charAt(0);
            console.log('使用用户名首字母作为头像:', this.currentUser.avatar);
          }
        } catch (e) {
          console.error('获取用户头像信息失败:', e);
        }
      }
      
      // 创建本地消息对象，包含当前用户头像信息
      const localMessage = {
        id: Date.now(),
        text: messageText,
        time: messageTime,
        sentByMe: true,
        avatar: this.currentUser.avatar || 'M' // 保存当前用户头像，确保历史消息头像一致
      }
      
      // 在本地添加消息（乐观更新）
      this.selectedConversation.messages.push(localMessage)
      this.selectedConversation.lastMessage = localMessage
      this.newMessage = ''
      
      // 清空contenteditable元素的内容
      const editableElement = this.$el.querySelector('.editable-input');
      if (editableElement) {
        editableElement.textContent = '';
      }
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
      
      // 获取当前用户的头像信息
      let senderIcon = this.currentUser.avatar; // 默认使用组件状态中的头像
      try {
        // 从localStorage获取用户信息，更新头像（如果有新的）
        const userInfoStr = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
        if (userInfoStr) {
          const userInfo = JSON.parse(userInfoStr);
          // 如果有新的头像信息，更新组件状态
          const newAvatar = userInfo.icon || userInfo.avatar || '';
          if (newAvatar && newAvatar !== this.currentUser.avatar) {
            this.currentUser.avatar = newAvatar;
            senderIcon = newAvatar;
            console.log('更新当前用户头像:', newAvatar);
          }
        }
      } catch (e) {
        console.error('解析用户信息失败:', e);
      }
      
      // 使用全局WebSocketService发送消息
      // 发送符合后端PrivateDTO格式的完整消息
      const messageData = {
        senderId: this.currentUserId, // 发送者ID（当前用户，使用组件状态）
        senderIcon: senderIcon, // 发送者头像
        receiveId: Number(this.selectedConversation.user.id), // 接收者ID
        receiveName: this.selectedConversation.user.name, // 接收者名称
        content: messageText // 消息内容
      };
      
      const success = WebSocketService.send('message', messageData);
      if (success) {
        console.log('WebSocket消息发送成功:', messageData);
      } else {
        console.warn('WebSocket未连接或消息发送失败，消息将在连接恢复后发送');
        // 可以添加离线消息队列逻辑
      }
    },
    attachFile() {
      // 附件功能实现
      alert('附件功能待实现')
    },
    handleInput(e) {
        // 获取纯文本内容并更新消息，避免HTML标签和输入法中间状态的影响
        this.newMessage = e.target.textContent || '';
      },
    onCompositionEnd(e) {
        this.isComposing = false;
        this.handleInput(e);
      },
    formatTime(timeStr) {
      // 改进的时间格式化
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
      if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
      if (diff < 259200000) return Math.floor(diff / 86400000) + '天前'
      // 超过3天直接显示日期
      return timeStr.slice(5, 10)
    },
    scrollToBottom() {
      const container = this.$refs.messagesContainer
      if (container) {
        container.scrollTop = container.scrollHeight
      }
    },
    // 附件功能实现（图片）
    attachImage() {
      alert('图片附件功能待实现');
    },
    // 打开表情包选择器
    openEmojiPicker() {
      this.showEmojiPicker = true;
    },
    // 关闭表情包选择器
    closeEmojiPicker() {
      this.showEmojiPicker = false;
    },
    // 选择表情包
    selectEmoji(emoji) {
      // 获取可编辑区域
      const editable = document.querySelector('.editable-input');
      if (editable) {
        // 保存当前选区
        const selection = window.getSelection();
        const range = selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
        
        // 在光标位置插入表情包
        document.execCommand('insertText', false, emoji);
        
        // 更新消息内容
        this.newMessage = editable.textContent || '';
        
        // 重新聚焦到可编辑区域
        editable.focus();
      } else {
        // 如果无法获取可编辑区域，直接添加到消息末尾
        this.newMessage += emoji;
      }
    },
    
    // 获取用户头像API方法
    async getUserAvatar() {
      try {
        // 获取token并设置请求头
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        const headers = {};
        
        if (token) {
          // 移除可能存在的'Bearer '前缀（如果后端已包含）
          const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token;
          headers['Authorization'] = `Bearer ${cleanToken}`;
          console.log('已设置请求头Authorization:', `Bearer ${cleanToken}`);
        }
        
        // 使用新的API端点获取头像路径
        const response = await axios.get('http://localhost:8081/api/community/user/avatar', { headers });
        console.log('获取用户头像API响应数据:', response.data);
        
        // 处理后端返回的Map集合
        if (response.data && response.data.data) {
          const userData = response.data.data;
          console.log('用户数据Map:', userData);
          
          // 设置头像路径
          if (userData.icon) {
            this.currentUser.avatar = userData.icon;
            console.log('API获取的头像路径:', userData.icon);
          }
          
          // 使用API返回的用户ID作为当前用户ID（最可靠的来源）
          if (userData.id) {
            const newUserId = Number(userData.id);
            if (newUserId !== this.currentUserId) {
              this.currentUserId = newUserId;
              console.log('从API获取的正确用户ID:', this.currentUserId);
              
              // 重新过滤好友列表，确保不显示用户自己
              this.filterConversations();
            }
          }
        }
      } catch (error) {
        console.error('获取用户头像失败:', error);
        // 保持默认头像
      }
    },
    
    // 过滤好友列表，确保不显示用户自己
    filterConversations() {
      // 验证并过滤掉ID与当前用户相同的错误好友
      const filteredConversations = this.conversations.filter(conv => {
        // 确保使用好友的ID而不是当前用户的ID
        const conversationId = Number(conv.id);
        if (conversationId === this.currentUserId) {
          console.error('发现ID与当前用户相同的错误好友，已过滤:', conv);
          return false;
        }
        return true;
      });
      
      // 如果过滤结果与原列表不同，则更新列表
      if (filteredConversations.length !== this.conversations.length) {
        console.log('过滤后的对话列表:', filteredConversations);
        this.conversations = filteredConversations;
        
        // 如果有选中的对话，检查是否被过滤掉了
        if (this.selectedConversation && !this.conversations.some(conv => conv.id === this.selectedConversation.id)) {
          // 如果选中的对话被过滤掉了，选择第一个对话
          this.selectedConversation = this.conversations[0] || null;
        }
      }
    }
  }
}
</script>

<style scoped>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.message-container {
  min-height: 100vh;
  width: 80%;
  max-width: 1400px;
  margin: 0 auto;
  background-color: #fff;
  color: var(--color-text-primary);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.2);
}

/* 页面头部 */
.page-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  box-shadow: var(--shadow-navbar);
}

.page-header h1 {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 消息包装器 */
.message-wrapper {
  display: flex;
  height: calc(100vh - 60px);
  background-color: var(--bg-card);
}

/* 左侧对话列表 */
.conversation-list {
  width: 300px;
  border-right: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  box-shadow: var(--shadow-navbar);
  height: auto;
}

.conversation-list.collapsed {
  position: absolute;
  left: 0;
  top: 60px;
  bottom: 0;
  z-index: 100;
  transform: translateX(-100%);
  background-color: var(--bg-tertiary);
}

.search-bar {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

.search-bar input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  background-color: var(--bg-card);
  color: var(--text-primary);
}

.search-bar input::placeholder {
  color: var(--text-secondary);
}

.search-bar input:focus {
  border-color: var(--color-primary);
  background-color: var(--bg-card);
}

.conversations {
  flex: 1;
  overflow-y: visible;
  background-color: var(--bg-tertiary);
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color 0.2s ease;
  background-color: var(--bg-tertiary);
}

.conversation-item:hover {
  background-color: var(--bg-card);
}

.conversation-item.active {
  background-color: var(--color-primary-transparent-20);
  border-left: 3px solid var(--color-primary);
}

.avatar-container {
  position: relative;
  margin-right: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 500;
  border: 2px solid #e0e0e0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  color: var(--color-primary);
  overflow: hidden;
}

.online-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid var(--bg-tertiary);
  transition: background-color 0.2s ease;
}

.online-status.online {
  background-color: #52c41a;
}

.online-status.offline {
  background-color: #d9d9d9;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.list-message-time {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 时间和未读消息数容器 */
.time-and-count {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: auto;
}

/* 未读消息数 */
.unread-count {
  font-size: 0.75rem;
  color: #ff4444;
  white-space: nowrap;
  margin-top: 2px;
  font-weight: bold;
}

.last-message {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.message-text {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-primary);
  flex-shrink: 0;
}

/* 右侧聊天窗口 */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-card);
}

.chat-header {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

.mobile-menu-btn {
  background: none;
  border: none;
  font-size: 18px;
  color: var(--text-primary);
  cursor: pointer;
  margin-right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
  display: none;
}

.mobile-menu-btn:hover {
  background-color: var(--bg-card);
}

.chat-header-info {
  display: flex;
  align-items: center;
}

.user-status {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
  margin-left: 8px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
}

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f5f5;
}

.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-secondary);
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  max-width: 80%;
  margin-bottom: 8px;
  animation: fadeIn 0.3s ease;
  align-items: flex-end;
}

.message-avatar {
  margin: 0 8px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.sent {
  margin-left: auto;
  justify-content: flex-end;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  position: relative;
  word-wrap: break-word;
  line-height: 1.4;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 仅保留聊天框的色彩方案 */
.message:not(.sent) .message-bubble {
  background-color: #fff;
  border-bottom-left-radius: 4px;
  border: 1px solid #e0e0e0;
}

.message.sent .message-bubble {
  background-color: #1890ff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 1px 3px rgba(24, 144, 255, 0.3);
}

/* 聊天框内的文字颜色使用深色 */
.message:not(.sent) .message-text {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
}

.message.sent .message-text {
  font-size: 14px;
  color: white;
  line-height: 1.4;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
}

/* 消息时间居中显示 */
.messages {
  position: relative;
}

.message-time {
  font-size: 11px;
  color: #666;
  font-weight: bold;
  text-align: center;
  margin: 0 0 8px 0;
  padding: 4px 12px;
  background-color: #f0f0f0;
  border-radius: 12px;
  display: inline-block;
  position: relative;
  left: 50%;
  transform: translateX(-50%);
  clear: both;
}

/* 聊天输入区域 */
.chat-input-area {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 -1px 3px rgba(0, 0, 0, 0.05);
}

.input-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.toolbar-btn {
  background: none;
  border: 1px solid #e0e0e0;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  background-color: #fafafa;
}

.toolbar-btn:hover {
  background-color: #f0f0f0;
  border-color: #d0d0d0;
  color: #333;
}

.input-wrapper {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.editable-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  color: #333;
  background-color: transparent;
  min-height: 40px;
  max-height: 200px;
  overflow-y: auto;
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.4;
  padding: 0;
  margin: 0;
}

.editable-input:focus {
  outline: none;
}

.editable-input:empty:before {
  content: attr(data-placeholder);
  color: #999;
  pointer-events: none;
}

/* 确保输入框在有内容时正确显示 */
.editable-input:not(:empty) {
  background-color: transparent;
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.char-count {
  font-size: 12px;
  color: #999;
}

.send-button {
  background-color: #1890ff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s ease;
}

.send-button:hover {
  background-color: #40a9ff;
}

.send-button:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.input-wrapper input::placeholder {
  color: #999;
}

.input-wrapper input::placeholder {
  color: #999;
}

/* 表情包选择器样式 */
.emoji-picker {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 12px;
  max-width: 100%;
  overflow: hidden;
}

.emoji-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fafafa;
}

.emoji-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.emoji-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.emoji-close:hover {
  background-color: #f0f0f0;
  color: #333;
}

.emoji-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-item {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
  padding: 0;
  margin: 0;
}

.emoji-item:hover {
  background-color: #f0f0f0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .mobile-menu-btn {
    display: block;
  }
  
  .conversation-list {
    position: absolute;
    left: 0;
    top: 60px;
    bottom: 0;
    z-index: 100;
  }
  
  .conversation-list:not(.collapsed) {
    transform: translateX(0);
  }
  
  /* 移动端表情包选择器样式 */
  .emoji-picker {
    position: absolute;
    bottom: 120px;
    left: 16px;
    right: 16px;
    z-index: 1000;
  }
}
</style>