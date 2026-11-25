package com.elingo.learning.lesson.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.dto.response.TagResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Lesson information for the admin console (any status)")
public record AdminLessonResponse(
        @Schema(description = "Lesson id", example = "7") 
        Long id,
                        
        @Schema(description = "Lesson title", example = "How to Order Coffee in English") 
        String title,
                        
        @Schema(description = "URL slug (never changes after creation)", 
                example = "how-to-order-coffee-in-english-3fa9c1d2") 
        String slug,
                        
        @Schema(description = "Lesson description", example = "A short everyday conversation.") 
        String description,
                        
        @Schema(description = "Whether the lesson requires a premium subscription", example = "false") 
        Boolean isPremium,
                        
        @Schema(description = "Lesson status", example = "DRAFT") 
        LessonStatus status,
                        
        @Schema(description = "Video duration in milliseconds", example = "212000") 
        Integer durationMs,
                        
        @Schema(description = "YouTube source URL as entered by the admin", 
                example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        String sourceUrl, // Không @BuildMediaUrl
        
        @Schema(description = "Public CDN URL of the thumbnail (null if none)", 
                example = "https://cdn.elingo.pro/system/lessons/5f1c.png")
        @BuildMediaUrl String thumbnailUrl,
                        
        @Schema(description = "First publication time (null while draft)", example = "2026-11-21T10:30:00") 
        LocalDateTime publishedAt,
                        
        @Schema(description = "Number of segments of the lesson", example = "12") 
        Integer segmentCount,
                        
        @Schema(description = "Creation time", example = "2026-11-20T09:00:00") 
        LocalDateTime createdAt,
                        
        @Schema(description = "Last update time", example = "2026-11-21T10:30:00") 
        LocalDateTime updatedAt,
                        
        @Schema(description = "Tags of the lesson") 
        Set<TagResponse> tags,
                        
        @Schema(description = "CEFR levels of the lesson") 
        Set<CefrLevelResponse> cefrLevels
) {
}
