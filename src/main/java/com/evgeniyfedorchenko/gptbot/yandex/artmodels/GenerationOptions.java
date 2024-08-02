package com.evgeniyfedorchenko.gptbot.yandex.artmodels;

import lombok.Builder;
import lombok.Setter;
import org.springframework.http.MediaType;

import java.util.Random;

@Setter
@Builder
public class GenerationOptions {

    @Builder.Default
    private final String mimeType = MediaType.IMAGE_JPEG_VALUE;

    @Builder.Default
    private final long seed = new Random().nextLong(0, Long.MAX_VALUE);

    @Builder.Default
    private final AspectRatio aspectRatio = new AspectRatio(1L, 1L);


    public record AspectRatio(long widthRatio, long heightRatio) { }

}
