import { subscribeTopic, sendMessage, onSocketStatus } from '../../services/stompClient';

const SPEAKING_TOPIC = '/user/queue/speaking';

export const subscribeSpeaking = (handler) => subscribeTopic(SPEAKING_TOPIC, handler);
export const onStatus = onSocketStatus;

export const sendCreate = ({ topic, maxParticipants, isPrivate }) => {
  const body = { maxParticipants, isPrivate: Boolean(isPrivate) };
  if (topic && topic.trim()) body.topic = topic.trim();
  return sendMessage('/app/speaking/create', body);
};

export const sendJoin = (roomCode) => {
  return sendMessage('/app/speaking/join', { roomCode: roomCode.trim().toUpperCase() });
};

export const sendLeave = () => {
  return sendMessage('/app/speaking/leave', {});
};

export const sendActiveRoom = () => {
  return sendMessage('/app/speaking/active-room', {});
};

export const sendSignal = (roomCode, toUserId, type, payload) => {
  return sendMessage('/app/speaking/signal', { roomCode, toUserId, type, payload });
};

export const sendMic = (roomCode, muted) => {
  return sendMessage('/app/speaking/mic', { roomCode, muted });
};

export const sendMute = (roomCode, targetUserId) => {
  return sendMessage('/app/speaking/mute', { roomCode, targetUserId });
};

export const sendTransferHost = (roomCode, newHostId) => {
  return sendMessage('/app/speaking/transfer-host', { roomCode, newHostId });
};

export const sendEnd = (roomCode) => {
  return sendMessage('/app/speaking/end', { roomCode });
};

export const sendKick = (roomCode, userId) => {
  return sendMessage('/app/speaking/kick', { roomCode, userId });
};

export const sendUnban = (roomCode, userId) => {
  return sendMessage('/app/speaking/unban', { roomCode, userId });
};

export const sendChat = (roomCode, text) => {
  return sendMessage('/app/speaking/chat', { roomCode, text });
};

