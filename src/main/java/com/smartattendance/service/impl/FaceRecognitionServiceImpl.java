package com.smartattendance.service.impl;

import com.smartattendance.service.FaceRecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FaceRecognitionServiceImpl
        implements FaceRecognitionService {

    @Value("${face.api.url}")
    private String faceApiUrl;

    private final RestTemplate restTemplate;

    @Override
    public boolean registerFace(
            String registrationNumber,
            MultipartFile image) {

        try {

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "student_id",
                    registrationNumber
            );

            body.add(
                    "image",
                    new ByteArrayResource(image.getBytes()) {

                        @Override
                        public String getFilename() {
                            return image.getOriginalFilename();
                        }
                    }
            );

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.MULTIPART_FORM_DATA
            );

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            String url =
                    faceApiUrl + "/face/register";

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class
                    );

            return response.getStatusCode()
                    .is2xxSuccessful();

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    @Override
    public String recognizeFace(
            MultipartFile image) {

        try {

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add(
                    "image",
                    new ByteArrayResource(image.getBytes()) {

                        @Override
                        public String getFilename() {
                            return image.getOriginalFilename();
                        }
                    }
            );

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.MULTIPART_FORM_DATA
            );

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            String url =
                    faceApiUrl + "/face/recognize";

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            Map.class
                    );

            if (!response.getStatusCode()
                    .is2xxSuccessful()
                    || response.getBody() == null) {

                throw new RuntimeException(
                        "Face recognition service failed."
                );
            }

            Map responseBody =
                    response.getBody();

            Object success =
                    responseBody.get("success");

            if (!Boolean.TRUE.equals(success)) {

                throw new RuntimeException(
                        "Face not recognized."
                );
            }

            Object studentId =
                    responseBody.get("studentId");

            if (studentId == null) {

                throw new RuntimeException(
                        "No student ID returned by face recognition."
                );
            }

            return studentId.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Face recognition failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}