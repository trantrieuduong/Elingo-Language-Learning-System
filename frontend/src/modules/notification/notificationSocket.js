import { subscribeTopic } from '../../services/stompClient';

const NOTIFICATION_TOPIC = '/user/queue/notifications';
export const subscribeNotifications = (handler) => subscribeTopic(NOTIFICATION_TOPIC, handler);
