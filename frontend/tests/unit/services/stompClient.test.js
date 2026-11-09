import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

const holder = vi.hoisted(() => ({ clients: [] }));

vi.mock('@stomp/stompjs', () => ({
  Client: class FakeClient {
    constructor(config) {
      this.config = config;
      this.connected = false;
      this.connectHeaders = undefined;
      this.callbacks = {};
      this.subscriptions = {};
      this.activate = vi.fn();
      this.deactivate = vi.fn();
      this.publish = vi.fn();
      this.subscribe = vi.fn((destination, callback) => {
        const subscription = { destination, unsubscribe: vi.fn() };
        this.callbacks[destination] = callback;
        this.subscriptions[destination] = subscription;
        return subscription;
      });
      holder.clients.push(this);
    }
  },
}));

describe('services/stompClient.js', () => {
  let stomp;

  const loadModule = async (apiUrl = 'http://localhost:8080') => {
    vi.resetModules();
    vi.stubEnv('VITE_API_URL', apiUrl);
    holder.clients.length = 0;
    return import('@/services/stompClient');
  };

  const currentClient = () => holder.clients.at(-1);

  /** Giả lập server chấp nhận kết nối */
  const simulateConnect = (client = currentClient()) => {
    client.connected = true;
    client.config.onConnect();
  };

  /** Giả lập server gửi một frame đến destination */
  const deliver = (destination, body, client = currentClient()) => {
    client.callbacks[destination]({ body: typeof body === 'string' ? body : JSON.stringify(body) });
  };

  beforeEach(async () => {
    localStorage.clear();
    stomp = await loadModule();
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  describe('initial state', () => {
    it('starts disconnected without creating a client', () => {
      expect(stomp.getSocketStatus()).toBe('disconnected');
      expect(holder.clients).toHaveLength(0);
    });
  });

  describe('connectSocket', () => {
    it('creates and activates a client and moves to connecting', () => {
      stomp.connectSocket();

      expect(holder.clients).toHaveLength(1);
      expect(currentClient().activate).toHaveBeenCalledTimes(1);
      expect(currentClient().config.reconnectDelay).toBe(3000);
      expect(stomp.getSocketStatus()).toBe('connecting');
    });

    it('builds the websocket url from an http api url', () => {
      stomp.connectSocket();

      expect(currentClient().config.brokerURL).toBe('ws://localhost:8080/ws');
    });

    it('builds a secure websocket url from an https api url', async () => {
      stomp = await loadModule('https://api.elingo.com');

      stomp.connectSocket();

      expect(currentClient().config.brokerURL).toBe('wss://api.elingo.com/ws');
    });

    it('does nothing when already connected or connecting', () => {
      stomp.connectSocket();
      stomp.connectSocket();

      expect(holder.clients).toHaveLength(1);
      expect(currentClient().activate).toHaveBeenCalledTimes(1);
    });

    it('sends the access token on every connection attempt', () => {
      localStorage.setItem('accessToken', 'token-1');
      stomp.connectSocket();
      const client = currentClient();

      client.config.beforeConnect();
      expect(client.connectHeaders).toEqual({ Authorization: 'Bearer token-1' });

      // Token mới sau khi refresh sẽ được dùng ở lần reconnect kế tiếp
      localStorage.setItem('accessToken', 'token-2');
      client.config.beforeConnect();
      expect(client.connectHeaders).toEqual({ Authorization: 'Bearer token-2' });
    });

    it('moves to connected when the broker accepts the connection', () => {
      stomp.connectSocket();

      simulateConnect();

      expect(stomp.getSocketStatus()).toBe('connected');
    });

    it('moves to disconnected when the websocket closes', () => {
      stomp.connectSocket();
      simulateConnect();

      currentClient().config.onWebSocketClose();

      expect(stomp.getSocketStatus()).toBe('disconnected');
    });

    it('moves to disconnected on a STOMP error', () => {
      stomp.connectSocket();
      simulateConnect();

      currentClient().config.onStompError();

      expect(stomp.getSocketStatus()).toBe('disconnected');
    });
  });

  describe('onSocketStatus', () => {
    it('calls the handler immediately with the current status', () => {
      const handler = vi.fn();

      stomp.onSocketStatus(handler);

      expect(handler).toHaveBeenCalledTimes(1);
      expect(handler).toHaveBeenCalledWith('disconnected');
    });

    it('notifies status changes in order', () => {
      const handler = vi.fn();
      stomp.onSocketStatus(handler);

      stomp.connectSocket();
      simulateConnect();
      currentClient().config.onWebSocketClose();

      expect(handler.mock.calls.map(([s]) => s)).toEqual([
        'disconnected',
        'connecting',
        'connected',
        'disconnected',
      ]);
    });

    it('stops notifying after unsubscribing', () => {
      const handler = vi.fn();
      const unsubscribe = stomp.onSocketStatus(handler);
      handler.mockClear();

      unsubscribe();
      stomp.connectSocket();

      expect(handler).not.toHaveBeenCalled();
    });

    it('supports several handlers', () => {
      const first = vi.fn();
      const second = vi.fn();
      stomp.onSocketStatus(first);
      stomp.onSocketStatus(second);

      stomp.connectSocket();

      expect(first).toHaveBeenLastCalledWith('connecting');
      expect(second).toHaveBeenLastCalledWith('connecting');
    });
  });

  describe('subscribeTopic', () => {
    it('waits for the connection before subscribing', () => {
      stomp.connectSocket();

      stomp.subscribeTopic('/user/queue/battle', vi.fn());

      expect(currentClient().subscribe).not.toHaveBeenCalled();
    });

    it('subscribes the registered topics once connected', () => {
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', vi.fn());
      stomp.subscribeTopic('/user/queue/xp', vi.fn());

      simulateConnect();

      expect(currentClient().subscribe).toHaveBeenCalledTimes(2);
      expect(currentClient().subscribe).toHaveBeenCalledWith('/user/queue/battle', expect.any(Function));
      expect(currentClient().subscribe).toHaveBeenCalledWith('/user/queue/xp', expect.any(Function));
    });

    it('subscribes immediately when already connected', () => {
      stomp.connectSocket();
      simulateConnect();

      stomp.subscribeTopic('/user/queue/battle', vi.fn());

      expect(currentClient().subscribe).toHaveBeenCalledWith('/user/queue/battle', expect.any(Function));
    });

    it('does nothing on the broker when there is no client', () => {
      expect(() => stomp.subscribeTopic('/user/queue/battle', vi.fn())).not.toThrow();
      expect(holder.clients).toHaveLength(0);
    });

    it('delivers the parsed payload to the handler', () => {
      const handler = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', handler);
      simulateConnect();

      deliver('/user/queue/battle', { event: 'queue:joined', data: { mode: 'TYPING' } });

      expect(handler).toHaveBeenCalledTimes(1);
      expect(handler).toHaveBeenCalledWith({ event: 'queue:joined', data: { mode: 'TYPING' } });
    });

    it('delivers messages to every handler of the same destination with one subscription', () => {
      const first = vi.fn();
      const second = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', first);
      stomp.subscribeTopic('/user/queue/battle', second);
      simulateConnect();

      deliver('/user/queue/battle', { event: 'x' });

      expect(currentClient().subscribe).toHaveBeenCalledTimes(1);
      expect(first).toHaveBeenCalledWith({ event: 'x' });
      expect(second).toHaveBeenCalledWith({ event: 'x' });
    });

    it('does not mix messages between destinations', () => {
      const battle = vi.fn();
      const xp = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', battle);
      stomp.subscribeTopic('/user/queue/xp', xp);
      simulateConnect();

      deliver('/user/queue/xp', { amount: 10 });

      expect(xp).toHaveBeenCalledWith({ amount: 10 });
      expect(battle).not.toHaveBeenCalled();
    });

    it('ignores frames with a malformed body', () => {
      const handler = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', handler);
      simulateConnect();

      expect(() => deliver('/user/queue/battle', 'not-json{')).not.toThrow();

      expect(handler).not.toHaveBeenCalled();
    });

    it('keeps delivering after a malformed frame', () => {
      const handler = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', handler);
      simulateConnect();

      deliver('/user/queue/battle', 'not-json{');
      deliver('/user/queue/battle', { ok: true });

      expect(handler).toHaveBeenCalledTimes(1);
      expect(handler).toHaveBeenCalledWith({ ok: true });
    });
  });

  describe('unsubscribe', () => {
    it('keeps the broker subscription while other handlers remain', () => {
      const first = vi.fn();
      const second = vi.fn();
      stomp.connectSocket();
      const unsubscribeFirst = stomp.subscribeTopic('/user/queue/battle', first);
      stomp.subscribeTopic('/user/queue/battle', second);
      simulateConnect();

      unsubscribeFirst();
      deliver('/user/queue/battle', { event: 'x' });

      expect(currentClient().subscriptions['/user/queue/battle'].unsubscribe).not.toHaveBeenCalled();
      expect(first).not.toHaveBeenCalled();
      expect(second).toHaveBeenCalledTimes(1);
    });

    it('cancels the broker subscription when the last handler is removed', () => {
      stomp.connectSocket();
      const unsubscribe = stomp.subscribeTopic('/user/queue/battle', vi.fn());
      simulateConnect();

      unsubscribe();

      expect(currentClient().subscriptions['/user/queue/battle'].unsubscribe).toHaveBeenCalledTimes(1);
    });

    it('subscribes again when a new handler registers after the last one left', () => {
      stomp.connectSocket();
      const unsubscribe = stomp.subscribeTopic('/user/queue/battle', vi.fn());
      simulateConnect();
      unsubscribe();
      const handler = vi.fn();

      stomp.subscribeTopic('/user/queue/battle', handler);
      deliver('/user/queue/battle', { event: 'again' });

      expect(currentClient().subscribe).toHaveBeenCalledTimes(2);
      expect(handler).toHaveBeenCalledWith({ event: 'again' });
    });

    it('can be called more than once safely', () => {
      stomp.connectSocket();
      const unsubscribe = stomp.subscribeTopic('/user/queue/battle', vi.fn());
      simulateConnect();

      unsubscribe();

      expect(() => unsubscribe()).not.toThrow();
      expect(currentClient().subscriptions['/user/queue/battle'].unsubscribe).toHaveBeenCalledTimes(1);
    });

    it('does not subscribe a removed topic after reconnecting', () => {
      stomp.connectSocket();
      const unsubscribe = stomp.subscribeTopic('/user/queue/battle', vi.fn());
      unsubscribe();

      simulateConnect();

      expect(currentClient().subscribe).not.toHaveBeenCalled();
    });

    it('does not fail when unsubscribing before the connection is established', () => {
      stomp.connectSocket();
      const unsubscribe = stomp.subscribeTopic('/user/queue/battle', vi.fn());

      expect(() => unsubscribe()).not.toThrow();
    });
  });

  describe('reconnect', () => {
    it('subscribes the topics again after the websocket reconnects', () => {
      const handler = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', handler);
      simulateConnect();
      expect(currentClient().subscribe).toHaveBeenCalledTimes(1);

      currentClient().config.onWebSocketClose();
      currentClient().connected = false;
      simulateConnect();

      expect(currentClient().subscribe).toHaveBeenCalledTimes(2);
      deliver('/user/queue/battle', { event: 'after-reconnect' });
      expect(handler).toHaveBeenCalledWith({ event: 'after-reconnect' });
    });

    it('subscribes again after a STOMP error followed by a new connection', () => {
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', vi.fn());
      simulateConnect();

      currentClient().config.onStompError();
      simulateConnect();

      expect(currentClient().subscribe).toHaveBeenCalledTimes(2);
    });

    it('does not create duplicate subscriptions when topics are added after reconnecting', () => {
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', vi.fn());
      simulateConnect();
      currentClient().config.onWebSocketClose();
      simulateConnect();

      stomp.subscribeTopic('/user/queue/battle', vi.fn());

      expect(currentClient().subscribe).toHaveBeenCalledTimes(2);
    });

    it('delivers messages once per handler after several reconnects', () => {
      const handler = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', handler);
      simulateConnect();
      currentClient().config.onWebSocketClose();
      simulateConnect();
      currentClient().config.onWebSocketClose();
      simulateConnect();

      deliver('/user/queue/battle', { event: 'x' });

      expect(handler).toHaveBeenCalledTimes(1);
    });
  });

  describe('sendMessage', () => {
    it('returns false when there is no client', () => {
      expect(stomp.sendMessage('/app/battle/queue/join', { mode: 'TYPING' })).toBe(false);
    });

    it('returns false while the client is not connected', () => {
      stomp.connectSocket();

      expect(stomp.sendMessage('/app/battle/queue/join', { mode: 'TYPING' })).toBe(false);
      expect(currentClient().publish).not.toHaveBeenCalled();
    });

    it('publishes the body as JSON when connected', () => {
      stomp.connectSocket();
      simulateConnect();

      const result = stomp.sendMessage('/app/battle/queue/join', { mode: 'TYPING' });

      expect(result).toBe(true);
      expect(currentClient().publish).toHaveBeenCalledWith({
        destination: '/app/battle/queue/join',
        body: JSON.stringify({ mode: 'TYPING' }),
      });
    });

    it('publishes an empty body when no body is given', () => {
      stomp.connectSocket();
      simulateConnect();

      stomp.sendMessage('/app/battle/queue/leave');

      expect(currentClient().publish).toHaveBeenCalledWith({
        destination: '/app/battle/queue/leave',
        body: '',
      });
    });

    it('serializes falsy bodies instead of dropping them', () => {
      stomp.connectSocket();
      simulateConnect();

      stomp.sendMessage('/app/test', 0);
      stomp.sendMessage('/app/test', null);

      expect(currentClient().publish).toHaveBeenNthCalledWith(1, { destination: '/app/test', body: '0' });
      expect(currentClient().publish).toHaveBeenNthCalledWith(2, { destination: '/app/test', body: 'null' });
    });

    it('returns false after the connection is lost', () => {
      stomp.connectSocket();
      simulateConnect();
      currentClient().connected = false;
      currentClient().config.onWebSocketClose();

      expect(stomp.sendMessage('/app/battle/queue/leave')).toBe(false);
    });
  });

  describe('disconnectSocket', () => {
    it('does nothing when there is no client', () => {
      const handler = vi.fn();
      stomp.onSocketStatus(handler);
      handler.mockClear();

      stomp.disconnectSocket();

      expect(handler).not.toHaveBeenCalled();
      expect(stomp.getSocketStatus()).toBe('disconnected');
    });

    it('deactivates the client and moves to disconnected', () => {
      stomp.connectSocket();
      simulateConnect();
      const client = currentClient();

      stomp.disconnectSocket();

      expect(client.deactivate).toHaveBeenCalledTimes(1);
      expect(stomp.getSocketStatus()).toBe('disconnected');
    });

    it('notifies the status handlers', () => {
      stomp.connectSocket();
      simulateConnect();
      const handler = vi.fn();
      stomp.onSocketStatus(handler);
      handler.mockClear();

      stomp.disconnectSocket();

      expect(handler).toHaveBeenCalledWith('disconnected');
    });

    it('cannot send messages after disconnecting', () => {
      stomp.connectSocket();
      simulateConnect();

      stomp.disconnectSocket();

      expect(stomp.sendMessage('/app/battle/queue/leave')).toBe(false);
    });

    it('allows connecting again with a new client', () => {
      stomp.connectSocket();
      stomp.disconnectSocket();

      stomp.connectSocket();

      expect(holder.clients).toHaveLength(2);
      expect(holder.clients[1].activate).toHaveBeenCalledTimes(1);
      expect(stomp.getSocketStatus()).toBe('connecting');
    });

    it('keeps the registered handlers and subscribes them again on the next connection', () => {
      const handler = vi.fn();
      stomp.connectSocket();
      stomp.subscribeTopic('/user/queue/battle', handler);
      simulateConnect();

      stomp.disconnectSocket();
      stomp.connectSocket();
      simulateConnect();

      expect(currentClient()).toBe(holder.clients[1]);
      expect(currentClient().subscribe).toHaveBeenCalledWith('/user/queue/battle', expect.any(Function));
      deliver('/user/queue/battle', { event: 'x' });
      expect(handler).toHaveBeenCalledWith({ event: 'x' });
    });

    it('does not subscribe on the old client after disconnecting', () => {
      stomp.connectSocket();
      simulateConnect();
      const oldClient = currentClient();
      stomp.disconnectSocket();

      stomp.subscribeTopic('/user/queue/battle', vi.fn());

      expect(oldClient.subscribe).not.toHaveBeenCalled();
    });
  });
});
