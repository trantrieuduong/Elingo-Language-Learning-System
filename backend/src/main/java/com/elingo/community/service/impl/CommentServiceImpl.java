package com.elingo.community.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.MediaKind;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.request.CreateCommentRequest;
import com.elingo.community.dto.request.UpdateCommentRequest;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.entity.Comment;
import com.elingo.community.entity.CommentLike;
import com.elingo.community.entity.CommentLikeId;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.mapper.PostMapper;
import com.elingo.community.repository.CommentLikeRepository;
import com.elingo.community.repository.CommentRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.CommentService;
import com.elingo.file.util.FileKey;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "COMMENT-SERVICE")
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostMapper postMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request, Long currentUserId) {
        Post post = postRepository.findById(request.postId())
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(AppError.POST_NOT_PUBLISHED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findById(request.parentCommentId())
                    .orElseThrow(() -> new AppException(AppError.PARENT_COMMENT_NOT_FOUND));

            if (!parent.getPost().getId().equals(post.getId())) {
                throw new AppException(AppError.PARENT_COMMENT_NOT_FOUND);
            }

            if (parent.getParentComment() != null) {
                throw new AppException(AppError.COMMENT_NESTING_LEVEL_EXCEEDED);
            }
        }

        String uploadsKey = null;
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            requireOwned(request.imageUrl(), currentUserId);
            uploadsKey = requireMediaKind(request.imageUrl(), MediaKind.IMAGE);
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parentComment(parent)
                .content(request.content())
                .imageUrl(uploadsKey)
                .likeCount(0)
                .build();

        Comment savedComment = commentRepository.save(comment);
        postRepository.updateCommentCount(post.getId(), 1);

        if (uploadsKey != null) {
            publisher.publishEvent(new FileAttachedEvent(request.imageUrl(), uploadsKey));
        }

        log.info("Comment created commentId={} postId={} parentCommentId={} hasImage={}",
                savedComment.getId(), post.getId(), parent != null ? parent.getId() : null, uploadsKey != null);
        return postMapper.toCommentResponse(savedComment, 0, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentsByPost(Long postId, Pageable pageable, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(AppError.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            boolean canView = (currentUserId != null && currentUserId.equals(post.getUser().getId()));
            if (!canView) {
                throw new AppException(AppError.POST_NOT_FOUND);
            }
        }

        Page<Comment> commentPage = commentRepository.findByPostIdAndParentCommentIsNull(postId, pageable);
        if (commentPage.isEmpty()) {
            return PageResponse.of(commentPage.map(c -> postMapper.toCommentResponse(c, 0, false)));
        }

        List<Long> commentIds = commentPage.getContent().stream().map(Comment::getId).toList();

        List<Object[]> replyCounts = commentRepository.countRepliesByParentCommentIds(commentIds);
        Map<Long, Integer> replyCountMap = replyCounts.stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> ((Long) r[1]).intValue()));

        Set<Long> likedCommentIds = (currentUserId != null)
                ? commentLikeRepository.findLikedCommentIds(currentUserId, commentIds)
                : Set.of();

        Page<CommentResponse> responsePage = commentPage.map(c -> postMapper.toCommentResponse(
                c,
                replyCountMap.getOrDefault(c.getId(), 0),
                likedCommentIds.contains(c.getId())
        ));

        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getRepliesByComment(Long parentCommentId, Pageable pageable, Long currentUserId) {
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new AppException(AppError.PARENT_COMMENT_NOT_FOUND));

        Post post = parent.getPost();
        if (post.getStatus() != PostStatus.PUBLISHED) {
            boolean canView = (currentUserId != null && currentUserId.equals(post.getUser().getId()));
            if (!canView) {
                throw new AppException(AppError.PARENT_COMMENT_NOT_FOUND);
            }
        }

        Page<Comment> repliesPage = commentRepository.findByParentCommentId(parentCommentId, pageable);
        if (repliesPage.isEmpty()) {
            return PageResponse.of(repliesPage.map(c -> postMapper.toCommentResponse(c, 0, false)));
        }

        List<Long> replyIds = repliesPage.getContent().stream().map(Comment::getId).toList();
        Set<Long> likedCommentIds = (currentUserId != null)
                ? commentLikeRepository.findLikedCommentIds(currentUserId, replyIds)
                : Set.of();

        Page<CommentResponse> responsePage = repliesPage.map(c -> postMapper.toCommentResponse(
                c,
                0,
                likedCommentIds.contains(c.getId())
        ));

        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new AppException(AppError.COMMENT_ACCESS_DENIED);
        }

        if (comment.getPost().getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(AppError.POST_NOT_PUBLISHED);
        }

        comment.setContent(request.content());

        String oldKey = comment.getImageUrl();
        String newKey = request.imageUrl();

        if (newKey != null && !newKey.isBlank()) {
            if (FileKey.isInZone(newKey, FileKey.VERIFIED_PREFIX)) {
                requireOwned(newKey, currentUserId);
                String uploadsKey = requireMediaKind(newKey, MediaKind.IMAGE);
                comment.setImageUrl(uploadsKey);
                publisher.publishEvent(new FileAttachedEvent(newKey, uploadsKey));
                if (oldKey != null && !oldKey.isBlank()) {
                    publisher.publishEvent(new FileDeletedEvent(List.of(oldKey)));
                }
            } else if (FileKey.isInZone(newKey, FileKey.UPLOADS_PREFIX)) {
                if (!newKey.equals(oldKey)) {
                    throw new AppException(AppError.INVALID_REQUEST);
                }
            } else {
                throw new AppException(AppError.INVALID_REQUEST);
            }
        } else {
            // Người dùng gỡ bỏ ảnh đính kèm của bình luận
            if (oldKey != null && !oldKey.isBlank()) {
                comment.setImageUrl(null);
                publisher.publishEvent(new FileDeletedEvent(List.of(oldKey)));
            }
        }

        int replyCount = (int) commentRepository.countByParentCommentId(commentId);
        boolean likedByMe = commentLikeRepository.existsByIdCommentIdAndIdUserId(commentId, currentUserId);

        log.info("Comment updated commentId={} hasImage={}", commentId, comment.getImageUrl() != null);
        return postMapper.toCommentResponse(comment, replyCount, likedByMe);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        boolean isCommentAuthor = comment.getUser().getId().equals(currentUserId);
        boolean isPostAuthor = comment.getPost().getUser().getId().equals(currentUserId);

        if (!isCommentAuthor && !isPostAuthor) {
            throw new AppException(AppError.COMMENT_ACCESS_DENIED);
        }

        long countToDelete = commentRepository.countByCommentOrReplies(commentId);
        List<String> imagesToDelete = commentRepository.findImageUrlsByCommentOrReplies(commentId);

        commentRepository.delete(comment);
        postRepository.updateCommentCount(comment.getPost().getId(), (int) -countToDelete);

        if (!imagesToDelete.isEmpty()) {
            publisher.publishEvent(new FileDeletedEvent(imagesToDelete));
        }

        log.info("Comment deleted commentId={} postId={} deletedCount={}", commentId, comment.getPost().getId(), countToDelete);
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        if (comment.getPost().getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(AppError.POST_NOT_PUBLISHED);
        }

        if (commentLikeRepository.existsByIdCommentIdAndIdUserId(commentId, currentUserId)) {
            throw new AppException(AppError.COMMENT_ALREADY_LIKED);
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        CommentLike like = CommentLike.builder()
                .id(new CommentLikeId(commentId, currentUserId))
                .comment(comment)
                .user(user)
                .build();

        commentLikeRepository.save(like);
        commentRepository.updateLikeCount(commentId, 1);
        log.info("Comment liked commentId={}", commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(AppError.COMMENT_NOT_FOUND));

        if (comment.getPost().getStatus() != PostStatus.PUBLISHED) {
            throw new AppException(AppError.POST_NOT_PUBLISHED);
        }

        if (!commentLikeRepository.existsByIdCommentIdAndIdUserId(commentId, currentUserId)) {
            throw new AppException(AppError.COMMENT_NOT_LIKED);
        }

        commentLikeRepository.deleteByIdCommentIdAndIdUserId(commentId, currentUserId);
        commentRepository.updateLikeCount(commentId, -1);
        log.info("Comment unliked commentId={}", commentId);
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
