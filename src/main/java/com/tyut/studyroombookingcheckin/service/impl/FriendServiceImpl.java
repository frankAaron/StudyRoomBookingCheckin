package com.tyut.studyroombookingcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyut.studyroombookingcheckin.common.BusinessException;
import com.tyut.studyroombookingcheckin.common.ErrorCode;
import com.tyut.studyroombookingcheckin.entity.Friend;
import com.tyut.studyroombookingcheckin.entity.StudyCheckin;
import com.tyut.studyroombookingcheckin.entity.User;
import com.tyut.studyroombookingcheckin.mapper.FriendMapper;
import com.tyut.studyroombookingcheckin.mapper.StudyCheckinMapper;
import com.tyut.studyroombookingcheckin.mapper.UserMapper;
import com.tyut.studyroombookingcheckin.service.FriendService;
import com.tyut.studyroombookingcheckin.vo.RankingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final UserMapper userMapper;
    private final StudyCheckinMapper checkinMapper;

    @Override
    public void sendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new BusinessException("不能添加自己为好友");
        }
        User friend = userMapper.selectById(friendId);
        if (friend == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        Long alreadyFriend = friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId).eq(Friend::getStatus, 1)
                .or()
                .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId).eq(Friend::getStatus, 1));
        if (alreadyFriend > 0) {
            throw new BusinessException(ErrorCode.ALREADY_FRIEND);
        }
        Long pending = friendMapper.selectCount(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId).eq(Friend::getStatus, 0));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXIST);
        }
        Friend f = new Friend();
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setStatus(0);
        f.setCreateTime(LocalDateTime.now());
        f.setUpdateTime(LocalDateTime.now());
        try {
            friendMapper.insert(f);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXIST);
        }
    }

    @Override
    @Transactional
    public void acceptRequest(Long userId, Long friendId) {
        Friend request = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0));
        if (request == null) {
            throw new BusinessException("好友请求不存在");
        }
        request.setStatus(1);
        request.setUpdateTime(LocalDateTime.now());
        friendMapper.updateById(request);

        Friend reverse = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId));
        if (reverse == null) {
            reverse = new Friend();
            reverse.setUserId(userId);
            reverse.setFriendId(friendId);
            reverse.setStatus(1);
            reverse.setCreateTime(LocalDateTime.now());
            reverse.setUpdateTime(LocalDateTime.now());
            friendMapper.insert(reverse);
        } else if (reverse.getStatus() == 0) {
            reverse.setStatus(1);
            reverse.setUpdateTime(LocalDateTime.now());
            friendMapper.updateById(reverse);
        }
    }

    @Override
    public void rejectRequest(Long userId, Long friendId) {
        Friend request = friendMapper.selectOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0));
        if (request == null) {
            throw new BusinessException("好友请求不存在");
        }
        friendMapper.deleteById(request.getId());
    }

    @Override
    @Transactional
    public void unfriend(Long userId, Long friendId) {
        friendMapper.delete(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendId)
                .or()
                .eq(Friend::getUserId, friendId).eq(Friend::getFriendId, userId));
    }

    @Override
    public List<RankingVO> listFriends(Long userId) {
        List<Friend> friends = friendMapper.selectList(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1));
        List<Long> friendIds = friends.stream()
                .map(Friend::getFriendId)
                .collect(Collectors.toList());
        friendIds.add(userId);

        List<RankingVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (Long fid : friendIds) {
            User u = userMapper.selectById(fid);
            if (u == null) continue;
            Integer minutes = checkinMapper.sumDailyMinutes(fid, today);
            RankingVO vo = new RankingVO();
            vo.setUserId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setTotalMinutes(minutes != null ? minutes : 0);
            result.add(vo);
        }
        result.sort(Comparator.comparing(RankingVO::getTotalMinutes).reversed());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        return result;
    }

    @Override
    public Page<RankingVO> pageRequests(Long userId, long current, long size) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getFriendId, userId);
        wrapper.eq(Friend::getStatus, 0);
        wrapper.orderByDesc(Friend::getCreateTime);
        Page<Friend> page = friendMapper.selectPage(new Page<>(current, size), wrapper);

        Page<RankingVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RankingVO> records = new ArrayList<>();
        for (Friend f : page.getRecords()) {
            User u = userMapper.selectById(f.getUserId());
            if (u == null) continue;
            RankingVO vo = new RankingVO();
            vo.setUserId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            records.add(vo);
        }
        voPage.setRecords(records);
        return voPage;
    }
}