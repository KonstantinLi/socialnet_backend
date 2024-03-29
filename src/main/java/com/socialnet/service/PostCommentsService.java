package com.socialnet.service;

import com.socialnet.dto.request.CommentRq;
import com.socialnet.dto.response.CommentRs;
import com.socialnet.dto.response.CommonRs;
import com.socialnet.dto.response.PersonRs;
import com.socialnet.entity.enums.FriendShipStatus;
import com.socialnet.entity.enums.LikeType;
import com.socialnet.entity.locationrelated.Weather;
import com.socialnet.entity.personrelated.FriendShip;
import com.socialnet.entity.postrelated.Post;
import com.socialnet.entity.postrelated.PostComment;
import com.socialnet.exception.PostCommentCreateException;
import com.socialnet.exception.PostCommentNotFoundException;
import com.socialnet.exception.PostNotFoundException;
import com.socialnet.mapper.CommentMapper;
import com.socialnet.mapper.WeatherMapper;
import com.socialnet.repository.*;
import com.socialnet.security.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostCommentsService {
    private final PostCommentsRepository postCommentsRepository;
    private final PostsRepository postsRepository;
    private final LikesRepository likesRepository;
    private final FriendShipRepository friendShipRepository;
    private final WeatherRepository weatherRepository;
    private final PersonRepository personRepository;

    private final JwtTokenUtils jwtTokenUtils;
    private final CommentMapper commentMapper;
    private final WeatherMapper weatherMapper;

    @Transactional
    public CommonRs<CommentRs> createComment(String authorization, Long postId, CommentRq commentRq) {
        Long myId = jwtTokenUtils.getId(authorization);

        if (commentRq.getCommentText() == null || commentRq.getCommentText().isBlank()) {
            throw new PostCommentCreateException("Текст комментария отсутствует");
        }

        Post post = postsRepository.findById(postId).orElseThrow(() ->
                new PostNotFoundException("Ошибка при добавлении комментария к посту с id " + postId +
                        ": пост не найден"));

        //TODO check if fetch is necessary
        fetchPost(postId, false);

        PostComment postComment = new PostComment();

        postComment.setPost(post);
        postComment.setAuthor(personRepository.findById(myId).orElseThrow());
        postComment.setTime(LocalDateTime.now());
        postComment.setIsBlocked(false);
        postComment.setIsDeleted(false);

        return updatePostComment(postComment, commentRq, myId);
    }

    @Transactional
    public CommonRs<CommentRs> editComment(String authorization, Long id, Long commentId, CommentRq commentRq) {
        Long myId = jwtTokenUtils.getId(authorization);

        return updatePostComment(
                fetchPostComment(
                        commentId, id,
                        commentRq.getIsDeleted() != null && !commentRq.getIsDeleted()
                ),
                commentRq, myId
        );
    }

    @Transactional
    public CommonRs<CommentRs> deleteComment(String authorization, Long id, Long commentId) {
        CommentRq commentRq = new CommentRq();
        commentRq.setIsDeleted(true);

        return editComment(authorization, id, commentId, commentRq);
    }

    @Transactional
    public CommonRs<CommentRs> recoverComment(String authorization, Long id, Long commentId) {
        CommentRq commentRq = new CommentRq();
        commentRq.setIsDeleted(false);

        return editComment(authorization, id, commentId, commentRq);
    }

    @Transactional
    public CommonRs<List<CommentRs>> getComments(String authorization, Long postId, Integer offset, Integer perPage) {
        Long myId = jwtTokenUtils.getId(authorization);
        Post post = fetchPost(postId, false);

        List<PostComment> postComments = postCommentsRepository
                .findAllRootCommentsByPostId(
                post.getId(),
                myId,
                PageRequest.of(
                        offset, perPage,
                        Sort.by("time").descending()
                )
        );

        long total = postCommentsRepository.countRootCommentsByPostId(
                post.getId(), myId
        );

        return getListPostCommentResponse(postComments, total, myId, offset, postComments.size());
    }


    protected PostComment fetchPostComment(Long id, Long postId, Boolean isDeleted) {
        Optional<PostComment> optionalPostComment = postCommentsRepository.findByIdAndPostIdAndIsDeleted(
                id, postId, isDeleted
        );

        if (optionalPostComment.isEmpty()) {
            throw new PostCommentNotFoundException(id);
        }

        return optionalPostComment.get();
    }

    private Post fetchPost(Long id, Boolean isDeleted) {
        Optional<Post> optionalPost;

        if (isDeleted == null) {
            optionalPost = postsRepository.findById(id);
        } else {
            optionalPost = postsRepository.findByIdAndIsDeleted(id, isDeleted);
        }

        if (optionalPost.isEmpty()) {
            throw new PostNotFoundException(id);
        }

        return optionalPost.get();
    }

    private CommonRs<CommentRs> updatePostComment(PostComment postComment, CommentRq commentRq, Long myId) {
        savePostComment(postComment, commentRq);

        return getPostCommentResponse(postComment, myId);
    }

    private void savePostComment(PostComment postComment, CommentRq commentRq) {
        if (commentRq.getParentId() != null) {
            PostComment parentPostComment = fetchPostComment(
                    commentRq.getParentId(), postComment.getPost().getId(), false
            );

            if (parentPostComment.getParentId() != null) {
                throw new PostCommentCreateException("Sub-comment of sub-comment is not allowed");
            }
        }

        commentMapper.commentRqToPostComment(commentRq, postComment);
        postCommentsRepository.save(postComment);
    }

    private CommonRs<List<CommentRs>> getListPostCommentResponse(
            List<PostComment> postComments, Long total, Long myId, Integer offset, Integer perPage
    ) {
        CommonRs<List<CommentRs>> commonRsListCommentRs = new CommonRs<>();
        commonRsListCommentRs.setOffset(offset);
        commonRsListCommentRs.setItemPerPage(perPage);
        commonRsListCommentRs.setPerPage(perPage);
        commonRsListCommentRs.setTotal(total);

        List<CommentRs> commentRsList = new ArrayList<>();

        for (PostComment postComment : postComments) {
            commentRsList.add(postCommentToCommentRs(postComment, myId));
        }

        commonRsListCommentRs.setData(commentRsList);

        return commonRsListCommentRs;
    }

    private CommonRs<CommentRs> getPostCommentResponse(PostComment postComment, Long myId) {
        CommonRs<CommentRs> commonRsCommentRs = new CommonRs<>();

        commonRsCommentRs.setData(postCommentToCommentRs(postComment, myId));

        return commonRsCommentRs;
    }

    private CommentRs postCommentToCommentRs(PostComment postComment, Long myId) {
        CommentRs commentRs = commentMapper.postCommentToCommentRs(postComment);

        commentRs.setLikes(likesRepository.countByTypeAndEntityId(LikeType.Comment, commentRs.getId()));
        commentRs.setMyLike(likesRepository.existsByTypeAndEntityIdAndPersonId(LikeType.Comment, commentRs.getId(), myId));

        fillAuthor(commentRs.getAuthor(), myId);

        commentRs.getSubComments().forEach(subCommentRs -> {
            subCommentRs.setLikes(likesRepository.countByTypeAndEntityId(LikeType.Comment, subCommentRs.getId()));
            subCommentRs.setMyLike(likesRepository.existsByTypeAndEntityIdAndPersonId(LikeType.Comment, subCommentRs.getId(), myId));

            fillAuthor(subCommentRs.getAuthor(), myId);
        });

        return commentRs;
    }

    private FriendShipStatus getFriendshipStatus(Long personId, Long destinationPersonId) {
        Optional<FriendShip> optionalFriendShip = friendShipRepository
                .findBySourcePerson_IdAndDestinationPerson_Id(personId, destinationPersonId);

        if (optionalFriendShip.isEmpty()) {
            return FriendShipStatus.UNKNOWN;
        }

        return optionalFriendShip.get().getStatus();
    }

    private PersonRs fillAuthor(PersonRs personRs, Long myId) {
        FriendShipStatus friendshipStatus = getFriendshipStatus(personRs.getId(), myId);
        personRs.setFriendStatus(friendshipStatus.toString());
        personRs.setIsBlockedByCurrentUser(friendshipStatus == FriendShipStatus.BLOCKED);

        Optional<Weather> optionalWeather = weatherRepository.findByCity(personRs.getCity());

        if (optionalWeather.isPresent()) {
            personRs.setWeather(weatherMapper.weatherToWeatherRs(optionalWeather.get()));
        }

        return personRs;
    }
}
