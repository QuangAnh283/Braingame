import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FaCrown, FaMedal } from 'react-icons/fa';
import socketService from '../../services/socket-service';
import authStore from '../../stores/auth-store';
import useRoomStore from '../../stores/use-room-store-realtime';
import AnswerResultPopup from './AnswerResultPopup';
import CompletionPopup from './CompletionPopup';
import GameStartPopup from './GameStartPopup';
import NotificationPopup from './NotificationPopup';
import '../../styles/components/room/game-room.css';

const formatTime = (seconds) => {
    const safe = Math.max(0, Number(seconds) || 0);
    const mins = Math.floor(safe / 60);
    const secs = safe % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const GameRoom = () => {
    const { roomCode } = useParams();
    const navigate = useNavigate();
    const currentUser = authStore((state: any) => state.user);
    const { currentRoom } = useRoomStore() as any;

    const [gameState, setGameState] = useState(null);
    const [currentQuestion, setCurrentQuestion] = useState(null);
    const [selectedAnswer, setSelectedAnswer] = useState(null);
    const [timeRemaining, setTimeRemaining] = useState(0);
    const [hasAnswered, setHasAnswered] = useState(false);
    const [isConnected, setIsConnected] = useState(true); 
    const [gameResults, setGameResults] = useState(null);
    const [answerResult, setAnswerResult] = useState(null);

    // ✅ NEW: States cho các popup thay thế toast
    const [notification, setNotification] = useState(null);
    const [showGameStart, setShowGameStart] = useState(false);
    const [showCompletion, setShowCompletion] = useState(false);

    const hasAnsweredRef = useRef(false);
    const questionStartTimeRef = useRef(null);
    const handleGameMessageRef = useRef(null);
    const handlePersonalMessageRef = useRef(null);

    useEffect(() => {
        hasAnsweredRef.current = hasAnswered;
    }, [hasAnswered]);

    // Khởi tạo với dữ liệu sự kiện game-started từ WaitingRoom
    useEffect(() => {
        const setupGameSubscriptions = () => {
            // Sửa: Xóa các listener tồn tại trước khi thêm mới để tránh trùng lặp
            socketService.off('game-started');
            socketService.off('next-question');
            socketService.off('answer-submitted');
            socketService.off('player-answered');
            socketService.off('game-ended');
            socketService.off('game-finished');
            socketService.off('countdown-tick');
            socketService.off('time-up');

            // Lắng nghe sự kiện game-started (chứa câu hỏi đầu tiên)
            socketService.on('game-started', (data) => {
                handleGameMessageRef.current?.({ type: 'GAME_STARTED', data });
            });

            // Lắng nghe sự kiện next-question
            socketService.on('next-question', (data) => {
                handleGameMessageRef.current?.({ type: 'NEXT_QUESTION', data });
            });

            // Lắng nghe sự kiện answer-submitted (kết quả cá nhân) - MỘT LẦN mỗi câu trả lời
            const answerSubmittedHandler = (data) => {
                handlePersonalMessageRef.current?.({ type: 'ANSWER_RESULT', data });
            };
            socketService.on('answer-submitted', answerSubmittedHandler);

            // Lắng nghe sự kiện player-answered (người chơi khác)
            socketService.on('player-answered', (data) => {
                // Chỉ cập nhật trạng thái của người chơi khác, không cập nhật điểm
                setGameState(prev => {
                    if (!prev || !prev.players) return prev;
                    return {
                        ...prev,
                        players: prev.players.map(p =>
                            p.userId === data.userId
                                ? { ...p, hasAnswered: true }
                                : p
                        )
                    };
                });
            });

            // Lắng nghe sự kiện game-ended/game-finished
            socketService.on('game-ended', (data) => {
                handleGameMessageRef.current?.({ type: 'GAME_ENDED', data });
            });

            socketService.on('game-finished', (data) => {
                handleGameMessageRef.current?.({ type: 'GAME_ENDED', data });
            });

            // Per-player flow: timeRemaining giờ do local countdown quản lý (xem useEffect bên dưới),
            // không dùng countdown-tick/time-up từ shared timer của server vì mỗi player đang ở
            // câu hỏi khác nhau với mốc thời gian riêng. Bỏ qua 2 sự kiện này.
        };

        const initializeWebSocket = async () => {
            try {
                await socketService.connect();
                setIsConnected(true);
                setupGameSubscriptions();
            } catch {
                setNotification({ type: 'error', message: 'Không thể kết nối đến game. Vui lòng thử lại!' });
                // Không chuyển hướng ngay lập tức, cho người dùng có cơ hội thử lại
            }
        };

        if (!currentUser || !roomCode) {
            navigate('/');
            return;
        }

        // Socket nên đã kết nối từ WaitingRoom
        if (socketService.isConnected()) {
            setIsConnected(true);
            setupGameSubscriptions();

            // Sửa: Yêu cầu trạng thái game hiện tại để lấy câu hỏi đang diễn ra nếu game đã bắt đầu
            if (currentRoom?.id) {
                socketService.emit('get-game-state', { roomId: currentRoom.id }, (response) => {
                    if (response && response.currentQuestion) {
                        setCurrentQuestion(response.currentQuestion);
                        setSelectedAnswer(null);
                        hasAnsweredRef.current = false;
                        setHasAnswered(false);
                        setTimeRemaining(response.remainingTime ?? response.currentQuestion.timeLimit ?? 30);
                        questionStartTimeRef.current = Date.now();
                    }
                    if (response && response.players) {
                        setGameState(response);
                    }
                });
            }
        } else {
            initializeWebSocket();
        }

        return () => {
            // KHÔNG ngắt kết nối khỏi phòng - để người dùng ở lại phòng
        };
    }, [currentRoom?.id, currentUser, navigate, roomCode]);

    // Per-player local countdown: mỗi câu hỏi của player tự đếm từ timeLimit về 0.
    // Khi 0 mà chưa trả lời thì auto-submit null (server tính sai, score 0, gửi câu tiếp theo).
    useEffect(() => {
        if (!currentQuestion) return;

        const limit = Number(currentQuestion.timeLimit) || 30;
        setTimeRemaining(limit);
        // Đánh dấu mốc bắt đầu để tính timeTaken khi player submit.
        questionStartTimeRef.current = Date.now();

        const interval = setInterval(() => {
            setTimeRemaining(prev => {
                if (prev <= 1) {
                    clearInterval(interval);
                    if (!hasAnsweredRef.current) {
                        hasAnsweredRef.current = true;
                        setHasAnswered(true);
                        socketService.emit('submit-answer', {
                            roomId: currentRoom?.id,
                            questionId: currentQuestion.questionId || currentQuestion.id,
                            answerId: null,
                            selectedAnswer: null,
                            selectedOptionIndex: null,
                            answerText: null,
                            // Hết giờ → timeTaken = full timeLimit (ms) để dashboard hiển thị đúng.
                            timeTaken: limit * 1000
                        });
                    }
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(interval);
    }, [currentQuestion, currentRoom?.id]);

    const handleGameMessage = (message) => {

        switch (message.type) {
            case 'GAME_STARTED':
                // Per-player flow: game-started giờ là tín hiệu khởi động, KHÔNG kèm câu hỏi.
                // Câu hỏi đầu tiên được gửi riêng cho từng player qua sự kiện next-question
                // ngay sau game-started, với thứ tự shuffle theo userId.
                if (currentRoom?.players) {
                    setGameState({
                        players: currentRoom.players.map(p => ({
                            ...p,
                            score: 0,
                            hasAnswered: false
                        }))
                    });
                }
                setShowGameStart(true);

                // Backwards-compat: nếu vẫn có question (server cũ chưa update) thì set luôn.
                if (message.data.question) {
                    setCurrentQuestion(message.data.question);
                    setSelectedAnswer(null);
                    hasAnsweredRef.current = false;
                    setHasAnswered(false);
                    setTimeRemaining(message.data.question.timeLimit || 30);
                    questionStartTimeRef.current = Date.now();
                }
                break;

            case 'NEXT_QUESTION':
            {
                // Cần lấy từ message.data.question, KHÔNG phải message.data trực tiếp
                const nextQuestionData = message.data.question || message.data;

                setCurrentQuestion(nextQuestionData);
                setSelectedAnswer(null);
                hasAnsweredRef.current = false;
                setHasAnswered(false);
                setTimeRemaining(nextQuestionData.timeLimit || 30);
                questionStartTimeRef.current = Date.now();
                setNotification({ type: 'info', message: `Câu hỏi ${nextQuestionData.questionNumber}/${nextQuestionData.totalQuestions}` });
                break;
            }

            case 'GAME_ENDED':
                setGameResults(message.data);
                setCurrentQuestion(null);
                setShowCompletion(true); // Hiển thị popup hoàn thành
                break;

            default:
                break;
        }
    };

    const handlePersonalMessage = (message) => {
        if (message.type === 'ANSWER_RESULT') {
            const result = message.data.result || message.data;

            // Cập nhật điểm cục bộ ngay lập tức
            const earnedScore = result.score || result.pointsEarned || 0;

            // Cập nhật gameState với điểm mới
            setGameState(prev => {
                if (!prev || !prev.players) return prev;
                return {
                    ...prev,
                    players: prev.players.map(p =>
                        p.userId === currentUser.id
                            ? { ...p, score: (p.score || 0) + earnedScore, hasAnswered: true }
                            : p
                    )
                };
            });

            setAnswerResult({
                isCorrect: result.isCorrect,
                score: earnedScore,
                streak: result.streak || 0,
                streakMultiplier: result.streakMultiplier || 1.0
            });

            if (message.data.completed) {
                // Player này đã hoàn thành tất cả câu hỏi
                setTimeout(() => {
                    setAnswerResult(null);
                    setCurrentQuestion(null);
                    hasAnsweredRef.current = false;
                    setHasAnswered(false);

                    setShowCompletion(true); // Hiển thị popup hoàn thành
                }, 2500);
            }
        }
    };

    handleGameMessageRef.current = handleGameMessage;
    handlePersonalMessageRef.current = handlePersonalMessage;

    const submitAnswer = () => {
        if (hasAnswered || !currentQuestion) return;

        const roomId = currentRoom?.id;
        const questionId = currentQuestion.questionId || currentQuestion.id;
        const questionOptions = currentQuestion.answers || currentQuestion.options || [];
        const selectedAnswerObj = questionOptions.find(opt =>
            (opt.text || opt.answerText || opt) === selectedAnswer
        );
        const selectedOptionIndex = questionOptions.findIndex(opt =>
            (opt.text || opt.answerText || opt) === selectedAnswer
        );
        const answerId = selectedAnswerObj?.id;

        // Đo thời gian player thực sự dùng để trả lời (ms) — backend dùng cho score + dashboard.
        const timeTaken = questionStartTimeRef.current
            ? Date.now() - questionStartTimeRef.current
            : null;

        socketService.emit('submit-answer', {
            roomId: roomId,
            questionId: questionId,
            answerId: answerId,
            selectedAnswer: selectedAnswer,
            selectedOptionIndex: selectedOptionIndex,
            answerText: selectedAnswer,
            timeTaken: timeTaken
        });

        hasAnsweredRef.current = true;
        setHasAnswered(true);
    };

    const startGame = () => {
        if (!currentRoom?.id) {
            setNotification({ type: 'error', message: 'Không tìm thấy phòng để bắt đầu game.' });
            return;
        }

        socketService.startGame(currentRoom.id, (response) => {
            if (response?.success === false) {
                setNotification({ type: 'error', message: response.message || 'Không thể bắt đầu game.' });
            }
        });
    };

    const getPlayerRankColor = (rank) => {
        switch (rank) {
            case 1: return '#FFD700'; // Gold
            case 2: return '#C0C0C0'; // Silver
            case 3: return '#CD7F32'; // Bronze
            default: return '#666';
        }
    };

    if (!isConnected) {
        return (
            <div className="game-room loading">
                <div className="loading-spinner">
                    <div className="spinner"></div>
                    <p>Đang kết nối đến game...</p>
                </div>
            </div>
        );
    }

    // Show results screen
    if (gameResults) {
        const rankings = gameResults.result?.ranking || gameResults.ranking || [];

        return (
            <div className="game-room results">
                <div className="results-container">
                    <h2 className="results-title">Kết Quả Game</h2>

                    <div className="final-leaderboard">
                        {rankings.length > 0 ? (
                            rankings.map((player, index) => (
                                <div key={player.userId} className={`result-item rank-${index + 1}`}>
                                    <div className="rank-badge" style={{ backgroundColor: getPlayerRankColor(index + 1) }}>
                                        <span className="rank-number">#{index + 1}</span>
                                        {index === 0 && <FaCrown className="rank-icon" />}
                                    </div>
                                    <div className="player-info">
                                        <div className="player-details">
                                            <h3 className="player-name">
                                                {player.userName || `User ${player.userId}`}
                                                {player.userId === currentUser?.id && <span className="you-badge"> (Bạn)</span>}
                                            </h3>
                                            <div className="player-stats">
                                                <span className="stat-item">
                                                    <strong>{player.totalScore || 0}</strong> điểm
                                                </span>
                                                <span className="stat-separator">•</span>
                                                <span className="stat-item">
                                                    <strong>{(player.totalTime / 1000).toFixed(1)}s</strong>
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                    {index < 3 && (
                                        <div className="medal-icon">
                                            {index === 0 && <FaMedal style={{ color: 'gold', fontSize: '24px' }} />}
                                            {index === 1 && <FaMedal style={{ color: 'silver', fontSize: '24px' }} />}
                                            {index === 2 && <FaMedal style={{ color: '#cd7f32', fontSize: '24px' }} />}
                                        </div>
                                    )}
                                </div>
                            ))
                        ) : (
                            <div className="no-results">
                                <p>Không có kết quả</p>
                            </div>
                        )}
                    </div>

                    <div className="results-actions">
                        <button onClick={() => navigate('/dashboard')} className="btn-primary">
                            Về Dashboard
                        </button>
                        <button onClick={() => navigate('/rooms')} className="btn-primary">
                            Tìm phòng khác
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // Show question screen
    if (currentQuestion) {
        // FIX: Backend returns 'answers' with field 'text', not 'answerText'
        const questionOptions = currentQuestion.answers || currentQuestion.options || [];

        return (
            <div className="game-room playing">
                <div className="game-header">
                    <div className="question-progress">
                        Câu {currentQuestion.questionNumber || 1}/{currentQuestion.totalQuestions || '?'}
                    </div>
                    <div className={`timer ${timeRemaining <= 10 ? 'urgent' : ''}`}>
                        ⏱️ {formatTime(timeRemaining)}
                    </div>
                    <div className="score">
                        {gameState?.players?.find(p => p.userId === currentUser.id)?.score || 0} điểm
                    </div>
                </div>

                <div className="question-container">
                    {currentQuestion.imageUrl && (
                        <div className="question-image">
                            <img src={currentQuestion.imageUrl} alt="Question" />
                        </div>
                    )}

                    <h2 className="question-text">{currentQuestion.questionText}</h2>

                    <div className="options-container">
                        {questionOptions.length > 0 ? (
                            questionOptions.map((option, index) => {
                                // FIX: Backend returns {id, text} not {id, answerText}
                                const optionText = option.text || option.answerText || option;
                                const optionId = option.id || index;

                                return (
                                    <button
                                        key={optionId}
                                        className={`option-btn ${selectedAnswer === optionText ? 'selected' : ''} ${hasAnswered ? 'disabled' : ''}`}
                                        onClick={() => !hasAnswered && setSelectedAnswer(optionText)}
                                        disabled={hasAnswered}
                                    >
                                        <span className="option-letter">{String.fromCharCode(65 + index)}</span>
                                        <span className="option-text">{optionText}</span>
                                    </button>
                                );
                            })
                        ) : (
                            <div className="no-options">
                                <p>⚠️ Không có đáp án nào được tải</p>
                                <p>Debug info: {JSON.stringify(currentQuestion, null, 2)}</p>
                            </div>
                        )}
                    </div>

                    <div className="question-actions">
                        <button
                            className={`submit-btn ${!selectedAnswer || hasAnswered ? 'disabled' : ''}`}
                            onClick={submitAnswer}
                            disabled={!selectedAnswer || hasAnswered}
                        >
                            {hasAnswered ? 'Đã trả lời' : 'Gửi đáp án'}
                        </button>
                    </div>
                </div>

                {/* Real-time player status */}
                {gameState?.players && (
                    <div className="players-status">
                        <h3>Trạng thái người chơi:</h3>
                        <div className="players-grid">
                            {gameState.players.map(player => (
                                <div key={player.userId} className={`player-status ${player.hasAnswered ? 'answered' : 'waiting'}`}>
                                    <img
                                        src={player.avatarUrl || '/default-avatar.png'}
                                        alt={player.displayName}
                                        className="mini-avatar"
                                    />
                                    <span className="player-name">{player.displayName}</span>
                                    <span className="status-indicator">
                                        {player.hasAnswered ? '✅' : '⏳'}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* Popup kết quả câu trả lời */}
                {answerResult && (
                    <AnswerResultPopup
                        result={answerResult}
                        onClose={() => setAnswerResult(null)}
                    />
                )}

                {/* Popup thông báo bắt đầu game */}
                {showGameStart && (
                    <GameStartPopup
                        onClose={() => setShowGameStart(false)}
                    />
                )}

                {/* Popup hoàn thành game */}
                {showCompletion && (
                    <CompletionPopup
                        onClose={() => setShowCompletion(false)}
                    />
                )}
            </div>
        );
    }

    // Show waiting/lobby screen
    return (
        <div className="game-room waiting">
            <div className="waiting-container">
                <h2>🎮 Phòng Game: {roomCode}</h2>

                {gameState && (
                    <div className="game-info">
                        <p>Trạng thái: <span className="status">{gameState.gameStatus}</span></p>
                        <p>Tổng câu hỏi: {gameState.totalQuestions}</p>

                        <div className="players-list">
                            <h3>Người chơi ({gameState.players?.length || 0}):</h3>
                            {gameState.players?.map(player => (
                                <div key={player.userId} className="player-item">
                                    <img
                                        src={player.avatarUrl || '/default-avatar.png'}
                                        alt={player.displayName}
                                        className="player-avatar"
                                    />
                                    <span className="player-name">
                                        {player.displayName}
                                        {player.userId === currentUser.id && ' (Bạn)'}
                                    </span>
                                    <span className={`player-status ${player.status.toLowerCase()}`}>
                                        {player.status}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                <div className="waiting-actions">
                    {gameState?.isHost && (
                        <button onClick={startGame} className="btn-primary start-btn">
                            Bắt đầu Game
                        </button>
                    )}

                    <button onClick={() => navigate('/rooms')} className="btn-secondary">
                        Rời phòng
                    </button>
                </div>

                {/* Popup thông báo chung */}
                {notification && (
                    <NotificationPopup
                        type={notification.type}
                        message={notification.message}
                        onClose={() => setNotification(null)}
                        autoClose={notification.autoClose !== false}
                    />
                )}
            </div>
        </div>
    );
};

export default GameRoom;

