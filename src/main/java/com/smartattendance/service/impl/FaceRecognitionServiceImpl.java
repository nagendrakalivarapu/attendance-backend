package com.smartattendance.service.impl;

import com.smartattendance.service.FaceRecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FaceRecognitionServiceImpl
        implements FaceRecognitionService {

    private final RestTemplate restTemplate;

    private static final String FACE_API =
            "http://localhost:8000/face/register";

    @Override
    public boolean registerFace(
            String registrationNumber,
            MultipartFile image) {

        try {

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            body.add("student_id", registrationNumber);

            body.add("image",
                    new ByteArrayResource(image.getBytes()) {

                        @Override
                        public String getFilename() {
                            return image.getOriginalFilename();
                        }
                    });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            FACE_API,
                            request,
                            String.class
                    );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
}