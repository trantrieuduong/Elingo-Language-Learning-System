package com.elingo.community.service.impl;

import com.elingo.common.dto.CursorResponse;
import com.elingo.common.enums.MediaKind;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.CursorUtils;
import com.elingo.community.dto.request.CreatePostRequest;
import com.elingo.community.dto.request.UpdatePostRequest;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.MediaType;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostGroup;
import com.elingo.community.entity.PostLike;
import com.elingo.community.entity.PostLikeId;
import com.elingo.community.entity.PostMedia;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.mapper.PostMapper;
import com.elingo.community.repository.PostGroupRepository;
import com.elingo.community.repository.PostLikeRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.PostService;
import com.elingo.file.util.FileKey;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "POST-SERVICE")
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostGroupRepository postGroupRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long currentUserId) {
        PostGroup group = postGroupRepository.findById(request.groupId())
                .orElseThrow(() -> new AppException(AppError.POST_GROUP_NOT_FOUND));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Post post = Post.builder()
                .user(user)
                .group(group)
                .title(request.title())
                .content(request.content())
                .status(PostStatus.PENDING)
                .likeCount(0)
                .commentCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        List<String> mediaKeys = request.mediaUrls() != null ? request.mediaUrls() : List.of();
        for (int i = 0; i < mediaKeys.size(); i++) {
            String verifiedKey = mediaKeys.get(i);
            requireOwned(verifiedKey, currentUserId);

            String uploadsKey = requireMediaKind(verifiedKey, MediaKind.IMAGE, MediaKind.VIDEO);
            MediaKind mediaKind = FileKey.mediaKindOf(verifiedKey);
            MediaType mediaType = MediaType.valueOf(mediaKind.name());

            PostMedia media = PostMedia.builder()
                    .post(savedPost)
                    .mediaType(mediaType)
                    .mediaUrl(uploadsKey)
                    .order(i)
                    .build();

            savedPost.getMedias().add(media);
            publisher.publishEvent(new FileAttachedEvent(verifiedKey, uploadsKey));
        }

        log.info("Post created postId={} groupId={} mediaCount={}", savedPost.getId(), group.getId(), savedPost.getMedias().size());
        return postMapper.toPostResponse(savedPost, false);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getPublishedPosts(String cursor, int limit, Long groupId, Long currentUserId) {
        int safeLimit = Math.clamp(limit, 1, 50);
        CursorUtils.CursorData cursorData = CursorUtils.decode(cursor);

        LocalDateTime cursorCreatedAt = cursorData != null ? cursorData.createdAt() : null;
        Long cursorId = cursorData != null ? cursorData.id() : null;
        PageRequest pageRequest = PageRequest.of(0, safeLimit + 1);

        List<Post> posts = (groupId != null)
                ? postRepository.findPublishedPostsByGroupAndCursor(groupId, cursorCreatedAt, cursorId, pageRequest)
                : postRepository.findPublishedPostsByCursor(cursorCreatedAt, cursorId, pageRequest);

        return buildCursorResponse(posts, safeLimit, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<PostResponse> getUserPosts(Long targetUserId, String cursor, int limit, Long currentUserId) {
        int safeLimit = Math.clamp(limit, 1, 50);
        CursorUtils.CursorData cursorData = CursorUtils.decode(cursor);
        boolean onlyPublished = currentUserId == null || !currentUserId.equals(targetUserId);

        List<Post> posts = postRepository.findUserPostsByCursor(
                targetUserId,
                onlyPublished,
                cursorData != null ? cursorData.createdAt() : null,
                cursorData != null ? cursorData.id() : null,
                PageRequest.of(0, safeLimit + 1)
        );

        return buildCursorResponse(posts, safeLimit, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (post.getStatus() == PostStatus.DELETED) {
            throw new AppException(AppError.POST_NOT_FOUND);
        }

        if (post.getStatus() != PostStatus.PUBLISHED) {
            boolean canView = (currentUserId != null && currentUserId.equals(post.getUser().getId()));
            if (!canView) {
                throw new AppException(AppError.POST_NOT_FOUND);
            }
        }

        boolean likedByMe = currentUserId != null
                && postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId);

        return postMapper.toPostResponse(post, likedByMe);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request, Long currentUserId) {
        Post post = postRepository.findByIdWithMedia(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (post.getStatus() == PostStatus.DELETED) {
            throw new AppException(AppError.POST_NOT_FOUND);
        }

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new AppException(AppError.POST_ACCESS_DENIED);
        }

        post.setTitle(request.title());
        post.setContent(request.content());

        List<PostMedia> currentMedias = post.getMedias();
        Map<String, PostMedia> existingByUrl = currentMedias.stream()
                .collect(Collectors.toMap(PostMedia::getMediaUrl, m -> m, (a, b) -> a));

        List<String> newKeys = request.mediaUrls() != null ? request.mediaUrls() : List.of();
        List<PostMedia> retainedOrNewMedias = new ArrayList<>();
        Set<PostMedia> keptMedias = new HashSet<>();

        for (int i = 0; i < newKeys.size(); i++) {
            String key = newKeys.get(i);
            if (FileKey.isInZone(key, FileKey.UPLOADS_PREFIX)) {
                PostMedia existing = existingByUrl.get(key);
                if (existing == null) {
                    throw new AppException(AppError.INVALID_REQUEST);
                }
                existing.setOrder(i);
                retainedOrNewMedias.add(existing);
                keptMedias.add(existing);
            } else {
                requireOwned(key, currentUserId);
                String uploadsKey = requireMediaKind(key, MediaKind.IMAGE, MediaKind.VIDEO);
                MediaKind mediaKind = FileKey.mediaKindOf(key);
                MediaType mediaType = MediaType.valueOf(mediaKind.name());

                PostMedia newMedia = PostMedia.builder()
                        .post(post)
                        .mediaType(mediaType)
                        .mediaUrl(uploadsKey)
                        .order(i)
                        .build();

                retainedOrNewMedias.add(newMedia);
                publisher.publishEvent(new FileAttachedEvent(key, uploadsKey));
            }
        }

        // Những media cũ không còn trong danh sách giữ lại sẽ bị gỡ bỏ
        List<String> deletedUrls = new ArrayList<>();
        List<PostMedia> toRemove = new ArrayList<>();
        for (PostMedia existing : currentMedias) {
            if (!keptMedias.contains(existing)) {
                toRemove.add(existing);
                deletedUrls.add(existing.getMediaUrl());
            }
        }

        currentMedias.removeAll(toRemove);
        for (PostMedia m : retainedOrNewMedias) {
            if (!currentMedias.contains(m)) {
                currentMedias.add(m);
            }
        }

        if (!deletedUrls.isEmpty()) {
            publisher.publishEvent(new FileDeletedEvent(deletedUrls));
        }

        boolean likedByMe = postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId);
        post.setStatus(PostStatus.PENDING);
        log.info("Post updated postId={} mediaCount={}", postId, currentMedias.size());
        return postMapper.toPostResponse(post, likedByMe);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new AppException(AppError.POST_ACCESS_DENIED);
        }

        if (post.getStatus() == PostStatus.DELETED) {
            throw new AppException(AppError.POST_NOT_FOUND);
        }

        // Xóa mềm: cập nhật trạng thái sang DELETED
        post.setStatus(PostStatus.DELETED);
        log.info("Post deleted postId={}", postId);
    }

    @Override
    @Transactional
    public void likePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(AppError.POST_NOT_PUBLISHED);
        }

        if (postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId)) {
            throw new AppException(AppError.POST_ALREADY_LIKED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        PostLike postLike = PostLike.builder()
                .id(new PostLikeId(postId, currentUserId))
                .post(post)
                .user(user)
                .build();

        postLikeRepository.save(postLike);
        postRepository.updateLikeCount(postId, 1);
        log.info("Post liked postId={}", postId);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (post.getStatus() == PostStatus.DELETED) {
            throw new AppException(AppError.POST_NOT_FOUND);
        }

        if (!postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId)) {
            throw new AppException(AppError.POST_NOT_LIKED);
        }

        postLikeRepository.deleteByIdPostIdAndIdUserId(postId, currentUserId);
        postRepository.updateLikeCount(postId, -1);
        log.info("Post unliked postId={}", postId);
    }

    private CursorResponse<PostResponse> buildCursorResponse(List<Post> posts, int limit, Long currentUserId) {
        Set<Long> likedPostIds = Set.of();
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(Post::getId).toList();
            likedPostIds = postLikeRepository.findLikedPostIds(currentUserId, postIds);
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        List<PostResponse> responses = posts.stream()
                .map(p -> postMapper.toPostResponse(p, finalLikedPostIds.contains(p.getId())))
                .toList();

        return CursorResponse.of(responses, limit, p -> CursorUtils.encode(p.createdAt(), p.id()));
    }

    private String requireMediaKind(String verifiedKey, MediaKind... allowed) {
        MediaKind actual = FileKey.mediaKindOf(verifiedKey);
        if (actual == null || !List.of(allowed).contains(actual)) {
            throw new AppException(AppError.INVALID_FILE_TYPE);
        }
        return FileKey.toUploads(verifiedKey);
    }

    private void requireOwned(String key, Long userId) {
        if (!FileKey.isOwnedBy(key, userId)) {
            throw new AppException(AppError.FILE_ACCESS_DENIED);
        }
    }
}
