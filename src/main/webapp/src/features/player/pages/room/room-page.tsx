import { useState, useEffect } from 'react';
import { IoAdd, IoClose, IoLogInOutline, IoRefresh, IoSearch } from 'react-icons/io5';
import { useNavigate } from 'react-router-dom';
import useRoomStore from '../../../../stores/use-room-store';
import useWebSocketCleanup from '../../../../hooks/use-web-socket-cleanup';
import CreateRoomModal from '../../../../components/room/CreateRoomModal';
import JoinByCodeModal from '../../../../components/room/JoinByCodeModal';
import RoomCard from '../../../../components/room/RoomCard';
import SimpleBackground from '../../../../shared/components/SimpleBackground';
import '../../../../styles/pages/room/room-page.css';

const RoomsPage = () => {
    // Navigation and user management
    const navigate = useNavigate();
    // WebSocket cleanup
    useWebSocketCleanup();

    // Room management state and actions
    const {
        rooms,
        loading,
        error,
        loadRooms,
        joinRoom,
        clearError,
        startAutoRefresh,
        stopAutoRefresh,
        subscribeToRoomList,
        unsubscribeFromRoomList
    } = useRoomStore();

    // Local component state
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [showJoinModal, setShowJoinModal] = useState(false);
    const [joinError, setJoinError] = useState('');
    const [success, setSuccess] = useState('');
    const [joinLoading, setJoinLoading] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 6;


    /**
     * Initialize room data and setup realtime updates
     * - Initial load via REST API
     * - Subscribe to Socket.IO for realtime broadcasts
     */
    useEffect(() => {
        // 1. Initial load
        loadRooms();

        // 2. Subscribe to realtime updates
        subscribeToRoomList();

        // 3. Auto-refresh fallback (15s) phòng trường hợp socket bị mất kết nối / miss event
        startAutoRefresh();

        // 4. Cleanup on unmount
        return () => {
            unsubscribeFromRoomList();
            stopAutoRefresh();
        };
    }, [loadRooms, startAutoRefresh, stopAutoRefresh, subscribeToRoomList, unsubscribeFromRoomList]);

    /**
     * Handle joining a public room
     * @param {string} roomCode - The room code to join
     */
    const handleJoinPublic = async (roomCode: string) => {

        // Validation
        if (!roomCode) {

            setJoinError('Mã phòng không hợp lệ');
            return;
        }

        // Clear previous errors and messages
        setJoinError('');
        setSuccess('');
        clearError();

        const result = await joinRoom(roomCode);

        if (result.success) {
            setSuccess('Đang chuyển hướng vào phòng chờ...');
            const targetRoom = result.data?.roomCode || result.data?.code || result.data?.Code || roomCode;
            navigate(`/waiting-room/${targetRoom}`);
        } else {
            setJoinError(result.error);
        }
    };

    /**
     * Handle joining a private room by code
     * @param {string} roomCode - The room code to join
     */
    const handleJoinPrivate = async (roomCode: string) => {
        setJoinLoading(true);
        setJoinError('');

        const result = await joinRoom(roomCode);

        if (result.success) {
            setSuccess('Đang chuyển hướng vào phòng chờ...');
            const targetRoom = result.data?.roomCode || result.data?.code || roomCode;
            setShowJoinModal(false);
            navigate(`/waiting-room/${targetRoom}`);
        } else {
            setJoinError(result.error);
        }

        setJoinLoading(false);
    };

    /**
     * Handle successful join from modal
     */
    const handleJoinSuccess = (result: any) => {
        if (result.success) {
            setSuccess('Đã tham gia phòng thành công!');
            const targetRoom = result.data?.roomCode || result.data?.code || result.data?.RoomCode;
            if (targetRoom) {
                navigate(`/waiting-room/${targetRoom}`);
            }
        }
    };

    /**
     * Filter rooms based on search query
     */
    const filteredRooms = rooms.filter(room => {
        if (!searchQuery) return true;
        const query = searchQuery.toLowerCase();
        const roomName = (room.roomName || room.RoomName || '').toLowerCase();
        const topicName = (room.topicName || room.TopicName || '').toLowerCase();
        const roomCode = (room.roomCode || room.RoomCode || '').toLowerCase();
        return (
            roomName.includes(query) ||
            topicName.includes(query) ||
            roomCode.includes(query)
        );
    });

    /**
     * Calculate pagination
     */
    const totalPages = Math.ceil(filteredRooms.length / itemsPerPage);
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const currentRooms = filteredRooms.slice(startIndex, endIndex);

    /**
     * Generate smart pagination numbers
     * Shows first page, last page, current page and surrounding pages
     */
    const getPaginationNumbers = () => {
        const pages = [];
        const maxVisiblePages = 5;

        if (totalPages <= maxVisiblePages) {
            // Show all pages if total is small
            for (let i = 1; i <= totalPages; i++) {
                pages.push(i);
            }
        } else {
            // Smart pagination logic
            const halfVisible = Math.floor(maxVisiblePages / 2);

            let startPage = Math.max(1, currentPage - halfVisible);
            let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

            // Adjust startPage if we're near the end
            if (endPage === totalPages) {
                startPage = Math.max(1, endPage - maxVisiblePages + 1);
            }

            // Add first page if not included
            if (startPage > 1) {
                pages.push(1);
                if (startPage > 2) {
                    pages.push('...');
                }
            }

            // Add visible pages
            for (let i = startPage; i <= endPage; i++) {
                pages.push(i);
            }

            // Add last page if not included
            if (endPage < totalPages) {
                if (endPage < totalPages - 1) {
                    pages.push('...');
                }
                pages.push(totalPages);
            }
        }

        return pages;
    };

    /**
     * Handle page change
     */
    const handlePageChange = (page: number) => {
        if (page >= 1 && page <= totalPages) {
            setCurrentPage(page);
            // Scroll to top of room grid
            const roomGrid = document.querySelector('.room-grid');
            if (roomGrid) {
                roomGrid.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }
    };

    /**
     * Handle search change and reset to first page
     */
    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchQuery(e.target.value);
        setCurrentPage(1); // Reset to first page when searching
    };

    return (
        <div className="room-page">
            <SimpleBackground />
            <main className="room-content">

                <div className="room-container">

                    <div className="room-wrapper">
                        <div className="room-header">
                            <div className="room-header-left">
                                <h1>Danh sách phòng</h1>
                            </div>

                            <div className="room-actions">
                                <button
                                    className="room-btn-action room-btn-create"
                                    onClick={() => setShowCreateModal(true)}
                                >
                                    <IoAdd className="room-btn-icon" />
                                    <span>Tạo phòng mới</span>
                                </button>
                                <button
                                    className="room-btn-action room-btn-join"
                                    onClick={() => setShowJoinModal(true)}
                                >
                                    <IoLogInOutline className="room-btn-icon" />
                                    <span>Tham gia bằng mã</span>
                                </button>
                            </div>
                        </div>

                        <div className="room-filter">
                            <div className="room-search-container">
                                <IoSearch className="room-search-icon" />
                                <input
                                    type="text"
                                    className="room-search-input"
                                    placeholder="Tìm kiếm phòng..."
                                    value={searchQuery}
                                    onChange={handleSearchChange}
                                />
                            </div>
                        </div>

                        {loading && rooms.length === 0 ? (
                            <div className="room-loading-container">
                                <div className="room-loading-spinner">
                                    <IoRefresh className="room-spinner-icon" />
                                </div>
                                <p>Đang tải danh sách phòng...</p>
                            </div>
                        ) : (
                            <div className="room-grid">
                                {currentRooms.length === 0 ? (
                                    <div className="room-empty">
                                        <div className="room-empty-icon">
                                            <IoSearch size={48} />
                                        </div>
                                        <h3>Không tìm thấy phòng nào</h3>
                                        <p>Hãy tạo phòng mới hoặc tham gia bằng mã phòng</p>
                                    </div>
                                ) : (
                                    currentRooms.map((room, index) => (
                                        <RoomCard
                                            key={room.RoomCode || `room-${index}`}
                                            room={room}
                                            onJoinPublic={handleJoinPublic}
                                        />
                                    ))
                                )}
                            </div>
                        )}

                        {/* Pagination */}
                        {totalPages > 1 && (
                            <div className="room-pagination">
                                <button
                                    className="pagination-button pagination-nav pagination-prev"
                                    onClick={() => {
                                        handlePageChange(currentPage - 1);
                                    }}
                                    disabled={currentPage === 1}
                                    aria-label="Trang trước"
                                >
                                    <span className="pagination-arrow">‹</span>
                                    <span className="pagination-text">Trước</span>
                                </button>

                                <div className="pagination-numbers">
                                    {getPaginationNumbers().map((page, index) => (
                                        page === '...' ? (
                                            <span key={`ellipsis-${index}`} className="pagination-ellipsis">
                                                ...
                                            </span>
                                        ) : (
                                            <button
                                                key={page}
                                                className={`pagination-button pagination-number ${page === currentPage ? 'active' : ''
                                                }`}
                                                onClick={() => {
                                                    handlePageChange(page);
                                                }}
                                                aria-label={`Trang ${page}`}
                                                aria-current={page === currentPage ? 'page' : undefined}
                                            >
                                                {page}
                                            </button>
                                        )
                                    ))}
                                </div>

                                <button
                                    className="pagination-button pagination-nav pagination-next"
                                    onClick={() => {
                                        handlePageChange(currentPage + 1);
                                    }}
                                    disabled={currentPage === totalPages}
                                    aria-label="Trang sau"
                                >
                                    <span className="pagination-text">Sau</span>
                                    <span className="pagination-arrow">›</span>
                                </button>
                            </div>
                        )}

                        {/* Results info */}
                        {filteredRooms.length > 0 && (
                            <div className="room-results-info">
                                Hiển thị {startIndex + 1}-{Math.min(endIndex, filteredRooms.length)} của {filteredRooms.length} phòng
                            </div>
                        )}
                    </div>
                </div>
            </main>

            {showCreateModal && (
                <CreateRoomModal
                    onClose={() => setShowCreateModal(false)}
                />
            )}

            <JoinByCodeModal
                isOpen={showJoinModal}
                onClose={() => {
                    setShowJoinModal(false);
                    setJoinError('');
                }}
                onJoin={handleJoinPrivate}
                onSuccess={handleJoinSuccess}
                loading={joinLoading}
                error={joinError}
            />

            {(error || joinError) && !showJoinModal && (
                <div className="room-notification room-error">
                    {error || joinError}
                    <button onClick={() => { clearError(); setJoinError(''); }}>
                        <IoClose />
                    </button>
                </div>
            )}

            {success && !showJoinModal && (

                <div className="room-notification room-success">
                    {success}
                </div>
            )}
        </div>
    );
};

export default RoomsPage;