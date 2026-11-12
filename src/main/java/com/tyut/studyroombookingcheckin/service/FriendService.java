package com.tyut.studyroombookingcheckin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.vo.RankingVO;

import java.util.List;

public interface FriendService {

    void sendRequest(Long userId, Long friendId);

    void acceptRequest(Long userId, Long friendId);

    void rejectRequest(Long userId, Long friendId);

    void unfriend(Long userId, Long friendId);

    List<RankingVO> listFriends(Long userId);

    Page<RankingVO> pageRequests(Long userId, long current, long size);
}