package com.elingo.admin.content.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.admin.content.service.AdminTagService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.vocabulary.dto.response.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AdminApiPaths.TAGS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Tags",
        description = "Management endpoints for vocabulary and lesson tags in the admin console"
)
public class AdminTagController {

    private final AdminTagService adminTagService;

    @GetMapping
    @Operation(
            summary = "Get all tags",
            description = "Retrieves all available tags for decks and lessons."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tags retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetAllTagsSuccessResponse",
                            summary = "Sample tags list response",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "label": "Travel & Tourism"
                                        },
                                        {
                                          "id": 2,
                                          "label": "Business & Finance"
                                        }
                                      ]
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<List<TagResponse>> getAllTags() {
        return ApiResponse.<List<TagResponse>>builder()
                .success(true)
                .data(adminTagService.getAllTags())
                .build();
    }

    @GetMapping("/{tagId}")
    @Operation(
            summary = "Get tag details by ID",
            description = "Retrieves details of a specific tag by its identifier."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tag retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetTagByIdSuccessResponse",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "label": "Travel & Tourism"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<TagResponse> getTagById(
            @Parameter(description = "Identifier of the tag", example = "1", required = true)
            @PathVariable Long tagId) {
        return ApiResponse.<TagResponse>builder()
                .success(true)
                .data(adminTagService.getTagById(tagId))
                .build();
    }

    @PostMapping
    @Operation(
            summary = "Create a new tag",
            description = "Creates a new tag. Tag label must be unique and non-blank."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Tag creation payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "CreateTagRequestExample",
                            value = """
                                    {
                                      "label": "Technology & AI"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tag created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreateTagSuccessResponse",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 3,
                                        "label": "Technology & AI"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<TagResponse> createTag(
            @Valid @RequestBody TagRequest request) {
        return ApiResponse.<TagResponse>builder()
                .success(true)
                .data(adminTagService.createTag(request))
                .build();
    }

    @PutMapping("/{tagId}")
    @Operation(
            summary = "Update an existing tag",
            description = "Updates the label of an existing tag. The new label must be unique."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Tag update payload",
            content = @Content(
                    examples = @ExampleObject(
                            name = "UpdateTagRequestExample",
                            value = """
                                    {
                                      "label": "Information Technology"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tag updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdateTagSuccessResponse",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 3,
                                        "label": "Information Technology"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<TagResponse> updateTag(
            @Parameter(description = "Identifier of the tag to update", example = "3", required = true)
            @PathVariable Long tagId,
            @Valid @RequestBody TagRequest request) {
        return ApiResponse.<TagResponse>builder()
                .success(true)
                .data(adminTagService.updateTag(tagId, request))
                .build();
    }

    @DeleteMapping("/{tagId}")
    @Operation(
            summary = "Delete a tag",
            description = "Deletes a tag by its ID. Throws error if the tag is currently referenced by any deck or lesson."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tag deleted successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DeleteTagSuccessResponse",
                            value = """
                                    {
                                      "success": true
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<Void> deleteTag(
            @Parameter(description = "Identifier of the tag to delete", example = "3", required = true)
            @PathVariable Long tagId) {
        adminTagService.deleteTag(tagId);
        return ApiResponse.<Void>builder()
                .success(true)
                .build();
    }
}
