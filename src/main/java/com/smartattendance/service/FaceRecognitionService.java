package com.smartattendance.service;

import org.springframework.web.multipart.MultipartFile;

public interface FaceRecognitionService {

    boolean registerFace(String registrationNumber,
                         MultipartFile image);

}