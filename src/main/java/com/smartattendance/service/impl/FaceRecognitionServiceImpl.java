
package com.smartattendance.service.impl;

import com.smartattendance.service.FaceRecognitionService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final Pattern STUDENT_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final long MAX_IMAGE_SIZE =
            5L * 1024 * 1024;

    private static final String FACE_API_KEY_HEADER =
            "X-Face-API-Key";

    private static final String REGISTER_ENDPOINT =
            "/face/register";

    private static final String RECOGNIZE_ENDPOINT =
            "/face/recognize";

    private final RestTemplate restTemplate;

    @Value("${face.api.url:}")
    private String faceApiUrl;

    @Value("${face.api.key:}")
    private String faceApiKey;

    // =====================================================
    // STARTUP CONFIGURATION VALIDATION
    // =====================================================

    @PostConstruct
    public void validateFaceApiConfiguration() {

        if (faceApiUrl == null || faceApiUrl.isBlank()) {
            throw new IllegalStateException(
                    "Face recognition API URL is not configured."
            );
        }

        if (faceApiKey == null
                || faceApiKey.isBlank()
                || faceApiKey.trim().length() < 32) {

            throw new IllegalStateException(
                    "Face recognition API key is missing or invalid."
            );
        }

        faceApiUrl = faceApiUrl.trim().replaceAll("/+$", "");
        faceApiKey = faceApiKey.trim();

        try {
            URI uri = URI.create(faceApiUrl);

            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {

                throw new IllegalStateException(
                        "Face recognition API URL is invalid."
                );
            }

        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Face recognition API URL is invalid."
            );
        }

        log.info("Face recognition API configuration validated.");
    }

    // =====================================================
    // REGISTER FACE
    // =====================================================

    @Override
    public boolean registerFace(
            String registrationNumber,
            MultipartFile image) {

        String studentId = normalizeStudentId(registrationNumber);

        validateImagePresent(image);

        try {
            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add("student_id", studentId);
            body.add("image", createImagePart(image));

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, createHeaders());

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            getFaceApiUrl() + REGISTER_ENDPOINT,
                            request,
                            Map.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {

                log.warn(
                        "Face registration returned an empty or unsuccessful HTTP response."
                );

                return false;
            }

            Map<?, ?> responseBody = response.getBody();

            if (!Boolean.TRUE.equals(responseBody.get("success"))) {

                logRegistrationFailure(responseBody);

                return false;
            }

            String returnedStudentId =
                    getString(responseBody, "studentId");

            if (returnedStudentId == null
                    || !studentId.equals(returnedStudentId.trim())) {

                log.error(
                        "Face registration returned an unexpected student identifier."
                );

                return false;
            }

            log.info("Face registration completed successfully.");

            return true;

        } catch (HttpStatusCodeException ex) {

            logUpstreamHttpError(
                    "Face registration",
                    ex
            );

            return false;

        } catch (ResourceAccessException ex) {

            log.error(
                    "Face registration service is unreachable: {}",
                    ex.getClass().getSimpleName()
            );

            return false;

        } catch (RestClientException ex) {

            log.error(
                    "Face registration request failed: {}",
                    ex.getClass().getSimpleName()
            );

            return false;
        }
    }

    // =====================================================
    // RECOGNIZE FACE
    // =====================================================

    @Override
    public String recognizeFace(MultipartFile image) {

        validateImagePresent(image);

        try {
            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add("image", createImagePart(image));

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, createHeaders());

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            getFaceApiUrl() + RECOGNIZE_ENDPOINT,
                            request,
                            Map.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {

                throw faceServiceUnavailable();
            }

            Map<?, ?> responseBody = response.getBody();

            if (!Boolean.TRUE.equals(responseBody.get("success"))) {

                String code = getString(responseBody, "code");

                log.warn(
                        "Face recognition unsuccessful. Service code: {}",
                        safeLogValue(code)
                );

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Face could not be recognized."
                );
            }

            String studentId =
                    getString(responseBody, "studentId");

            if (studentId == null) {
                log.error(
                        "Face recognition response did not contain a student identifier."
                );

                throw faceServiceUnavailable();
            }

            studentId = studentId.trim();

            if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {

                log.error(
                        "Face recognition returned an invalid student identifier."
                );

                throw faceServiceUnavailable();
            }

            return studentId;

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (HttpStatusCodeException ex) {

            logUpstreamHttpError(
                    "Face recognition",
                    ex
            );

            throw faceServiceUnavailable();

        } catch (ResourceAccessException ex) {

            log.error(
                    "Face recognition service is unreachable: {}",
                    ex.getClass().getSimpleName()
            );

            throw faceServiceUnavailable();

        } catch (RestClientException ex) {

            log.error(
                    "Face recognition request failed: {}",
                    ex.getClass().getSimpleName()
            );

            throw faceServiceUnavailable();
        }
    }

    // =====================================================
    // COMMON REQUEST HEADERS
    // =====================================================

    private HttpHeaders createHeaders() {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.MULTIPART_FORM_DATA
        );

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );

        headers.set(
                FACE_API_KEY_HEADER,
                getFaceApiKey()
        );

        return headers;
    }

    // =====================================================
    // IMAGE MULTIPART RESOURCE
    // =====================================================

    private HttpEntity<ByteArrayResource> createImagePart(
            MultipartFile image) {

        String contentType = image.getContentType();

        if (contentType == null
                || (!MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)
                && !MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPEG and PNG images are supported."
            );
        }

        try {
            byte[] imageBytes = image.getBytes();

            if (imageBytes.length == 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Image file is empty."
                );
            }

            if (imageBytes.length > MAX_IMAGE_SIZE) {

                throw new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "Image must not exceed 5 MB."
                );
            }

            String filename =
                    MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)
                            ? "face-upload.jpg"
                            : "face-upload.png";

            ByteArrayResource resource =
                    new ByteArrayResource(imageBytes) {

                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    };

            HttpHeaders partHeaders = new HttpHeaders();

            partHeaders.setContentType(
                    MediaType.parseMediaType(contentType)
            );

            partHeaders.setContentDispositionFormData(
                    "image",
                    filename
            );

            return new HttpEntity<>(
                    resource,
                    partHeaders
            );

        } catch (IOException ex) {

            log.warn(
                    "Unable to read uploaded face image."
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded image."
            );
        }
    }

    // =====================================================
    // STUDENT ID VALIDATION
    // =====================================================

    private String normalizeStudentId(String registrationNumber) {

        if (registrationNumber == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student registration number is required."
            );
        }

        String studentId = registrationNumber.trim();

        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid student registration number."
            );
        }

        return studentId;
    }

    // =====================================================
    // IMAGE VALIDATION
    // =====================================================

    private void validateImagePresent(MultipartFile image) {

        if (image == null || image.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image file is required."
            );
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {

            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must not exceed 5 MB."
            );
        }
    }

    // =====================================================
    // FACE API URL
    // =====================================================

    private String getFaceApiUrl() {

        if (faceApiUrl == null || faceApiUrl.isBlank()) {

            throw new IllegalStateException(
                    "Face recognition API URL is not configured."
            );
        }

        return faceApiUrl;
    }

    // =====================================================
    // FACE API KEY
    // =====================================================

    private String getFaceApiKey() {

        if (faceApiKey == null
                || faceApiKey.isBlank()
                || faceApiKey.length() < 32) {

            throw new IllegalStateException(
                    "Face recognition API key is not configured correctly."
            );
        }

        return faceApiKey;
    }

    // =====================================================
    // RESPONSE HELPERS
    // =====================================================

    private String getString(
            Map<?, ?> responseBody,
            String key) {

        Object value = responseBody.get(key);

        if (value instanceof String stringValue
                && !stringValue.isBlank()) {

            return stringValue;
        }

        return null;
    }

    private void logRegistrationFailure(Map<?, ?> responseBody) {

        String code = getString(responseBody, "code");
        String message = getString(responseBody, "message");

        log.warn(
                "Face registration unsuccessful. Code: {}, Message: {}",
                safeLogValue(code),
                safeLogValue(message)
        );
    }

    private void logUpstreamHttpError(
            String operation,
            HttpStatusCodeException ex) {

        log.error(
                "{} API returned HTTP status {}.",
                operation,
                ex.getStatusCode().value()
        );
    }

    private String safeLogValue(String value) {

        if (value == null) {
            return "N/A";
        }

        String sanitized = value
                .replaceAll("[\\r\\n\\t]", " ")
                .replaceAll("[\\p{Cntrl}]", "");

        if (sanitized.length() > 120) {
            sanitized = sanitized.substring(0, 120);
        }

        return sanitized;
    }

    // =====================================================
    // GENERIC UPSTREAM ERROR
    // =====================================================

    private ResponseStatusException faceServiceUnavailable() {

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Face recognition service is currently unavailable."
        );
    }
}
