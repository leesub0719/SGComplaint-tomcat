package com.transit.SGComplaint.DTO;

import java.nio.file.Path;

public record StoredAttachment(
        Path path,
        String originalName,
        String contentType) {
}
