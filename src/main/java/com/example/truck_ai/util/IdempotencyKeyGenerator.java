package com.example.truck_ai.util;

import com.example.truck_ai.enums.SourceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IdempotencyKeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyGenerator.class);

    private IdempotencyKeyGenerator() {
    }

    public static String generate(Long tripId, SourceChannel sourceChannel, String senderName, String rawText) {
        log.trace("generate() start: tripId={}, sourceChannel={}, senderName={}, rawTextLength={}",
                tripId, sourceChannel, senderName, rawText == null ? 0 : rawText.length());

        String raw = tripId + "|" + sourceChannel + "|" + senderName + "|" + rawText;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            String key = HexFormat.of().formatHex(hash);
            log.debug("generate() done: tripId={}, sourceChannel={}, senderName={} -> idempotencyKey={}",
                    tripId, sourceChannel, senderName, key);
            return key;
        } catch (NoSuchAlgorithmException e) {
            log.error("generate() failed: SHA-256 algorithm not available", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
