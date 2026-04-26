import io from 'socket.io-client';

type SocketCallback = (_data: any) => void;
type RoomListCallback = ((_payload: { type: string; data: any }) => void) | null;

class SocketService {
    socket: any;
    connected: boolean;
    reconnectAttempts: number;
    maxReconnectAttempts: number;
    listeners: Map<string, SocketCallback[]>;
    rooms: Set<string | number>;
    _subscribedRoomList: boolean;
    _roomListCallback: RoomListCallback;
    _onRoomCreated: SocketCallback | null;
    _onRoomDeleted: SocketCallback | null;
    _onRoomUpdated: SocketCallback | null;
    _roomCallbacks: Map<string | number, SocketCallback>;
    _roomHandlers: Map<string | number, Record<string, SocketCallback>>;

    constructor() {
        this.socket = null;
        this.connected = false;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.listeners = new Map();
        this.rooms = new Set();
        this._subscribedRoomList = false;
        this._roomListCallback = null;
        this._onRoomCreated = null;
        this._onRoomDeleted = null;
        this._onRoomUpdated = null;
        this._roomCallbacks = new Map();
        this._roomHandlers = new Map();
    }

    connect(token: string | null = null) {
        return new Promise<void>((resolve, reject) => {
            if (this.connected && this.socket) {
                return resolve();
            }

            if (this.socket) {
                this.disconnect();
            }

            const socketUrl = window.location.hostname === 'localhost'
                ? 'http://localhost:9093'
                : 'https://socket.dungmetri.io.vn';

            const connectionOptions: any = {
                transports: ['polling', 'websocket'],
                reconnection: true,
                reconnectionAttempts: this.maxReconnectAttempts,
                reconnectionDelay: 1000,
                reconnectionDelayMax: 5000,
                timeout: 20000,
                upgrade: true,
                rememberUpgrade: true,
                withCredentials: true,
                forceNew: false
            };
            if (token) {
                connectionOptions.query = { token };
            }

            this.socket = io(socketUrl, connectionOptions);

            this.socket.on('connect', () => {
                this.connected = true;
                this.reconnectAttempts = 0;
                this._setupGlobalListeners();

                // Đăng ký lại broadcast danh sách phòng nếu đã đăng ký trước đó
                if (this._subscribedRoomList && this._roomListCallback) {
                    try {
                        this.subscribeToRoomList(this._roomListCallback);
                    } catch {
                    }
                }

                // Khôi phục trạng thái phòng khi reconnect
                this.rooms.forEach(roomId => {
                    this.joinRoom(roomId);
                });

                resolve();
            });

            this.socket.on('connect_error', (_error) => {
                this.reconnectAttempts++;
                if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                    reject(new Error('Failed to connect after maximum attempts'));
                }
            });

            this.socket.on('error', (_error) => {
            });

            this.socket.on('reconnect', (_attemptNumber) => {
                this.connected = true;
                this.reconnectAttempts = 0;
                
                if (this._subscribedRoomList && this._roomListCallback) {
                    this.subscribeToRoomList(this._roomListCallback);
                }
            });

            this.socket.on('reconnect_failed', () => {
                reject(new Error('Reconnection failed'));
            });
        });
    }

    _setupGlobalListeners() {
        this.listeners.forEach((callbacks, event) => {
            callbacks.forEach(callback => this.socket.off(event, callback));
        });
        this.listeners.clear();


        this.on('roomCreated', (data) => {
            if (this._onRoomCreated) this._onRoomCreated(data);
        });

        this.on('roomDeleted', (data) => {
            if (this._onRoomDeleted) this._onRoomDeleted(data);
        });

        this.on('roomUpdated', (data) => {
            if (this._onRoomUpdated) this._onRoomUpdated(data);
        });

        this.on('connection-confirmed', () => {
        });

        this.on('subscription-confirmed', () => {
        });

        this.on('join-room-success', () => {
        });

        this.on('join-room-error', () => {
        });

        this.on('leave-room-success', () => {
        });

        this.on('leave-room-error', () => {
        });

        this.on('start-game-success', () => {
        });

        this.on('start-game-error', () => {
        });
    }

    setOnRoomCreated(callback: SocketCallback) {
        this._onRoomCreated = callback;
    }

    setOnRoomDeleted(callback: SocketCallback) {
        this._onRoomDeleted = callback;
    }

    setOnRoomUpdated(callback: SocketCallback) {
        this._onRoomUpdated = callback;
    }

    disconnect() {
        if (this.socket) {
            this.rooms.forEach(room => {
                this.leaveRoom(room);
            });

            this.socket.removeAllListeners();
            this.socket.disconnect();
            this.socket = null;
            this.connected = false;
            this.listeners.clear();
            this.rooms.clear();
        }
    }

    emit(event: string, data: any = null, callback: SocketCallback | null = null) {
        if (!this.socket || !this.connected) {
            return;
        }

        if (callback) {
            this.socket.emit(event, data, callback);
        } else {
            this.socket.emit(event, data);
        }
    }

    on(event: string, callback: SocketCallback) {
        if (!this.socket) {
            return;
        }

        this.socket.on(event, callback);

        // Lưu tham chiếu listener để dọn dẹp
        if (!this.listeners.has(event)) {
            this.listeners.set(event, []);
        }
        this.listeners.get(event).push(callback);
    }

    off(event: string, callback: SocketCallback | null = null) {
        if (!this.socket) return;

        if (callback) {
            this.socket.off(event, callback);
        } else {
            this.socket.off(event);
        }

        // Dọn dẹp các tham chiếu đã lưu
        if (this.listeners.has(event)) {
            if (callback) {
                const callbacks = this.listeners.get(event);
                const index = callbacks.indexOf(callback);
                if (index > -1) {
                    callbacks.splice(index, 1);
                }
            } else {
                this.listeners.delete(event);
            }
        }
    }

    once(event: string, callback: SocketCallback) {
        if (!this.socket) {
            return;
        }

        this.socket.once(event, (data) => {
            callback(data);
        });
    }

    joinRoom(roomCodeOrId: string | number, callback?: SocketCallback) {
        if (!this.socket || !this.connected) {
            callback?.({ success: false, error: 'Socket not connected' });
            return;
        }

        this.rooms.add(roomCodeOrId);

        const timeout = setTimeout(() => {
            callback?.({ success: false, error: 'Join room timeout' });
        }, 10000);

        // [FIXED] Backend mong đợi roomCode, không phải roomId
        const joinData = { roomCode: String(roomCodeOrId) };

        // Lắng nghe sự kiện thành công/lỗi
        const successHandler = (data) => {
            clearTimeout(timeout);
            this.off('join-room-success', successHandler);
            this.off('join-room-error', errorHandler);
            callback?.({ success: true, ...data });
        };

        const errorHandler = (data) => {
            clearTimeout(timeout);
            this.off('join-room-success', successHandler);
            this.off('join-room-error', errorHandler);
            this.rooms.delete(roomCodeOrId); // SỬA: Dùng roomCodeOrId thay vì roomId
            callback?.({ success: false, error: data.message || 'Failed to join room' });
        };

        this.once('join-room-success', successHandler);
        this.once('join-room-error', errorHandler);

        this.emit('join-room', joinData);
    }

    leaveRoom(roomId: string | number, callback?: SocketCallback) {
        if (!this.socket || !this.connected) {
            callback?.({ success: false, error: 'Socket not connected' });
            return;
        }

        this.rooms.delete(roomId);

        const timeout = setTimeout(() => {
            callback?.({ success: false, error: 'Leave room timeout' });
        }, 10000);

        // Listen for success/error events
        const successHandler = (data) => {
            clearTimeout(timeout);
            this.off('leave-room-success', successHandler);
            this.off('leave-room-error', errorHandler);
            callback?.({ success: true, ...data });
        };

        const errorHandler = (data) => {
            clearTimeout(timeout);
            this.off('leave-room-success', successHandler);
            this.off('leave-room-error', errorHandler);
            callback?.({ success: false, error: data.message || 'Failed to leave room' });
        };

        this.once('leave-room-success', successHandler);
        this.once('leave-room-error', errorHandler);

        this.emit('leave-room', { roomId });
    }

    startGame(roomId: string | number, callback?: SocketCallback) {
        if (!this.socket || !this.connected) {
            callback?.({ success: false, error: 'Socket not connected' });
            return;
        }

        this.emit('start-game', { roomId }, (response) => {
            callback?.(response);
        });
    }

    submitAnswer(roomId: string | number, questionId: string | number, answerId: string | number) {
        if (!this.socket || !this.connected) {
            return;
        }

        this.emit('submit-answer', {
            roomId: roomId,
            questionId: questionId,
            answerId: answerId
        });
    }

    nextQuestion(roomId: string | number) {
        if (!this.socket || !this.connected) {
            return;
        }

        this.emit('next-question', { roomId });
    }

    subscribeToRoomList(callback: RoomListCallback) {
        if (!this.socket || !this.connected) {
            this._subscribedRoomList = true;
            this._roomListCallback = callback;
            return;
        }

        this._subscribedRoomList = true;
        this._roomListCallback = callback;

        if (!callback) {
            return;
        }

        // Tránh đăng ký listener trùng khi reconnect hoặc remount.
        this.off('room-created');
        this.off('room-deleted');
        this.off('room-updated');

        this.emit('subscribe-room-list');

        this.on('room-created', (data) => {
            callback({ type: 'CREATE_ROOM', data });
        });

        this.on('room-deleted', (data) => {
            callback({ type: 'ROOM_DELETED', data });
        });

        this.on('room-updated', (data) => {
            callback({ type: 'ROOM_UPDATED', data });
        });

    }

    unsubscribeFromRoomList() {
        try {
            this.emit('unsubscribe-room-list');
        } catch {}
        this._subscribedRoomList = false;
        this._roomListCallback = null;

        this.off('room-created');
        this.off('room-deleted');
        this.off('room-updated');
    }

    /**
     * Đăng ký sự kiện phòng - khớp chính xác với backend
     */
    subscribeToRoom(roomId: string | number, callback: SocketCallback) {

        this.unsubscribeFromRoom(roomId);

        this._roomCallbacks = this._roomCallbacks || new Map();
        this._roomCallbacks.set(roomId, callback);

        //Khớp CHÍNH XÁC tên sự kiện backend từ RoomEventListener.java
        const handlePlayerJoined = (data) => {
            if (data.roomId === Number(roomId)) {
                callback({ type: 'PLAYER_JOINED', data });
            }
        };

        const handlePlayerLeft = (data) => {
            if (data.roomId === Number(roomId)) {
                callback({ type: 'PLAYER_LEFT', data });
            }
        };

        const handleRoomPlayers = (data) => {
            if (data.roomId === Number(roomId)) {
                callback({ type: 'ROOM_PLAYERS_UPDATED', data });
            }
        };

        const handleGameStarted = (data) => {
            if (data.roomId === Number(roomId)) {
                callback({ type: 'GAME_STARTED', data });
            }
        };

        const handlePlayerKicked = (data) => {
            if (data.roomId === Number(roomId)) {
                callback({ type: 'PLAYER_KICKED', data });
            }
        };

        // SỬA SỰ KIỆN THIẾU: Thêm handler host-changed
        const handleHostChanged = (data) => {
            if (data.roomId === Number(roomId)) {
                callback({ type: 'HOST_CHANGED', data });
            }
        };

        this._roomHandlers = this._roomHandlers || new Map();
        this._roomHandlers.set(roomId, {
            'player-joined': handlePlayerJoined,
            'player-left': handlePlayerLeft,
            'room-players': handleRoomPlayers,
            'room-players-updated': handleRoomPlayers,
            'game-started': handleGameStarted,
            'player-kicked': handlePlayerKicked,
            'host-changed': handleHostChanged
        });

        // Đăng ký listeners - PHẢI khớp chính xác tên sự kiện backend
        this.on('player-joined', handlePlayerJoined);
        this.on('player-left', handlePlayerLeft);
        this.on('room-players', handleRoomPlayers);
        this.on('room-players-updated', handleRoomPlayers);
        this.on('game-started', handleGameStarted);
        this.on('player-kicked', handlePlayerKicked);
        this.on('host-changed', handleHostChanged);
    }

    unsubscribeFromRoom(roomId: string | number) {
        if (!this._roomHandlers) return;
        
        const handlers = this._roomHandlers.get(roomId);
        if (handlers) {
            Object.entries(handlers).forEach(([event, handler]) => {
                this.off(event, handler);
            });
            
            this._roomHandlers.delete(roomId);
        }
        
        if (this._roomCallbacks) {
            this._roomCallbacks.delete(roomId);
        }
    }

    isConnected() {
        return this.connected && this.socket?.connected;
    }

    getSocketId() {
        return this.socket?.id || null;
    }

    getJoinedRooms() {
        return new Set(this.rooms);
    }

    // Event helper methods
    onPlayerJoined(callback: SocketCallback) {
        this.on('player-joined', callback);
    }

    onPlayerLeft(callback: SocketCallback) {
        this.on('player-left', callback);
    }

    onPlayerKicked(callback: SocketCallback) {
        this.on('player-kicked', callback);
    }

    onRoomPlayers(callback: SocketCallback) {
        this.on('room-players', callback);
    }

    onGameStarted(callback: SocketCallback) {
        this.on('game-started', callback);
    }

    onQuestionStarted(callback: SocketCallback) {
        this.on('question-started', callback);
    }

    onNextQuestion(callback: SocketCallback) {
        this.on('next-question', callback);
    }

    onAnswerSubmitted(callback: SocketCallback) {
        this.on('answer-submitted', callback);
    }

    onGameFinished(callback: SocketCallback) {
        this.on('game-ended', callback);
        this.on('game-finished', callback);
    }

    onError(callback: SocketCallback) {
        this.on('error', callback);
    }

    onRoomCreated(callback: SocketCallback) {
        this.on('roomCreated', callback);
    }

    onRoomDeleted(callback: SocketCallback) {
        this.on('roomDeleted', callback);
    }

    onRoomUpdated(callback: SocketCallback) {
        this.on('roomUpdated', callback);
    }
}

const socketService = new SocketService();

export default socketService;
