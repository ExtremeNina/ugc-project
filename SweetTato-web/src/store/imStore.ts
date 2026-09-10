import { defineStore } from "pinia";
import { ref } from "vue";
import { store } from "@/store";

// 使用setup模式定义
export const imStore = defineStore("imStore", () => {
  const userList = ref<Array<any>>([]);

  const message = ref<any>({});

  const countMessage = ref({
    chatCount: 0,
    likeOrCollectionCount: 0,
    commentCount: 0,
    followCount: 0,
  });

  const privateMessage = ref<any>(null);

  const moderationResult = ref<any>(null);
  // [实时通信升级] WebSocket 重连序号，聊天窗口监听后执行增量同步
  const wsReconnectVersion = ref(0);
  const pendingMessages = ref<Record<string, any>>({});
  const messageStatus = ref<any>(null);


  const setUserList = (data: Array<any>) => {
    userList.value = data;
  };

  const setCountMessage = (data: any) => {
    countMessage.value = data;
  };

  const setMessage = (data: any) => {
    message.value = data;
  };

  const setPrivateMessage = (data: any) => {
    privateMessage.value = data;
  };

  const setModerationResult = (data: any) => {
    moderationResult.value = data;
  };
  const markWsReconnected = () => { wsReconnectVersion.value++; };
  const trackPendingMessage = (msg: any) => { pendingMessages.value[msg.clientMessageId] = msg; };
  const updatePendingMessage = (ack: any) => {
    const item = pendingMessages.value[ack.clientMessageId];
    if (item) { item.status = ack.success ? "SENT" : "FAILED"; item.messageId = ack.messageId; }
  };
  const setMessageStatus = (status: any) => { messageStatus.value = status; };

  return { userList, countMessage, message, privateMessage, moderationResult, wsReconnectVersion, pendingMessages, messageStatus, setUserList, setCountMessage, setMessage, setPrivateMessage, setModerationResult, markWsReconnected, trackPendingMessage, updatePendingMessage, setMessageStatus };
});

export function useImStore() {
  return imStore(store);
}
