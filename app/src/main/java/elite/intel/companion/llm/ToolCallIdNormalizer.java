package elite.intel.companion.llm;

import elite.intel.companion.model.llm.LlmMessage;
import elite.intel.companion.model.llm.LlmToolInvocation;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Produces a wire-safe copy of a tool-call transcript. Every non-blank id becomes exactly nine ASCII
 * alphanumeric characters, while matching assistant calls and tool results retain the same rewritten id.
 * The mapping is deterministic for a request so replayed history stays stable and prompt-cache friendly.
 */
final class ToolCallIdNormalizer {

    private static final int ID_LENGTH = 9;
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(ALPHANUMERIC.length());
    private static final BigInteger ID_SPACE = BASE.pow(ID_LENGTH);

    private ToolCallIdNormalizer() {
    }

    /**
     * Rewrites only ids exposed by tool-call wire protocols; message content and tool arguments are preserved.
     */
    static List<LlmMessage> forWire(List<LlmMessage> messages) {
        Map<String, String> ids = normalizedIds(messages);
        if (ids.isEmpty()) {
            return messages;
        }

        List<LlmMessage> normalized = new ArrayList<>(messages.size());
        boolean changed = false;
        for (LlmMessage message : messages) {
            List<LlmToolInvocation> calls = new ArrayList<>(message.toolCalls().size());
            boolean callsChanged = false;
            for (LlmToolInvocation call : message.toolCalls()) {
                String id = normalizedId(ids, call.id());
                if (Objects.equals(id, call.id())) {
                    calls.add(call);
                } else {
                    callsChanged = true;
                    calls.add(new LlmToolInvocation(id, call.name(), call.arguments()));
                }
            }

            String toolCallId = normalizedId(ids, message.toolCallId());
            if (!callsChanged && Objects.equals(toolCallId, message.toolCallId())) {
                normalized.add(message);
                continue;
            }
            changed = true;
            normalized.add(new LlmMessage(message.role(), message.content(), toolCallId,
                    callsChanged ? calls : message.toolCalls()));
        }
        return changed ? List.copyOf(normalized) : messages;
    }

    private static Map<String, String> normalizedIds(List<LlmMessage> messages) {
        Map<String, String> ids = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        for (LlmMessage message : messages) {
            for (LlmToolInvocation call : message.toolCalls()) {
                register(ids, used, call.id());
            }
            register(ids, used, message.toolCallId());
        }
        return ids;
    }

    private static void register(Map<String, String> ids, Set<String> used, String rawId) {
        if (rawId == null || rawId.isBlank() || ids.containsKey(rawId)) {
            return;
        }
        String candidate = isWireSafe(rawId) ? rawId : compactId(rawId, 0);
        int collision = 0;
        while (!used.add(candidate)) {
            candidate = compactId(rawId, ++collision);
        }
        ids.put(rawId, candidate);
    }

    private static String normalizedId(Map<String, String> ids, String rawId) {
        return rawId == null || rawId.isBlank() ? rawId : ids.getOrDefault(rawId, rawId);
    }

    private static boolean isWireSafe(String id) {
        if (id.length() != ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    private static String compactId(String rawId, int collision) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(rawId.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Integer.toString(collision).getBytes(StandardCharsets.UTF_8));
            BigInteger value = new BigInteger(1, digest.digest()).mod(ID_SPACE);
            char[] id = new char[ID_LENGTH];
            for (int i = ID_LENGTH - 1; i >= 0; i--) {
                BigInteger[] division = value.divideAndRemainder(BASE);
                id[i] = ALPHANUMERIC.charAt(division[1].intValue());
                value = division[0];
            }
            return new String(id);
        } catch (NoSuchAlgorithmException missingSha256) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime", missingSha256);
        }
    }
}
