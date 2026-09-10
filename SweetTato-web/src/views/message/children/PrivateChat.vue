<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="600px"
    :with-header="false"
    @close="handleClose"
  >
    <div class="private-chat">
      <!-- 头部 -->
      <header class="chat-header">
        <div class="header-left" @click="handleClose">
          <DArrowRight style="width: 1.2em; height: 1.2em; cursor: pointer" />
        </div>
        <div class="header-user">
          <el-avatar :src="friend.icon" :size="36" />
          <span class="header-name">{{ friend.username }}</span>
        </div>
        <div class="header-right"></div>
      </header>

      <div class="chat-divider"></div>

      <!-- 聊天记录 -->
      <main class="chat-main" ref="chatBodyRef">
        <div class="chat-record">
          <div v-for="(item, index) in chatList" :key="index" class="msg-wrapper">
            <!-- 对方消息 -->
            <div v-if="!item.isOwn" class="message-other">
              <el-avatar :src="friend.icon" :size="36" class="msg-avatar" />
              <div class="msg-body">
                <div class="msg-content">{{ item.content }}</div>
                <span class="msg-time">{{ item.time }}</span>
              </div>
            </div>
            <!-- 自己的消息 -->
            <div v-else class="message-self">
              <div class="msg-body">
                <div class="msg-content">{{ item.content }}</div>
                <span class="msg-time">{{ item.time }}{{ item.status ? ` · ${item.status}` : item.deliveryStatus === 2 ? " · 已读" : item.deliveryStatus === 1 ? " · 已送达" : "" }}</span>
              </div>
              <el-avatar :src="myIcon" :size="36" class="msg-avatar" />
            </div>
          </div>
        </div>
      </main>

      <!-- 输入区域 -->
      <footer class="chat-footer">
        <div class="chat-divider"></div>
        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button type="primary" size="small" class="send-btn" @click="sendMessage">发送</el-button>
        </div>
      </footer>
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref, watch, nextTick, onUnmounted } from "vue";
import { DArrowRight } from "@element-plus/icons-vue";
import { getPrivateChatHistory, syncPrivateChatHistory } from "@/api/im";
import { useUserStore } from "@/store/userStore";
import { useImStore } from "@/store/imStore";

const props = defineProps<{
  modelValue: boolean;
  friend: {
    userId: string;
    username: string;
    icon: string;
  };
}>();

const emit = defineEmits<{
  (e: "update:modelValue", val: boolean): void;
  (e: "sendWsMessage", message: string): void;
}>();

const userStore = useUserStore();
const imStore = useImStore();
const visible = ref(props.modelValue);
const chatList = ref<Array<any>>([]);
const maxMessageId = ref(0);
const retryTimers = new Map<string, any>();
const inputText = ref("");
const chatBodyRef = ref<HTMLElement | null>(null);

const myIcon = userStore.getUserInfo()?.avatar || "";
const currentUserId = userStore.getUserInfo()?.id || "";

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val;
    if (val) {
      fetchHistory();
    }
  }
);

watch(visible, (val) => {
  emit("update:modelValue", val);
});

// 监听WebSocket推送的私聊消息
watch(() => imStore.privateMessage, (msg: any) => {
  if (!msg) return;
  const friendId = props.friend.userId;
  if (msg.senderId === Number(friendId) || msg.senderId === Number(currentUserId)) {
    const exists = chatList.value.some(item => item.id === msg.messageId || (msg.clientMessageId && item.clientMessageId === msg.clientMessageId));
    if (!exists) {
      chatList.value.push({
        id: msg.messageId, clientMessageId: msg.clientMessageId,
        isOwn: msg.senderId === Number(currentUserId),
        content: msg.content,
        time: msg.dateTime,
      });
      if (msg.messageId) maxMessageId.value = Math.max(maxMessageId.value, msg.messageId);
      // [实时通信升级] 接收方确认已收到，服务端据此记录 DELIVERED
      emit("sendWsMessage", JSON.stringify({ type: "delivery_ack", messageId: msg.messageId }));
      scrollToBottom();
    }
  }
});

// [实时通信升级] WebSocket 恢复后按 afterId 补齐断线期间消息
watch(() => imStore.wsReconnectVersion, () => {
  if (!visible.value || !props.friend.userId) return;
  syncPrivateChatHistory(props.friend.userId, maxMessageId.value).then(res => {
    (res.data || []).forEach((item: any) => {
      if (!chatList.value.some(old => old.id === item.id)) chatList.value.push({ ...item, time: item.time });
      if (item.id) maxMessageId.value = Math.max(maxMessageId.value, item.id);
    });
    chatList.value.sort((a, b) => (a.id || 0) - (b.id || 0));
  });
});

// [实时通信升级] 将全局 ACK 与送达/已读事件映射到当前会话的 UI 状态。
watch(() => imStore.pendingMessages, () => {
  chatList.value.forEach(item => {
    if (item.clientMessageId && imStore.pendingMessages[item.clientMessageId]) {
      const pending = imStore.pendingMessages[item.clientMessageId];
      item.status = pending.status;
      item.id = pending.messageId || item.id;
      if (item.id) maxMessageId.value = Math.max(maxMessageId.value, item.id);
    }
  });
}, { deep: true });

watch(() => imStore.messageStatus, (status: any) => {
  if (!status) return;
  const item = chatList.value.find(message => message.id === status.messageId);
  if (item) { item.deliveryStatus = status.status === "read" ? 2 : 1; item.status = ""; }
});

const fetchHistory = () => {
  if (!props.friend.userId) return;
  getPrivateChatHistory(props.friend.userId).then((res) => {
    chatList.value = res.data || [];
    maxMessageId.value = chatList.value.reduce((m, x) => Math.max(m, x.id || 0), 0);
    sendReadAck();
    scrollToBottom();
  });
};

const sendReadAck = () => {
  if (maxMessageId.value > 0) {
    emit("sendWsMessage", JSON.stringify({ type: "read_ack", receiveId: Number(props.friend.userId), maxReadMessageId: maxMessageId.value }));
  }
};

const sendMessage = () => {
  const text = inputText.value.trim();
  if (!text) return;
  const message = {
    type: "text",
    // [实时通信升级] 重试必须复用该 ID，服务端以此保证幂等
    clientMessageId: crypto.randomUUID(),
    receiveId: Number(props.friend.userId),
    receiveName: props.friend.username,
    senderId: Number(currentUserId),
    senderIcon: myIcon,
    content: text,
  };
  emit("sendWsMessage", JSON.stringify(message));
  imStore.trackPendingMessage({ ...message, status: "SENDING" });
  chatList.value.push({
    clientMessageId: message.clientMessageId, status: "SENDING",
    isOwn: true,
    content: text,
    time: "刚刚",
  });
  const retry = (attempt = 0) => {
    const pending = imStore.pendingMessages[message.clientMessageId];
    if (!pending || pending.status !== "SENDING") return;
    if (attempt >= 3) { imStore.updatePendingMessage({ clientMessageId: message.clientMessageId, success: false }); return; }
    retryTimers.set(message.clientMessageId, setTimeout(() => {
      emit("sendWsMessage", JSON.stringify(message)); retry(attempt + 1);
    }, 5000));
  };
  retry();
  inputText.value = "";
  scrollToBottom();
};

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight;
    }
  });
};

const handleClose = () => {
  emit("sendWsMessage", JSON.stringify({
    type: "leave_chat",
    receiveId: Number(props.friend.userId),
    receiveName: props.friend.username,
    senderId: Number(currentUserId),
    senderIcon: myIcon,
    content: "",
  }));
  visible.value = false;
};

onUnmounted(() => {
  handleClose();
});
</script>

<style lang="less" scoped>
.private-chat {
  display: flex;
  flex-direction: column;
  height: 100%;

  .chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    flex-shrink: 0;

    .header-left {
      cursor: pointer;
      display: flex;
      align-items: center;
    }

    .header-user {
      display: flex;
      align-items: center;
      gap: 10px;

      .header-name {
        font-size: 16px;
        font-weight: 600;
        color: #333;
      }
    }

    .header-right {
      width: 20px;
    }
  }

  .chat-divider {
    height: 1px;
    background-color: #f0f0f0;
    flex-shrink: 0;
  }

  .chat-main {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;

    .chat-record {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .msg-wrapper {
      .message-other {
        display: flex;
        align-items: flex-start;
        gap: 10px;

        .msg-avatar {
          flex-shrink: 0;
        }

        .msg-body {
          max-width: 70%;

          .msg-content {
            background: #f5f5f5;
            padding: 10px 14px;
            border-radius: 4px 12px 12px 12px;
            font-size: 14px;
            color: #333;
            line-height: 1.5;
            word-break: break-word;
          }

          .msg-time {
            display: block;
            font-size: 12px;
            color: #999;
            margin-top: 4px;
            text-align: center;
          }
        }
      }

      .message-self {
        display: flex;
        align-items: flex-start;
        justify-content: flex-end;
        gap: 10px;

        .msg-avatar {
          flex-shrink: 0;
        }

        .msg-body {
          max-width: 70%;
          display: flex;
          flex-direction: column;
          align-items: flex-end;

          .msg-content {
            background: #ff2442;
            padding: 10px 14px;
            border-radius: 12px 4px 12px 12px;
            font-size: 14px;
            color: #fff;
            line-height: 1.5;
            word-break: break-word;
          }

          .msg-time {
            display: block;
            font-size: 12px;
            color: #999;
            margin-top: 4px;
            text-align: center;
          }
        }
      }
    }
  }

  .chat-footer {
    flex-shrink: 0;

    .input-area {
      padding: 12px 20px;
      display: flex;
      gap: 10px;
      align-items: flex-end;

      :deep(.el-textarea__inner) {
        font-size: 14px;
      }

      .send-btn {
        flex-shrink: 0;
        margin-bottom: 2px;
      }
    }
  }
}
</style>
