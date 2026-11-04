/**
 * Quản lý kết nối STOMP và nhận sự kiện realtime cho Battle.
 * Chỉ kết nối khi người dùng truy cập trang Battle.
 */

import { Client } from '@stomp/stompjs';

const WS_URL = `${import.meta.env.VITE_API_URL.replace(/^http/, 'ws')}/ws`;

let client = null;
const messageHandlers = new Set();
const statusHandlers = new Set();

const setStatus = (status) => statusHandlers.forEach((h) => h(status)); // 'connecting' | 'connected' | 'disconnected'

export const connect = () => {
  if (client) return;
  client = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    beforeConnect: () => {
      client.connectHeaders = { Authorization: `Bearer ${localStorage.getItem('accessToken')}` };
    },
    onConnect: () => {
      client.subscribe('/user/queue/battle', (frame) => {
        try {
          const envelope = JSON.parse(frame.body); // { event, data }
          messageHandlers.forEach((h) => h(envelope));
        } catch {
          // Bỏ qua payload lỗi định dạng
        }
      });
      setStatus('connected');
    },
    onWebSocketClose: () => setStatus('disconnected'),
    onStompError: () => setStatus('disconnected'),
  });
  setStatus('connecting');
  client.activate();
};

export const disconnect = () => {
  if (!client) return;
  const c = client;
  client = null;
  c.deactivate();
  setStatus('disconnected');
};

// Trả true nếu gửi thành công, false nếu chưa connect
export const send = (destination, body) => {
  if (!client?.connected) return false;
  client.publish({ destination, body: body === undefined ? '' : JSON.stringify(body) });
  return true;
};

export const onMessage = (handler) => {
  messageHandlers.add(handler);
  return () => messageHandlers.delete(handler);
};

export const onStatus = (handler) => {
  statusHandlers.add(handler);
  return () => statusHandlers.delete(handler);
};
