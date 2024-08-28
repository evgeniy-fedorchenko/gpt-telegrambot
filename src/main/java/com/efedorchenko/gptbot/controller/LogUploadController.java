package com.efedorchenko.gptbot.controller;

import com.efedorchenko.gptbot.logupload.LogUploadService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Base64;
import java.util.zip.ZipOutputStream;

/**
 * Черновой вариант передачи логов. Ныне не используется, так как контроллеры недоступны извне
 * И в целом, для обеспечения нужно уровня безопасности такого запроса требуется много усилий,
 * которые лучше направить на перенос сервиса в облако и ходить за логами уже туда напрямую
 */
@Slf4j
@RestController
@RequestMapping(path = "/logs")
@AllArgsConstructor
public class LogUploadController {

    private final LogUploadService logUploadService;

    @GetMapping(path = "/download-all")
    public ResponseEntity<StreamingResponseBody> uploadAllLogs(HttpServletRequest request,
                                                               HttpServletResponse response) {

        if (!checkAuth(request)) {
            log.warn("Unauthorized request: {}", request.getRequestURL());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        StreamingResponseBody responseBody = outputStream -> {

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                logUploadService.zippingLogsIn(zipOutputStream);
                log.debug("Logs successfully transferred");

            } catch (IOException e) {
                log.error("Log transfer filed, I/O error has occurred. Ex: {}", e.getMessage());
            }
        };
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        responseHeaders.setContentDispositionFormData("attachment", "logs.zip");

        return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);
    }

    private boolean checkAuth(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Basic ")) {
            return false;
        }
        byte[] decode = Base64.getDecoder().decode(header.substring(6));
        return new String(decode).equals("login:password");
    }
}
