package com.elingo.community.moderation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;
import java.util.List;

public record OpenAiModerationRequest(String model, List<Input> input) {
    public interface Input {}

    public record TextInput(String type, String text) implements Input {
        public TextInput(String text) { this("text", text); }
    }

    public record ImageInput(String type, @JsonProperty("image_url") ImageUrl imageUrl) implements Input {
        public ImageInput(ImageUrl imageUrl) { this("image_url", imageUrl); }
    }

    public record ImageUrl(String url) {
        public static ImageUrl from(String mimeType, byte[] bytes) {
            return new ImageUrl("data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes));
        }
        public static ImageUrl from(String publicUrl) {
            return new ImageUrl(publicUrl);
        }
    }
}
