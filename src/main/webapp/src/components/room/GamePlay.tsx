import { useEffect, useState } from 'react';
import useGameStore from '../../stores/use-game-store';
import '../../styles/components/room/game-play.css';
import { CircleCheck, CircleX, Clock3, Trophy } from 'lucide-react';

/**
 * GamePlay Component - Real-time quiz gameplay
 * Similar to Kahoot/Quizizz game interface
 */
const GamePlay = ({ isHost, onLeave }) => {
    const {
        currentQuestion,
        currentQuestionIndex,
        totalQuestions,
        timeRemaining,
        selectedAnswer,
        hasAnswered,
        answerResult,
        players,
        submitAnswer,
        nextQuestion
    } = useGameStore() as any;

    const [showResults, setShowResults] = useState(false);

    useEffect(() => {
        // Show results for 3 seconds after answering
        if (hasAnswered && answerResult) {
            setShowResults(true);
            const timer = setTimeout(() => {
                setShowResults(false);
            }, 3000);
            return () => clearTimeout(timer);
        }
    }, [hasAnswered, answerResult]);

    /**
     * Handle answer selection
     */
    const handleAnswerClick = (answerId) => {
        if (hasAnswered) return;
        submitAnswer(answerId);
    };

    /**
     * Handle next question (host only)
     */
    const handleNextQuestion = () => {
        if (isHost) {
            nextQuestion();
        }
    };

    if (!currentQuestion) {
        return (
            <div className="game-play-container">
                <div className="loading-question">
                    <div className="spinner"></div>
                    <p>Đang tải câu hỏi...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="game-play-container">
            {/* Header */}
            <div className="game-header">
                <div className="question-progress">
                    <span>Câu {currentQuestionIndex}/{totalQuestions}</span>
                </div>
                <div className="timer-display">
                    <span className={timeRemaining <= 5 ? 'timer-critical' : ''}>
                        <Clock3 size={16} /> {timeRemaining}s
                    </span>
                </div>
                <button className="btn-leave" onClick={onLeave}>
                    Rời phòng
                </button>
            </div>

            {/* Question Display */}
            <div className="question-section">
                <h2 className="question-text">{currentQuestion.questionText}</h2>

                {currentQuestion.imageUrl && (
                    <img
                        src={currentQuestion.imageUrl}
                        alt="Question"
                        className="question-image"
                    />
                )}
            </div>

            {/* Answer Options */}
            {!showResults ? (
                <div className="answers-grid">
                    {currentQuestion.answers?.map((answer, index) => (
                        <button
                            key={answer.id}
                            className={`answer-option ${selectedAnswer === answer.id ? 'selected' : ''
                            } ${hasAnswered ? 'disabled' : ''}`}
                            onClick={() => handleAnswerClick(answer.id)}
                            disabled={hasAnswered}
                            style={{
                                animationDelay: `${index * 0.1}s`,
                                backgroundColor: getAnswerColor(index)
                            }}
                        >
                            <span className="answer-icon">{getAnswerIcon(index)}</span>
                            <span className="answer-text">{answer.answerText}</span>
                        </button>
                    ))}
                </div>
            ) : (
                <div className="answer-result-section">
                    <div className={`result-card ${answerResult?.isCorrect ? 'correct' : 'incorrect'}`}>
                        <div className="result-icon">
                            {answerResult?.isCorrect ? <CircleCheck size={28} /> : <CircleX size={28} />}
                        </div>
                        <h3>
                            {answerResult?.isCorrect ? 'Chính xác!' : 'Sai rồi!'}
                        </h3>
                        <p className="points-earned">
                            +{answerResult?.score || 0} điểm
                        </p>

                    </div>

                    {/* Players who answered */}
                    <div className="players-answered">
                        <h4>Đã trả lời: {players.filter(p => p.hasAnswered).length}/{players.length}</h4>
                    </div>

                    {/* Next button for host */}
                    {isHost && (
                        <button
                            className="btn-next-question"
                            onClick={handleNextQuestion}
                        >
                            Câu tiếp theo →
                        </button>
                    )}
                </div>
            )}

            {/* Mini Leaderboard */}
            <div className="mini-leaderboard">
                <h4><Trophy size={16} /> Top 3</h4>
                <div className="top-players">
                    {players.slice(0, 3).map((player, index) => (
                        <div key={player.userId} className="top-player">
                            <span className="rank">#{index + 1}</span>
                            <span className="name">{player.username}</span>
                            <span className="score">{player.totalScore || 0}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

/**
 * Get color for answer option
 */
const getAnswerColor = (index) => {
    const colors = ['#e74c3c', '#3498db', '#f39c12', '#2ecc71'];
    return colors[index % colors.length];
};

/**
 * Get icon for answer option
 */
const getAnswerIcon = (index) => {
    const icons = ['△', '◇', '○', '□'];
    return icons[index % icons.length];
};

export default GamePlay;
