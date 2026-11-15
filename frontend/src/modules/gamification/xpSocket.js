import { subscribeTopic } from '../../services/stompClient';

export const subscribeXp = (handler) => subscribeTopic('/user/queue/xp-event', handler);

