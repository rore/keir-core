package dev.keir.examples;

import com.fasterxml.jackson.core.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/** Bounded JSON data transport; opaque host keys never pass through this codec. */
final class EffectJson {
    private static final int MAX_BYTES=1_048_576;
    private static final long MAX_INTEGER=9_007_199_254_740_991L;
    private static final JsonFactory JSON=JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(16).maxDocumentLength(MAX_BYTES)
            .maxStringLength(MAX_BYTES).maxNameLength(MAX_BYTES).maxNumberLength(1024).build())
        .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private EffectJson() {}

    static Object parse(byte[] bytes) {
        if(bytes==null || bytes.length>MAX_BYTES) throw invalid();
        try {
            String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            if(text.startsWith("\ufeff")) throw invalid();
            try(JsonParser parser=JSON.createParser(text)) {
                if(parser.nextToken()==null) throw invalid();
                Object value=read(parser,1,new int[]{0});
                if(parser.nextToken()!=null) throw invalid();
                return value;
            }
        } catch(Exception error) { throw invalid(); }
    }

    private static Object read(JsonParser parser,int depth,int[] nodes) throws java.io.IOException {
        if(depth>16 || ++nodes[0]>MAX_BYTES) throw invalid();
        return switch(parser.currentToken()) {
            case START_OBJECT -> {
                Map<String,Object> value=new LinkedHashMap<>();
                while(parser.nextToken()!=JsonToken.END_OBJECT) {
                    if(parser.currentToken()!=JsonToken.FIELD_NAME || ++nodes[0]>MAX_BYTES) throw invalid();
                    String key=parser.currentName(); unicode(key);
                    parser.nextToken(); value.put(key,read(parser,depth+1,nodes));
                }
                yield value;
            }
            case START_ARRAY -> {
                List<Object> value=new ArrayList<>();
                while(parser.nextToken()!=JsonToken.END_ARRAY) value.add(read(parser,depth+1,nodes));
                yield value;
            }
            case VALUE_STRING -> { String value=parser.getText(); unicode(value); yield value; }
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case VALUE_NUMBER_INT -> {
                String raw=parser.getText(); long value=Long.parseLong(raw);
                if(raw.equals("-0") || value < -MAX_INTEGER || value>MAX_INTEGER) throw invalid();
                yield value;
            }
            default -> throw invalid();
        };
    }

    static byte[] encode(Object value) {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        write(value,1,output); return output.toByteArray();
    }
    static Object copy(Object value) { return parse(encode(value)); }

    private static void append(ByteArrayOutputStream output,String text) {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        if(output.size()+bytes.length>MAX_BYTES) throw invalid();
        output.writeBytes(bytes);
    }
    private static void write(Object value,int depth,ByteArrayOutputStream output) {
        if(depth>16) throw invalid();
        if(value instanceof String text) { quoted(text,output); return; }
        if(value instanceof Boolean bool) { append(output,bool.toString()); return; }
        if(value instanceof Integer || value instanceof Long) {
            long integer=((Number)value).longValue();
            if(integer<0 || integer>MAX_INTEGER) throw invalid();
            append(output,Long.toString(integer)); return;
        }
        if(value instanceof List<?> list) {
            append(output,"[");
            for(int i=0;i<list.size();i++) { if(i>0)append(output,","); write(list.get(i),depth+1,output); }
            append(output,"]"); return;
        }
        if(!(value instanceof Map<?,?> map)) throw invalid();
        List<String> keys=new ArrayList<>();
        for(Object key:map.keySet()) { if(!(key instanceof String text))throw invalid(); keys.add(text); }
        keys.sort((left,right)->Arrays.compare(left.codePoints().toArray(),right.codePoints().toArray()));
        append(output,"{");
        for(int i=0;i<keys.size();i++) {
            if(i>0)append(output,","); String key=keys.get(i);
            quoted(key,output); append(output,":"); write(map.get(key),depth+1,output);
        }
        append(output,"}");
    }
    private static void quoted(String text,ByteArrayOutputStream output) {
        unicode(text);
        if(text.getBytes(StandardCharsets.UTF_8).length+2>MAX_BYTES) throw invalid();
        StringBuilder escaped=new StringBuilder("\"");
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='"' || c=='\\') escaped.append('\\').append(c);
            else if(c<32) escaped.append("\\u00").append("0123456789abcdef".charAt(c>>>4)).append("0123456789abcdef".charAt(c&15));
            else escaped.append(c);
        }
        append(output,escaped.append('"').toString());
    }
    static void unicode(String text) {
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(Character.isHighSurrogate(c)) {
                if(++i>=text.length() || !Character.isLowSurrogate(text.charAt(i)))throw invalid();
            } else if(Character.isLowSurrogate(c))throw invalid();
        }
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid JSON data."); }
}
