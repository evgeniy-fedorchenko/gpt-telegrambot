package com.efedorchenko.gptbot.logupload;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class LogUploadService {

    public void zippingLogsIn(ZipOutputStream zipOutputStream) throws IOException {

        Path logsPath = Paths.get(System.getProperty("user.dir"), "logs");
        if (!Files.exists(logsPath) || !Files.isDirectory(logsPath)) {
            log.error("Logs directory not found or is not a directory: {}", logsPath);
            return;
        }
        Files.walkFileTree(logsPath, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = logsPath.relativize(file);
                zipOutputStream.putNextEntry(new ZipEntry(relativePath.toString().replace('\\', '/')));
                Files.copy(file, zipOutputStream);
                zipOutputStream.closeEntry();

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(logsPath)) {
                    Path relativePath = logsPath.relativize(dir);
                    String name = relativePath.toString().replace('\\', '/') + "/";
                    zipOutputStream.putNextEntry(new ZipEntry(name));
                    zipOutputStream.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
