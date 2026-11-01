package dev.keir.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.keir.runtime.RuntimeData.*;

class Read2StructuralContractsTest {
    private static final Path FIXTURES = Path.of("..", "conformance", "read2-structural");

    @Test void structuralArtifactPositivesDoNotReplaceModelAdmissionOrCurrentAuthority() throws Exception {
        var callbacks = new AtomicInteger();
        var authorizations = new AtomicInteger();
        var bound = GenericRuntimeTest.bound("read","scope",GenericRuntimeTest.reads((a,h,ms)->{
            callbacks.incrementAndGet(); return GenericRuntimeTest.done(GenericRuntimeTest.read("COMPLETED"));
        }));
        byte[] trusted = GenericRuntimeTest.file("read","artifact.json");
        var rows = list(parseJson(Files.readAllBytes(FIXTURES.resolve("residuals.json")),1_048_576),"");
        for (Object value : rows) {
            var row = map(value,"");
            if ("compile".equals(row.get("stage"))) continue;
            String id = (String)row.get("id");
            byte[] bytes = row.containsKey("raw") ? ((String)row.get("raw")).getBytes(StandardCharsets.UTF_8) : canonical(row.get("document"));
            if ("bind".equals(row.get("stage"))) {
                var binding = KeirRuntime.bindModel(bytes,GenericRuntimeTest.approval("read","scope"),GenericRuntimeTest.reads((a,h,ms)->{callbacks.incrementAndGet();return GenericRuntimeTest.done(null);}));
                var expected = map(row.get("expected"),"");
                GenericRuntimeTest.bindingDenied(binding,(String)expected.get("code"),(String)expected.get("path"));
                assertEquals(0,callbacks.get(),id); assertEquals(0,authorizations.get(),id); continue;
            }
            var admission = GenericRuntimeTest.admission("read",trusted);
            // The host test deliberately selects mutated fixture bytes; other trust metadata stays independent.
            admission.put("expectedArtifactDigest",Boolean.TRUE.equals(row.get("badPin")) ? "sha256:"+"0".repeat(64) : digest(bytes));
            if(row.containsKey("admission")) admission.putAll(map(row.get("admission"),""));
            var preparation = KeirRuntime.prepareProgram(bytes,bound,admission);
            var expected = map(row.get("expected"),"");
            if ("invoke".equals(row.get("stage"))) {
                assertTrue(preparation.succeeded(),id);
                var invocation = GenericRuntimeTest.readInvocation("scope",Optional.empty(),request->{authorizations.incrementAndGet();return GenericRuntimeTest.done(false);});
                var outcome = GenericRuntimeTest.await(KeirRuntime.invokeProgram(preparation.prepared(),Map.of("taskId","task1"),invocation));
                assertEquals(expected.get("code"),outcome.error().get("code"),id);
                assertEquals(expected.get("path"),outcome.error().get("path"),id);
                assertEquals(1,authorizations.get(),id);
            } else {
                GenericRuntimeTest.denied(preparation,(String)expected.get("code"),(String)expected.get("path"));
                assertEquals(0,authorizations.get(),id);
            }
            assertEquals(0,callbacks.get(),id);
            authorizations.set(0);
        }
        byte[] literalBytes = Files.readAllBytes(FIXTURES.resolve("artifact-literals.json"));
        var approved = GenericRuntimeTest.admission("read",trusted); approved.put("expectedArtifactDigest",digest(literalBytes));
        assertEquals(Files.readString(FIXTURES.resolve("artifact-literals.sha256")).trim(),digest(literalBytes));
        var prepared = KeirRuntime.prepareProgram(literalBytes,bound,approved); assertTrue(prepared.succeeded());
        var invocation = new KeirRuntime.Invocation2("scope",Map.of(),new KeirRuntime.Deadline(()->0,1000),r->GenericRuntimeTest.done(true),Optional.empty());
        var result = GenericRuntimeTest.await(KeirRuntime.invokeProgram(prepared.prepared(),Map.of("taskId","task1"),invocation));
        assertEquals("COMPLETED",result.classification()); assertEquals(Map.of("classification","DONE"),result.result());
        assertEquals(0,callbacks.get());
    }
}
