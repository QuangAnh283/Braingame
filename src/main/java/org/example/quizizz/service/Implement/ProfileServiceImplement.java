package org.example.quizizz.service.Implement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quizizz.mapper.ProfileMapper;
import org.example.quizizz.model.dto.game.GameHistoryResponse;
import org.example.quizizz.model.dto.profile.*;
import org.example.quizizz.model.entity.*;
import org.example.quizizz.repository.*;
import org.example.quizizz.service.Interface.IFileStorageService;
import org.example.quizizz.service.Interface.IProfileService;
import org.example.quizizz.service.helper.AchievementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service quản lý thông tin cá nhân người dùng (profile).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImplement implements IProfileService {

    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final IFileStorageService fileStorageService;
    private final GameHistoryRepository gameHistoryRepository;
    private final RankRepository rankRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final ExamRepository examRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RoomRepository roomRepository;
    private final AchievementService achievementService;

    /**
     * Lấy thông tin profile của người dùng.
     * @param userId Id người dùng
     * @return Thông tin profile
     */
    @Override
    public UpdateProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        
        UpdateProfileResponse response = new UpdateProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setDob(user.getDob());
        response.setCreatedAt(user.getCreatedAt());
        response.setAvatarURL(user.getAvatarURL()); // avatarURL đã là presignedUrl
        
        return response;
    }

    /**
     * Cập nhật thông tin profile của người dùng.
     * @param userId Id người dùng
     * @param request Thông tin cập nhật
     * @return Thông tin profile sau cập nhật
     */
    @Override
    @Transactional
    public UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        profileMapper.updateUserFromDto(request, user);
        userRepository.save(user);
        
        return getProfile(userId); // Sử dụng lại method getProfile để có presigned URL
    }

    /**
     * Cập nhật avatar cho người dùng.
     * @param userId Id người dùng
     * @param file File avatar
     * @return Thông tin avatar mới
     * @throws Exception Nếu upload lỗi
     */
    @Override
    @Transactional
    public UpdateAvatarResponse updateAvatar(Long userId, MultipartFile file) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        
        // Upload file và lưu presignedUrl vào DB
        String presignedUrl = fileStorageService.uploadAvatar(file, userId);
        user.setAvatarURL(presignedUrl);
        userRepository.save(user);
        
        UpdateAvatarResponse response = new UpdateAvatarResponse();
        response.setAvatarURL(presignedUrl);
        return response;
    }

    /**
     * Lấy URL avatar của người dùng.
     * @param userId Id người dùng
     * @return Avatar URL
     * @throws Exception Nếu lỗi
     */
    @Override
    public String getAvatarUrl(Long userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        
        return user.getAvatarURL(); // avatarURL đã là presignedUrl
    }
    
    @Override
    public List<UserSearchResponse> searchUsers(String keyword) {
        List<User> users = userRepository.searchUsers(keyword);
        return users.stream()
            .limit(10) // Giới hạn 10 kết quả
            .map(user -> {
                UserSearchResponse response = new UserSearchResponse();
                response.setId(user.getId());
                response.setUsername(user.getUsername());
                response.setFullName(user.getFullName());
                response.setAvatarURL(user.getAvatarURL()); // avatarURL đã là presignedUrl
                return response;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public PublicProfileResponse getPublicProfile(String username) throws Exception {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        
        PublicProfileResponse response = new PublicProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setAvatarURL(user.getAvatarURL()); // avatarURL đã là presignedUrl
        
        return response;
    }

    /**
     * Lấy thống kê profile của người dùng để hiển thị trên Dashboard và ProfileStats.
     * @param userId Id người dùng
     * @return Thống kê profile
     */
    @Override
    public ProfileStatsResponse getProfileStats(Long userId) {
        log.info("Getting profile stats for user {}", userId);

        ProfileStatsResponse.ProfileStatsResponseBuilder statsBuilder = ProfileStatsResponse.builder();

        // Lấy thông tin rank
        Optional<Rank> rankOpt = rankRepository.findByUserId(userId);
        if (rankOpt.isPresent()) {
            Rank rank = rankOpt.get();
            statsBuilder.gamesPlayed(rank.getGamePlayed());
            statsBuilder.totalScore(rank.getTotalScore());
            statsBuilder.currentRank(calculateCurrentRank(userId, rank.getTotalScore()));

            // Tính thời gian nhanh nhất (totalTime là milliseconds)
            if (rank.getTotalTime() != null && rank.getTotalTime() > 0 && rank.getGamePlayed() > 0) {
                long avgTimeSeconds = (rank.getTotalTime() / 1000) / rank.getGamePlayed();
                statsBuilder.fastestTime(String.valueOf(avgTimeSeconds));
            } else {
                statsBuilder.fastestTime("0");
            }
        } else {
            statsBuilder.gamesPlayed(0);
            statsBuilder.totalScore(0);
            statsBuilder.currentRank(0);
            statsBuilder.fastestTime("N/A");
        }

        // Lấy thông tin game histories
        List<GameHistory> gameHistories = gameHistoryRepository.findByUserId(userId);

        // Tính highest score
        Integer highestScore = gameHistories.stream()
            .map(GameHistory::getScore)
            .max(Integer::compareTo)
            .orElse(0);
        statsBuilder.highestScore(highestScore);

        // Tính average score
        Double averageScore = gameHistories.isEmpty() ? 0.0 :
            gameHistories.stream()
                .mapToInt(GameHistory::getScore)
                .average()
                .orElse(0.0);
        statsBuilder.averageScore(averageScore);

        // Tính highest rank và medals từ game histories
        int highestRank = calculateHighestRank(userId, gameHistories);
        int medals = calculateMedals(userId, gameHistories);
        
        statsBuilder.highestRank(highestRank);
        statsBuilder.medals(medals);

        // Tìm best topic (chủ đề có điểm cao nhất)
        String bestTopic = findBestTopic(userId);
        statsBuilder.bestTopic(bestTopic);



        log.info("Profile stats retrieved successfully for user {}", userId);
        return statsBuilder.build();
    }

    @Override
    public List<GameHistoryResponse> getRecentGameHistory(Long userId) {
        return List.of();
    }

    @Override
    public PlayerStatsResponse getPlayerStats(Long userId) {
        return null;
    }

    /**
     * Tính rank hiện tại dựa trên total score
     */
    private Integer calculateCurrentRank(Long userId, Integer totalScore) {
        List<Rank> allRanks = rankRepository.findAll();
        allRanks.sort((r1, r2) -> r2.getTotalScore().compareTo(r1.getTotalScore()));

        for (int i = 0; i < allRanks.size(); i++) {
            if (allRanks.get(i).getUserId().equals(userId)) {
                return i + 1;
            }
        }
        return allRanks.size() + 1;
    }

    /**
     * Format time từ milliseconds sang string dễ đọc
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
    }

    /**
     * Tìm chủ đề tốt nhất của user (chủ đề có tỷ lệ đúng cao nhất)
     */
    private String findBestTopic(Long userId) {
        try {
            List<UserAnswer> userAnswers = userAnswerRepository.findByUserId(userId);

            if (userAnswers.isEmpty()) {
                return "N/A";
            }

            // Nhóm theo topic và tính tỷ lệ đúng
            Map<Long, List<UserAnswer>> answersByTopic = new HashMap<>();

            for (UserAnswer answer : userAnswers) {
                Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
                if (question != null && question.getExamId() != null) {
                    Exam exam = examRepository.findById(question.getExamId()).orElse(null);
                    if (exam != null && exam.getTopicId() != null) {
                        answersByTopic.computeIfAbsent(exam.getTopicId(), k -> new ArrayList<>()).add(answer);
                    }
                }
            }

            // Tìm topic có tỷ lệ đúng cao nhất
            Long bestTopicId = null;
            double bestAccuracy = 0.0;

            for (Map.Entry<Long, List<UserAnswer>> entry : answersByTopic.entrySet()) {
                List<UserAnswer> answers = entry.getValue();
                long correctCount = answers.stream().filter(UserAnswer::getIsCorrect).count();
                double accuracy = (double) correctCount / answers.size();

                if (accuracy > bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestTopicId = entry.getKey();
                }
            }

            if (bestTopicId != null) {
                Optional<Topic> topicOpt = topicRepository.findById(bestTopicId);
                if (topicOpt.isPresent()) {
                    return topicOpt.get().getName();
                }
            }

            return "N/A";
        } catch (Exception e) {
            log.error("Error finding best topic: {}", e.getMessage());
            return "N/A";
        }
    }

    /**
     * Tính highest rank (rank tốt nhất) của user từ game histories.
     * Rank càng thấp càng tốt (1 là tốt nhất).
     */
    private int calculateHighestRank(Long userId, List<GameHistory> gameHistories) {
        if (gameHistories.isEmpty()) {
            return 0;
        }

        int bestRank = Integer.MAX_VALUE;

        // Nhóm game histories theo game session
        Map<Long, List<GameHistory>> historiesBySession = gameHistories.stream()
            .filter(gh -> gh.getGameSessionId() != null)
            .collect(Collectors.groupingBy(GameHistory::getGameSessionId));

        // Với mỗi game session, tính rank của user
        for (Map.Entry<Long, List<GameHistory>> entry : historiesBySession.entrySet()) {
            List<GameHistory> sessionHistories = entry.getValue();
            
            // Sắp xếp theo score giảm dần
            sessionHistories.sort((h1, h2) -> Integer.compare(h2.getScore(), h1.getScore()));
            
            // Tìm rank của user trong session này
            for (int i = 0; i < sessionHistories.size(); i++) {
                if (sessionHistories.get(i).getUserId().equals(userId)) {
                    int rank = i + 1;
                    if (rank < bestRank) {
                        bestRank = rank;
                    }
                    break;
                }
            }
        }

        return bestRank == Integer.MAX_VALUE ? 0 : bestRank;
    }

    /**
     * Đếm số medals (số lần đạt top 3) của user.
     */
    private int calculateMedals(Long userId, List<GameHistory> gameHistories) {
        if (gameHistories.isEmpty()) {
            return 0;
        }

        int medalCount = 0;

        // Nhóm game histories theo game session
        Map<Long, List<GameHistory>> historiesBySession = gameHistories.stream()
            .filter(gh -> gh.getGameSessionId() != null)
            .collect(Collectors.groupingBy(GameHistory::getGameSessionId));

        // Với mỗi game session, kiểm tra xem user có đạt top 3 không
        for (Map.Entry<Long, List<GameHistory>> entry : historiesBySession.entrySet()) {
            List<GameHistory> sessionHistories = entry.getValue();
            
            // Sắp xếp theo score giảm dần
            sessionHistories.sort((h1, h2) -> Integer.compare(h2.getScore(), h1.getScore()));
            
            // Kiểm tra xem user có trong top 3 không
            for (int i = 0; i < Math.min(3, sessionHistories.size()); i++) {
                if (sessionHistories.get(i).getUserId().equals(userId)) {
                    medalCount++;
                    break;
                }
            }
        }

        return medalCount;
    }
}
