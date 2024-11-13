package com.c206.backend.domain.quest.service;

import com.c206.backend.domain.quest.dto.response.MemberQuestListResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MemberQuestService {

    List<MemberQuestListResponseDto> getQuestList(Long memberId);

    Integer getQuestReward(Long memberId, Long memberQuestId);

    void addQuestList(Long memberId);

    void addQuestListSchedule();

    void  updateMemberQuest(Long memberId,Long memberPetId, Long questId);

}
