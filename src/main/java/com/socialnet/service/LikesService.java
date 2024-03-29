package com.socialnet.service;

import com.socialnet.dto.request.LikeRq;
import com.socialnet.dto.response.CommonRs;
import com.socialnet.dto.response.LikeRs;
import com.socialnet.entity.enums.LikeType;
import com.socialnet.entity.postrelated.Like;
import com.socialnet.entity.postrelated.Post;
import com.socialnet.entity.postrelated.PostComment;
import com.socialnet.exception.*;
import com.socialnet.repository.LikesRepository;
import com.socialnet.repository.PostCommentsRepository;
import com.socialnet.repository.PostsRepository;
import com.socialnet.security.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikesService {
    private final LikesRepository likesRepository;
    private final PostsRepository postsRepository;
    private final PostCommentsRepository postCommentsRepository;

    private final JwtTokenUtils jwtTokenUtils;

    @Transactional
    public CommonRs<LikeRs> getLikes(String authorization, Long itemId, LikeType type) {
        Like like = new Like();
        like.setType(type);
        like.setEntityId(itemId);

        return getLikeResponse(like);
    }

    @Transactional
    public CommonRs<LikeRs> putLike(String authorization, LikeRq likeRq) {
        Long myId = jwtTokenUtils.getId(authorization);

        if (likeRq.getType() == null || likeRq.getItemId() == null) {
            throw new LikeTargetNotProvidedException();
        }

        Optional<Like> optionalLike = likesRepository.findByPersonIdAndTypeAndEntityId(
                myId, likeRq.getType(), likeRq.getItemId()
        );

        if (optionalLike.isPresent()) {
            throw new LikeAlreadyExistsException(myId, likeRq.getType(), likeRq.getItemId());
        }

        switch (likeRq.getType()) {
            case Post -> {
                Optional<Post> optionalPost = postsRepository.findById(likeRq.getItemId());

                if (optionalPost.isEmpty()) {
                    throw new PostNotFoundException(likeRq.getItemId());
                }
            }
            case Comment -> {
                Optional<PostComment> optionalPostComment = postCommentsRepository.findById(likeRq.getItemId());

                if (optionalPostComment.isEmpty()) {
                    throw new PostCommentNotFoundException(likeRq.getItemId());
                }
            }
        }

        Like like = new Like();
        like.setPersonId(myId);
        like.setType(likeRq.getType());
        like.setEntityId(likeRq.getItemId());

        likesRepository.save(like);

        return getLikeResponse(like);
    }

    @Transactional
    public CommonRs<LikeRs> deleteLike(String authorization, Long itemId, LikeType type) {
        Long myId = jwtTokenUtils.getId(authorization);

        Optional<Like> optionalLike = likesRepository.findByPersonIdAndTypeAndEntityId(
                myId, type, itemId
        );

        if (optionalLike.isEmpty()) {
            throw new LikeNotFoundException(myId, type, itemId);
        }

        likesRepository.deleteByPersonIdAndTypeAndEntityId(
                myId, type, itemId
        );

        return getLikeResponse(optionalLike.get());
    }


    private CommonRs<LikeRs> getLikeResponse(Like like) {
        CommonRs<LikeRs> commonRsLikeRs = new CommonRs<>();
        LikeRs likeRs = new LikeRs();

        likeRs.setUsers(likesRepository.findAllByTypeAndEntityId(like.getType(), like.getEntityId())
                .stream()
                .map(Like::getPersonId)
                .collect(Collectors.toSet()));

        likeRs.setLikes(likeRs.getUsers().size());
        commonRsLikeRs.setData(likeRs);

        return commonRsLikeRs;
    }
}
