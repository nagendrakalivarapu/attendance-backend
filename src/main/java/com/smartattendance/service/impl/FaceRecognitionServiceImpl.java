
package com.smartattendance.service.impl;

import com.smartattendance.service.FaceRecognitionService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final Pattern STUDENT_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final RestTemplate restTemplate;

    @Value("${face.api.url}")
    private String faceApiUrl;

    @Value("${face.api.key}")
    private String faceApiKey;

    // =====================================================
    // REGISTER FACE
    // =====================================================

    @Override
    public boolean registerFace(
            String registrationNumber,
            MultipartFile image) {

        validateStudentId(registrationNumber);
        validateImagePresent(image);

        try {

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add("student_id", registrationNumber);
            body.add("image", createImagePart(image));

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, createHeaders());

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            getFaceApiUrl() + "/face/register",
                            request,
                            Map.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {

                return false;
            }

            Map<?, ?> responseBody = response.getBody();

            if (!Boolean.TRUE.equals(responseBody.get("success"))) {
                return false;
            }

            // Ensure FastAPI registered the requested student.
            Object returnedStudentId = responseBody.get("studentId");

            if (!(returnedStudentId instanceof String returnedId)
                    || !registrationNumber.equals(returnedId)) {

                log.error(
                        "Face registration returned an unexpected student identifier."
                );

                return false;
            }

            return true;

        } catch (RestClientException ex) {

            log.error(
                    "Face registration API request failed: {}",
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
                            getFaceApiUrl() + "/face/recognize",
                            request,
                            Map.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {

                throw faceServiceUnavailable();
            }

            Map<?, ?> responseBody = response.getBody();

            if (!Boolean.TRUE.equals(responseBody.get("success"))) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Face could not be recognized."
                );
            }

            Object studentId = responseBody.get("studentId");

            if (!(studentId instanceof String recognizedId)
                    || !STUDENT_ID_PATTERN.matcher(recognizedId).matches()) {

                throw faceServiceUnavailable();
            }

            return recognizedId;

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (RestClientException ex) {

            log.error(
                    "Face recognition API request failed: {}",
                    ex.getClass().getSimpleName()
            );

            throw faceServiceUnavailable();
        }
    }

    // =====================================================
    // COMMON REQUEST HEADERS
    // =====================================================

    private HttpHeaders createHeaders() {

        String apiKey = getFaceApiKey();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        headers.setAccept(
                List.of(MediaType.APPLICATION_JSON)
        );

        headers.set(
                "X-Face-API-Key",
                apiKey
        );

        return headers;
    }

    // =====================================================
    // IMAGE MULTIPART RESOURCE
    // =====================================================

    private HttpEntity<ByteArrayResource> createImagePart(
            MultipartFile image) {

        String contentType = image.getContentType();

        if (!MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)
                && !MediaType.IMAGE_PNG_VALUE.equalsIgnoreCase(contentType)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported image format."
            );
        }

        try {

            byte[] imageBytes = image.getBytes();

            ByteArrayResource resource =
                    new ByteArrayResource(imageBytes) {

                        @Override
                        public String getFilename() {
                            return MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(
                                    contentType
                            ) ? "face-upload.jpg" : "face-upload.png";
                        }
                    };

            HttpHeaders partHeaders = new HttpHeaders();

            partHeaders.setContentType(
                    MediaType.parseMediaType(contentType)
            );

            return new HttpEntity<>(resource, partHeaders);

        } catch (IOException ex) {

            log.warn("Unable to read uploaded face image.");

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read uploaded image."
            );
        }
    }

    // =====================================================
    // VALIDATION
    // =====================================================

    private void validateStudentId(String registrationNumber) {

        if (registrationNumber == null
                || !STUDENT_ID_PATTERN.matcher(registrationNumber).matches()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid student registration number."
            );
        }
    }

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

        return faceApiUrl.trim().replaceAll("/+$", "");
    }

    // =====================================================
    // FACE API KEY
    // =====================================================

    private String getFaceApiKey() {

        if (faceApiKey == null
                || faceApiKey.isBlank()
                || faceApiKey.trim().length() < 32) {

            throw new IllegalStateException(
                    "Face recognition API key is not configured correctly."
            );
        }

        return faceApiKey.trim();
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
