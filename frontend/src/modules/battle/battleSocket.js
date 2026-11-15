import { subscribeTopic, sendMessage, onSocketStatus } from '../../services/stompClient'

const BATTLE_TOPIC = '/user/queue/battle'

export const subscribeBattle = (handler) => subscribeTopic(BATTLE_TOPIC, handler)
export const send = sendMessage
export const onStatus = onSocketStatus
