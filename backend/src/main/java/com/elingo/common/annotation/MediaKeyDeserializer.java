package com.elingo.common.annotation;

import com.elingo.file.util.FileKey;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

public class MediaKeyDeserializer extends ValueDeserializer<Object> {

    @Value("${r2.public-url}")
    private String publicUrl;

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> keys = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                keys.add(toKey(p.getValueAsString(), ctxt));
            }
            return keys;
        }
        return toKey(p.getValueAsString(), ctxt);
    }

    private String toKey(String value, DeserializationContext ctxt) {
        try {
            return FileKey.toKey(publicUrl, value);
        } catch (IllegalArgumentException e) {
            throw ctxt.weirdStringException(value, String.class, e.getMessage());
        }
    }
}
