package dev.keir.examples;

/** Finite reservations owned by one lexical harness, with no queue or global cache. */
final class GenericCapacity {
    static final int MODEL_BYTES=65_536, ARTIFACT_BYTES=1_048_576;
    private int models, prepared, modelBytes, descriptorBytes, artifactBytes, inflight;
    private boolean working;
    static void capacity(boolean allowed) { if(!allowed) throw new IllegalArgumentException("CAPACITY_EXCEEDED"); }
    static void scenarios(int fileBytes,int rows,int rowBytes,int stubs,int calls) {
        capacity(fileBytes>=0&&fileBytes<=1_048_576&&rows>=0&&rows<=128&&rowBytes>=0&&rowBytes<=65_536&&stubs>=0&&stubs<=4&&calls>=0&&calls<=4);
    }
    synchronized Lease work() { capacity(!working); working=true; return new Lease(()->{ synchronized(this) { working=false; } }); }
    synchronized Lease model(int raw,int canonical,int descriptors) {
        capacity(raw>=0&&raw<=MODEL_BYTES&&canonical>=0&&canonical<=MODEL_BYTES&&descriptors>=0&&descriptors<=MODEL_BYTES
            &&models<4&&modelBytes+canonical<=262_144&&descriptorBytes+descriptors<=262_144);
        models++; modelBytes+=canonical; descriptorBytes+=descriptors;
        return new Lease(()->{ synchronized(this) { models--; modelBytes-=canonical; descriptorBytes-=descriptors; } });
    }
    synchronized Lease program(int bytes) {
        capacity(bytes>=0&&bytes<=ARTIFACT_BYTES&&prepared<8&&artifactBytes+bytes<=8_388_608);
        prepared++; artifactBytes+=bytes;
        return new Lease(()->{ synchronized(this) { prepared--; artifactBytes-=bytes; } });
    }
    synchronized Lease invocation() { capacity(inflight<2); inflight++; return new Lease(()->{ synchronized(this) { inflight--; } }); }
    static final class Lease implements AutoCloseable {
        private Runnable release;
        Lease(Runnable release) { this.release=release; }
        public synchronized void close() { if(release!=null) { release.run(); release=null; } }
    }
}
