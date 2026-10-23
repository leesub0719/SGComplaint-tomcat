package com.transit.SGComplaint.controller;

import com.transit.SGComplaint.DTO.StoredAttachment;
import com.transit.SGComplaint.service.UploadValidation;
import com.transit.SGComplaint.service.DdokBusGuideImageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/route-guide-images")
public class RouteGuideImageController {

    private final DdokBusGuideImageService imageService;

    public RouteGuideImageController(DdokBusGuideImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{imageNo}/image")
    public ResponseEntity<Resource> image(
            @PathVariable(name = "imageNo") Long imageNo) {
        try {
            StoredAttachment image = imageService.getImage(imageNo);
            return ResponseEntity.ok()
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.parseMediaType(UploadValidation.imageContentType(image.path().getFileName().toString())))
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .body(new FileSystemResource(image.path()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }
}
