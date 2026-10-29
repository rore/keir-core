package dev.keir.compiler;

final class CompileFailure extends RuntimeException {
    final String code;
    final String path;

    CompileFailure(String code, String path) {
        super(code, null, false, false);
        this.code = code;
        this.path = path;
    }
}
