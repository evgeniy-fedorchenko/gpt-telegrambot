package com.efedorchenko.gptbot.controller;

import com.efedorchenko.gptbot.logupload.LogUploadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping(path = "/logs")
@AllArgsConstructor
public class LogUploadController {

    private final LogUploadService logUploadService;

    @GetMapping(path = "/download-all")
    public ResponseEntity<StreamingResponseBody> uploadAllLogs(HttpServletResponse response) {

        StreamingResponseBody responseBody = outputStream -> {

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                logUploadService.zippingLogsIn(zipOutputStream);
                log.debug("Logs successfully transferred");

            } catch (IOException e) {
                log.error("Log transfer filed, I/O error has occurred. Ex: {}", e.getMessage());
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "logs.zip");

        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }
}
