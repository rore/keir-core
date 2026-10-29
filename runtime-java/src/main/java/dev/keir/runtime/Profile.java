package dev.keir.runtime;

import java.util.Map;

final class Profile {
    static final Map<String, String> ENVIRONMENT = Map.of(
            "celJava", "0.14.0", "celNode", "0.6.1", "protobufJava", "4.35.1", "protobufNode", "2.6.2");
    static final int ARTIFACT_BYTES = 1_048_576;
    static final int DCM_BYTES = 65_536;
    static final int STEPS = 8;
    static final int CALLS = 4;
    static final int TOTAL_AST_NODES = 512;
    static final int TRACE_EVENTS = 64;
    static final int STRING_BYTES = 256;
    static final int VALUE_BYTES = 8_192;
    static final long MAX_INTEGER = 9_007_199_254_740_991L;

    private Profile() {}
}
