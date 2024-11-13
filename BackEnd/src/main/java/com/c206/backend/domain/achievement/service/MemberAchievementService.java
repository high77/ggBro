package com.c206.backend.domain.achievement.service;

import com.c206.backend.domain.achievement.dto.response.MemberAchievementListResponseDto;
import com.c206.backend.domain.achievement.dto.response.NewGoalResponseDto;

import java.util.List;

public interface MemberAchievementService {

    // 회원의 업적 리스트 조회
    List<MemberAchievementListResponseDto> getMemberAchievementList(Long memberId);

    // 업적보상 받기
    NewGoalResponseDto getReward(Long memberId, Long memberAchievementId);

    // 업적 진행상황 업데이트
    void updateMemberAchievement(Long memberId, Long achievementId,int progress);
}
