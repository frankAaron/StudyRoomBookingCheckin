package com.tyut.studyroombookingcheckin.vo;

import lombok.Data;

@Data
public class RankingVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer totalMinutes;
    private Integer rank;
}