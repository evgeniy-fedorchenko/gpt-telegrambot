package com.efedorchenko.gptbot.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DataFormatUtils {


    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";


    private static final Pattern BASE64_PATTERN = Pattern.compile("[A-Za-z0-9+/]{4000,}={0,2}");
    private static final int MIN_BASE64_LENGTH = 5000;

    public static String excludeBase64(String jsonString) {
        StringBuilder result = new StringBuilder(jsonString);
        Matcher matcher = BASE64_PATTERN.matcher(jsonString);

        while (matcher.find()) {
            String base64Candidate = matcher.group();
            if (base64Candidate.length() >= MIN_BASE64_LENGTH && isValidBase64(base64Candidate)) {
                result.replace(matcher.start(), matcher.end(), "<base64_encoding_string>");
                matcher.reset(result.toString());
            }
        }
        return result.toString();
    }

    private static boolean isValidBase64(String str) {
        if (str.length() % 4 != 0) {
            return false;
        }
        String start = str.substring(0, Math.min(100, str.length()));
        String end = str.substring(Math.max(0, str.length() - 100));

        return start.matches("^[A-Za-z0-9+/]+") && end.matches("[A-Za-z0-9+/]+=*$");
    }

}
