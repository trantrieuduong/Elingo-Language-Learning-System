import { Client } from '@stomp/stompjs'

/**
 * Quản lý kết nối STOMP dùng chung cho toàn app.
 * Tự đăng ký lại các kênh khi reconnect và unsubscribe khi không còn handler.
 */
let client = null
let status = 'disconnected' // 'connecting' | 'connected' | 'disconnected'
const statusHandlers = new Set()
const topicHandlers = new Map() // destination -> Set(handler) (các handler đã đăng ký, được giữ lại khi reconnect)
const liveSubs = new Map() // destination -> StompSubscription (các subscription đang hoạt động)

const setStatus = (s) => { status = s; statusHandlers.forEach((h) => h(s)) }

// Chuyển URL API sang WebSocket (http -> ws, https -> wss)
const WS_URL = `${import.meta.env.VITE_API_URL.replace(/^http/, 'ws')}/ws`

// Đăng ký nhận tin nhắn từ destination nếu chưa được đăng ký
const attach = (destination) => {
  if (!client?.connected || liveSubs.has(destination)) return;
  const sub = client.subscribe(destination, (frame) => {
    let payload;
    try {
      payload = JSON.parse(frame.body);
    } catch {
      return;
    } // bỏ qua payload lỗi định dạng
    topicHandlers.get(destination)?.forEach((h) => h(payload));
  });
  liveSubs.set(destination, sub);
}

export const getSocketStatus = () => status

export const connectSocket = () => {
  if (client) return;
  client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    beforeConnect: () => {
      client.connectHeaders = { Authorization: `Bearer ${localStorage.getItem('accessToken')}` };
    },
    onConnect: () => {
      liveSubs.clear();
      topicHandlers.forEach((_, destination) => attach(destination)); // Đăng ký lại các kênh đã đăng ký trước đó
      setStatus('connected');
    },
    onWebSocketClose: () => {
      liveSubs.clear();
      setStatus('disconnected');
    },
    onStompError: () => {
      liveSubs.clear();
      setStatus('disconnected');
    },
  });
  setStatus('connecting');
  client.activate();
}

export const disconnectSocket = () => {
  if (!client) return;
  const c = client;
  client = null;
  liveSubs.clear(); // Xóa các subscription hiện tại, giữ lại danh sách handler đã đăng ký
  c.deactivate();
  setStatus('disconnected');
}

// Đăng ký nhận tin nhắn từ destination và trả về hàm hủy đăng ký
export const subscribeTopic = (destination, handler) => {
  if (!topicHandlers.has(destination)) topicHandlers.set(destination, new Set());
  topicHandlers.get(destination).add(handler);
  attach(destination);
  return () => {
    const set = topicHandlers.get(destination);
    if (!set) return;
    set.delete(handler);
    if (set.size === 0) {
      topicHandlers.delete(destination);
      liveSubs.get(destination)?.unsubscribe(); // Hủy subscription khi không còn handler nào
      liveSubs.delete(destination);
    }
  };
}

// Gửi message qua WebSocket nếu đã kết nối
export const sendMessage = (destination, body) => {
  if (!client?.connected) return false;
  client.publish({ destination, body: body === undefined ? '' : JSON.stringify(body) });
  return true;
}

// Đăng ký lắng nghe trạng thái WebSocket và trả hàm hủy
export const onSocketStatus = (handler) => {
  statusHandlers.add(handler);
  handler(status);
  return () => statusHandlers.delete(handler);
}

