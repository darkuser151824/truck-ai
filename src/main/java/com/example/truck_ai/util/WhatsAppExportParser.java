package com.example.truck_ai.util;

import com.example.truck_ai.dto.IngestionRequest;
import com.example.truck_ai.enums.SourceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WhatsAppExportParser {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppExportParser.class);

    // Matches a line starting with a WhatsApp export date stamp, in either
    // Android ("12/07/23, 10:15 am - ...") or iOS ("[12/07/23, 10:15:32 AM] ...") form.
    private static final Pattern DATE_PREFIX = Pattern.compile("^\\[?\\d{1,2}/\\d{1,2}/\\d{2,4},");

    // Full "date, time - Sender: message" / "[date, time] Sender: message" line.
    // Lines with a date prefix that don't match this (e.g. "... - Ramesh added Suresh")
    // are WhatsApp system/notification lines, not driver messages.
    private static final Pattern MESSAGE_LINE = Pattern.compile(
            "^\\[?(\\d{1,2}/\\d{1,2}/\\d{2,4}),\\s*(\\d{1,2}:\\d{2}(?::\\d{2})?\\s?(?:[APap][Mm])?)\\]?\\s*-?\\s*([^:]+):\\s(.*)$"
    );

    private WhatsAppExportParser() {
    }

    public static List<IngestionRequest> parse(Long tripId, String rawDump) {
        log.info("Parsing WhatsApp dump: tripId={}, length={} chars", tripId, rawDump == null ? 0 : rawDump.length());

        List<IngestionRequest> messages = new ArrayList<>();
        if (rawDump == null || rawDump.isBlank()) {
            log.warn("Empty/null rawDump for tripId={}, returning zero messages", tripId);
            return messages;
        }

        String currentSender = null;
        StringBuilder currentText = null;
        int lineNumber = 0;

        for (String line : rawDump.split("\\r?\\n")) {
            lineNumber++;
            if (line.isBlank()) {
                log.trace("Line {}: blank, skipping", lineNumber);
                continue;
            }

            if (DATE_PREFIX.matcher(line).find()) {
                log.trace("Line {}: date-prefixed: '{}'", lineNumber, line);
                flush(messages, tripId, currentSender, currentText);
                currentSender = null;
                currentText = null;

                Matcher matcher = MESSAGE_LINE.matcher(line);
                if (matcher.matches()) {
                    currentSender = matcher.group(3).trim();
                    currentText = new StringBuilder(line);
                    log.debug("Line {}: opened new message, sender='{}'", lineNumber, currentSender);
                } else {
                    log.debug("Line {}: date-prefixed but no 'Sender: text' pattern — treating as system/notification line, skipping: '{}'", lineNumber, line);
                }
            } else if (currentText != null) {
                log.trace("Line {}: no date prefix, appending as continuation of sender='{}': '{}'", lineNumber, currentSender, line);
                currentText.append("\n").append(line);
            } else {
                log.debug("Line {}: stray line with no open message context, skipping: '{}'", lineNumber, line);
            }
        }
        flush(messages, tripId, currentSender, currentText);

        log.info("Finished parsing WhatsApp dump: tripId={}, {} messages extracted", tripId, messages.size());
        return messages;
    }

    private static void flush(List<IngestionRequest> messages, Long tripId, String sender, StringBuilder text) {
        if (sender == null || text == null) {
            log.trace("Flush called with nothing open (sender/text null), no-op");
            return;
        }
        IngestionRequest request = new IngestionRequest();
        request.setTripId(tripId);
        request.setSourceChannel(SourceChannel.WHATSAPP);
        request.setSenderName(sender);
        request.setRawText(text.toString());
        messages.add(request);
        log.debug("Flushed message: sender='{}', rawText='{}'", sender, text);
    }
}
