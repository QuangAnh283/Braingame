package org.example.quizizz.service.Implement;

import org.example.quizizz.common.config.MinioConfig;
import org.example.quizizz.service.Interface.IFileStorageService;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service lưu trữ file sử dụng MinIO (avatar, hình ảnh quiz).
 * Đảm bảo bucket tồn tại, upload và xóa file, trả về URL file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageServiceImplement implements IFileStorageService {

    private final MinioClient internalMinioClient;
    private final MinioConfig minioConfig;

    @Autowired
    @Qualifier("publicMinioClient")
    private MinioClient publicMinioClient;

    /**
     * Upload avatar cho người dùng lên MinIO.
     * @param file File avatar
     * @param userId Id người dùng
     * @return Object key của avatar trong bucket
     * @throws Exception Nếu upload lỗi
     */
    @Override
    public String uploadAvatar(MultipartFile file, Long userId) throws Exception {
        log.info("Starting avatar upload for user: {}, file size: {} bytes", userId, file.getSize());

        try {
            testMinioConnection();
            ensureBucketExists(minioConfig.getAvatarBucket());
            String fileName = "avatars/" + userId + "/avatar_" + UUID.randomUUID().toString().substring(0, 8) + getFileExtension(file.getOriginalFilename());

            log.info("Uploading avatar file: {} to bucket: {}", fileName, minioConfig.getAvatarBucket());

            internalMinioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getAvatarBucket())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Successfully uploaded avatar: {} for user: {}", fileName, userId);
            return fileName;

        } catch (MinioException e) {
            log.error("MinIO error during avatar upload for user {}: {}", userId, e.getMessage(), e);
            throw new Exception("MinIO upload failed: " + e.getMessage(), e);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Signature/Credential error during avatar upload for user {}: {}", userId, e.getMessage(), e);
            throw new Exception("Authentication failed - check MinIO credentials: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error during avatar upload for user {}: {}", userId, e.getMessage(), e);
            throw new Exception("File upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during avatar upload for user {}: {}", userId, e.getMessage(), e);
            throw new Exception("Upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test kết nối MinIO
     */
    private void testMinioConnection() throws Exception {
        try {
            internalMinioClient.listBuckets();
            log.debug("MinIO connection test successful");
        } catch (Exception e) {
            log.error("MinIO connection test failed: {}", e.getMessage());
            throw new Exception("Cannot connect to MinIO server at " + minioConfig.getEndpoint() + ". Check if MinIO is running and credentials are correct.", e);
        }
    }

    /**
     * Upload hình ảnh quiz lên MinIO.
     * @param file File hình ảnh
     * @param quizId Id quiz
     * @return Presigned URL của file
     * @throws Exception Nếu upload lỗi
     */
    @Override
    public String uploadQuizImage(MultipartFile file, Long quizId) throws Exception {
        ensureBucketExists(minioConfig.getImageBucket());
        String fileName = "quiz_" + quizId + "_" + UUID.randomUUID().toString().substring(0, 8) + getFileExtension(file.getOriginalFilename());

        internalMinioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getImageBucket())
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return getPresignedUrl(minioConfig.getImageBucket(), fileName);
    }

    /**
     * Xóa file khỏi MinIO.
     * @param bucketName Tên bucket
     * @param fileName Tên file
     * @throws Exception Nếu xóa lỗi
     */
    @Override
    public void deleteFile(String bucketName, String fileName) throws Exception {
        internalMinioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    /**
     * Lấy presigned URL để truy cập avatar.
     * @param fileName Tên file avatar
     * @return Presigned URL
     */
    @Override
    public String getAvatarUrl(String fileName) throws Exception {
        return getPresignedUrl(minioConfig.getAvatarBucket(), fileName);
    }

    /**
     * Đảm bảo bucket tồn tại, nếu chưa thì tạo mới.
     * @param bucketName Tên bucket
     * @throws Exception Nếu lỗi MinIO
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean found = internalMinioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            internalMinioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * Tạo presigned URL có thời hạn để truy cập file an toàn.
     * @param bucketName Tên bucket
     * @param fileName Tên file
     * @return Presigned URL có thời hạn ngắn theo cấu hình
     */
    private String getPresignedUrl(String bucketName, String fileName) throws Exception {
        return publicMinioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(fileName)
                        .expiry(minioConfig.getPresignedUrlExpiryMinutes(), TimeUnit.MINUTES)
                        .build()
        );
    }

    /**
     * Lấy phần mở rộng của file.
     * @param fileName Tên file
     * @return Phần mở rộng (ví dụ: .jpg)
     */
    private String getFileExtension(String fileName) {
        return fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf("."))
                : "";
    }
}
