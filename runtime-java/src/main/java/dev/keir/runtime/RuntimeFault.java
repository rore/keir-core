package dev.keir.runtime;

final class RuntimeFault extends RuntimeException {
    final String code;
    final String path;

    RuntimeFault(String code, String path) {
        super(code, null, false, false);
        this.code = code;
        this.path = path;
    }
}
