import { create } from 'zustand';
import socketService from '../services/socket-service';

type GameAnswer = {
    content: string;
};

type GameQuestion = {
    questionId: number | string;
    sequence: number;
    totalQuestions: number;
    content: string;
    imageUrl?: string;
    timeLimit?: number;
    answers: GameAnswer[];
};

type GamePlayer = {
    userId: number | string;
    username: string;
    score?: number;
    totalScore?: number;
    [key: string]: unknown;
};

type AnswerResult = {
    isCorrect: boolean;
    score?: number;
    totalScore?: number;
    userId: number | string;
    [key: string]: unknown;
};

type GameStoreState = {
    isGameActive: boolean;
    isGameStarting: boolean;
    isGameFinished: boolean;
    currentQuestion: GameQuestion | null;
    currentQuestionIndex: number;
    totalQuestions: number;
    timeRemaining: number;
    timerInterval: ReturnType<typeof setInterval> | null;
    questionStartTime: number | null;
    selectedAnswer: number | null;
    hasAnswered: boolean;
    answerResult: AnswerResult | null;
    players: GamePlayer[];
    leaderboard: GamePlayer[];
    roomId: string | number | null;
    isHost: boolean;
    isLoading: boolean;
    error: string | null;
    initGame: (roomId: string | number, isHost?: boolean) => void;
    startGame: () => void;
    onGameStarted: (data: unknown) => void;
    loadNextQuestion: (questionData: { question?: GameQuestion; totalQuestions?: number } | GameQuestion) => void;
    submitAnswer: (answerId: number) => void;
    onAnswerResult: (result: AnswerResult) => void;
    nextQuestion: () => void;
    onGameFinished: (data: unknown) => void;
    endGame: () => void;
    startTimer: (duration: number) => void;
    stopTimer: () => void;
    onTimerExpired: () => void;
    updatePlayers: (players: GamePlayer[]) => void;
    addPlayer: (player: GamePlayer) => void;
    removePlayer: (userId: string | number) => void;
    updateLeaderboard: () => void;
    setupSocketListeners: () => void;
    cleanupSocketListeners: () => void;
    reset: () => void;
    clearError: () => void;
};

/**
 * Game Store - Real-time game state management
 */
const useGameStore = create<GameStoreState>((set, get) => ({
    // Game state
    isGameActive: false,
    isGameStarting: false,
    isGameFinished: false,

    // Current question state
    currentQuestion: null,
    currentQuestionIndex: 0,
    totalQuestions: 0,

    // Timer state
    timeRemaining: 0,
    timerInterval: null,
    questionStartTime: null,

    // Player answers
    selectedAnswer: null,
    hasAnswered: false,
    answerResult: null,

    // Leaderboard
    players: [],
    leaderboard: [],

    // Room info
    roomId: null,
    isHost: false,

    // Loading & errors
    isLoading: false,
    error: null,

    // ==================== GAME ACTIONS ====================

    /**
     * Initialize game state for a room
     */
    initGame: (roomId, isHost = false) => {
        set({
            roomId,
            isHost,
            isGameActive: false,
            isGameStarting: false,
            isGameFinished: false,
            currentQuestion: null,
            currentQuestionIndex: 0,
            selectedAnswer: null,
            hasAnswered: false,
            answerResult: null,
            error: null
        });
    },

    /**
     * Start the game (host only)
     */
    startGame: () => {
        const { roomId, isHost } = get();

        if (!isHost) {
            set({ error: 'Only host can start the game' });
            return;
        }
        set({ isGameStarting: true, error: null });

        socketService.startGame(roomId);
    },

    /**
     * Handle game started event
     */
    onGameStarted: (_data) => {
        set({
            isGameActive: true,
            isGameStarting: false,
            isGameFinished: false,
            currentQuestionIndex: 0
        });
    },

    /**
     * Load next question
     */
    loadNextQuestion: (questionData) => {
        const { currentQuestionIndex } = get();
        // Stop previous timer
        get().stopTimer();

        const nextQuestion = (questionData as { question?: GameQuestion }).question || (questionData as GameQuestion);
        const totalQuestions = (questionData as { totalQuestions?: number }).totalQuestions || get().totalQuestions;

        set({
            currentQuestion: nextQuestion,
            currentQuestionIndex: currentQuestionIndex + 1,
            totalQuestions,
            selectedAnswer: null,
            hasAnswered: false,
            answerResult: null,
            questionStartTime: Date.now()
        });

        // Start timer for this question
        if (nextQuestion?.timeLimit) {
            get().startTimer(nextQuestion.timeLimit);
        }
    },

    /**
     * Submit answer for current question
     */
    submitAnswer: (answerId) => {
        const {
            roomId,
            currentQuestion,
            hasAnswered
        } = get();

        if (hasAnswered) {
            return;
        }

        if (!currentQuestion) {
            return;
        }

        set({
            selectedAnswer: answerId,
            hasAnswered: true
        });

        socketService.submitAnswer(
            roomId,
            currentQuestion.questionId,
            answerId
        );

        // Stop timer after answer
        get().stopTimer();
    },

    /**
     * Handle answer result from server
     */
    onAnswerResult: (result) => {
        set({
            answerResult: result
        });

        // Update player score in leaderboard
        const { players } = get();
        const updatedPlayers = players.map(player => {
            if (player.userId === result.userId) {
                return {
                    ...player,
                    score: result.score,
                    totalScore: result.totalScore
                };
            }
            return player;
        });

        set({ players: updatedPlayers });
        get().updateLeaderboard();
    },

    /**
     * Request next question (host only)
     */
    nextQuestion: () => {
        const { roomId, isHost } = get();

        if (!isHost) {
            return;
        }
        socketService.nextQuestion(roomId);

        set({
            selectedAnswer: null,
            hasAnswered: false,
            answerResult: null
        });
    },

    /**
     * Handle game finished
     */
    onGameFinished: (_data) => {
        get().stopTimer();

        set({
            isGameActive: false,
            isGameFinished: true,
            currentQuestion: null
        });

        get().updateLeaderboard();
    },

    /**
     * End/leave game
     */
    endGame: () => {
        get().stopTimer();

        set({
            isGameActive: false,
            isGameFinished: false,
            currentQuestion: null,
            currentQuestionIndex: 0,
            selectedAnswer: null,
            hasAnswered: false,
            answerResult: null,
            players: [],
            leaderboard: []
        });
    },

    // ==================== TIMER ACTIONS ====================

    /**
     * Start countdown timer for question
     */
    startTimer: (duration) => {
        // Clear existing timer
        get().stopTimer();

        set({ timeRemaining: duration });

        const interval = setInterval(() => {
            const { timeRemaining } = get();

            if (timeRemaining <= 0) {
                get().stopTimer();
                get().onTimerExpired();
            } else {
                set({ timeRemaining: timeRemaining - 1 });
            }
        }, 1000);

        set({ timerInterval: interval });
    },

    /**
     * Stop timer
     */
    stopTimer: () => {
        const { timerInterval } = get();

        if (timerInterval) {
            clearInterval(timerInterval);
            set({ timerInterval: null });
        }
    },

    /**
     * Handle timer expiration
     */
    onTimerExpired: () => {
        const { hasAnswered, isHost } = get();
        if (!hasAnswered) {
            // Auto-submit null answer if player didn't answer
            set({ hasAnswered: true });
        }

        // Host auto-advances to next question
        if (isHost) {
            setTimeout(() => {
                get().nextQuestion();
            }, 3000); // 3 second delay to show results
        }
    },

    // ==================== PLAYER ACTIONS ====================

    /**
     * Update players list
     */
    updatePlayers: (players) => {
        set({ players });
        get().updateLeaderboard();
    },

    /**
     * Add player to game
     */
    addPlayer: (player) => {
        const { players } = get();
        const exists = players.find(p => p.userId === player.userId);

        if (!exists) {
            set({ players: [...players, { ...player, score: 0, totalScore: 0 }] });
            get().updateLeaderboard();
        }
    },

    /**
     * Remove player from game
     */
    removePlayer: (userId) => {
        const { players } = get();
        set({
            players: players.filter(p => p.userId !== userId)
        });
        get().updateLeaderboard();
    },

    /**
     * Update leaderboard based on scores
     */
    updateLeaderboard: () => {
        const { players } = get();

        const sorted = [...players].sort((a, b) => {
            return (b.totalScore || 0) - (a.totalScore || 0);
        });

        set({ leaderboard: sorted });
    },

    // ==================== SOCKET EVENT SETUP ====================

    /**
     * Setup socket event listeners for game
     */
    setupSocketListeners: () => {
        // Game events
        socketService.onGameStarted((data) => {
            get().onGameStarted(data);
        });

        socketService.onNextQuestion((data) => {
            get().loadNextQuestion(data);
        });

        socketService.onAnswerSubmitted((result) => {
            get().onAnswerResult(result);
        });

        socketService.onGameFinished((data) => {
            get().onGameFinished(data);
        });

        // Player events
        socketService.onPlayerJoined((data) => {
            get().addPlayer(data);
        });

        socketService.onPlayerLeft((data) => {
            get().removePlayer(data.userId);
        });

        socketService.onRoomPlayers((data) => {
            get().updatePlayers(data.players);
        });

        // Error handling
        socketService.onError((error) => {
            set({ error: error.message || 'An error occurred' });
        });
    },

    /**
     * Cleanup socket listeners
     */
    cleanupSocketListeners: () => {
        // Remove all game-related listeners
        socketService.off('game-started');
        socketService.off('next-question');
        socketService.off('answer-submitted');
        socketService.off('game-finished');
        socketService.off('player-joined');
        socketService.off('player-left');
        socketService.off('room-players');
    },

    // ==================== UTILITY ====================

    /**
     * Reset game store to initial state
     */
    reset: () => {
        get().stopTimer();
        get().cleanupSocketListeners();

        set({
            isGameActive: false,
            isGameStarting: false,
            isGameFinished: false,
            currentQuestion: null,
            currentQuestionIndex: 0,
            totalQuestions: 0,
            timeRemaining: 0,
            timerInterval: null,
            questionStartTime: null,
            selectedAnswer: null,
            hasAnswered: false,
            answerResult: null,
            players: [],
            leaderboard: [],
            roomId: null,
            isHost: false,
            isLoading: false,
            error: null
        });
    },

    /**
     * Clear error
     */
    clearError: () => set({ error: null })
}));

export default useGameStore;
