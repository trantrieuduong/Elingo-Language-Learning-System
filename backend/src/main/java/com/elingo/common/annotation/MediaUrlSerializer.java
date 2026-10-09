package com.elingo.common.annotation;

import com.elingo.file.util.FileKey;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.Collection;

public class MediaUrlSerializer extends ValueSerializer<Object> {

    @Value("${r2.public-url}")
    private String publicUrl;

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value instanceof Collection<?> items) {
            gen.writeStartArray();
            for (Object item : items) {
                writeOne(item, gen);
            }
            gen.writeEndArray();
        } else {
            writeOne(value, gen);
        }
    }

    private void writeOne(Object value, JsonGenerator gen) {
        String url = value == null ? null : FileKey.toPublicUrl(publicUrl, value.toString());
        if (url == null) {
            gen.writeNull();
        } else {
            gen.writeString(url);
        }
    }
}
