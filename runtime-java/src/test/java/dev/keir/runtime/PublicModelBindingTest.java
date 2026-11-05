package dev.keir.runtime;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static dev.keir.runtime.RuntimeData.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicModelBindingTest {
    @Test void matchesSharedRawBindingCasesWithoutDispatch() throws IOException {
        var dispatches = new AtomicInteger();
        var adapter = new KeirRuntime.BoundEffectAdapter("1", "simulation", "sample.RequestUuid/1",
                (args, protectedArgs, remainingMs, context) -> {
                    dispatches.incrementAndGet();
                    throw new AssertionError("Binding dispatched an adapter");
                });
        var registrations = Map.of("tasks.create", adapter, "tasks.get", adapter);
        var cases = list(parseJson(resource("cases.json"), 4096), "");
        assertEquals(4, cases.size());
        for (Object item : cases) {
            var row = map(item, "");
            var file = (String)row.get("file");
            var expected = row.get("diagnostic");
            var binding = KeirRuntime.bindModel(resource(file), registrations);
            assertEquals(expected == null, binding.succeeded(), file);
            if (expected == null) {
                assertNull(binding.diagnostic(), file);
            } else {
                var diagnostic = binding.diagnostic();
                assertNotNull(diagnostic, file);
                assertEquals(expected, Map.of("code", diagnostic.code(), "path", diagnostic.path()), file);
            }
            assertEquals(0, dispatches.get(), file);
        }
    }

    private static byte[] resource(String file) throws IOException {
        try (var stream = PublicModelBindingTest.class.getResourceAsStream("/model-binding/" + file)) {
            assertNotNull(stream, file);
            return stream.readAllBytes();
        }
    }
}
