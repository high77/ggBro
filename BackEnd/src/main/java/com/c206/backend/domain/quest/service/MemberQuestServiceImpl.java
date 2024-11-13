package com.c206.backend.domain.quest.service;

import com.c206.backend.domain.member.entity.Member;
import com.c206.backend.domain.member.entity.MemberInfo;
import com.c206.backend.domain.member.exception.member.MemberError;
import com.c206.backend.domain.member.exception.member.MemberException;
import com.c206.backend.domain.member.repository.MemberInfoRepository;
import com.c206.backend.domain.member.repository.MemberRepository;
import com.c206.backend.domain.pet.entity.MemberPet;
import com.c206.backend.domain.pet.repository.MemberPetRepository;
import com.c206.backend.domain.quest.dto.response.MemberQuestListResponseDto;
import com.c206.backend.domain.quest.entity.MemberQuest;
import com.c206.backend.domain.quest.entity.Quest;
import com.c206.backend.domain.quest.exception.MemberQuestError;
import com.c206.backend.domain.quest.exception.MemberQuestException;
import com.c206.backend.domain.quest.repository.MemberQuestRepository;
import com.c206.backend.domain.quest.repository.QuestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberQuestServiceImpl implements MemberQuestService {

    private final MemberRepository memberRepository;
    private final QuestRepository questRepository;
    private final MemberQuestRepository memberQuestRepository;
    private final MemberPetRepository memberPetRepository;
    private final MemberInfoRepository memberInfoRepository;

    @Override
    public void  updateMemberQuest(Long memberId,Long memberPetId, Long questId) {
        MemberQuest memberQuest=memberQuestRepository.findTopByMemberIdAndMemberPetIdAndQuestIdOrderByIdDesc(memberId,memberPetId,questId);
        System.out.println(memberQuest);
        if (memberQuest!=null) {
            memberQuest.updateProgress();
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MemberQuestListResponseDto> getQuestList(Long memberId) {
//        List<MemberQuest> findedQuestList = memberQuestRepository.findByMemberId(memberId);
        List<MemberQuest> findedQuestList = memberQuestRepository.findTop3ByMemberIdOrderByCreatedAtDesc(memberId);
        List<MemberQuestListResponseDto> resList = new ArrayList<>();

        for(MemberQuest findedQuest : findedQuestList){
            MemberQuestListResponseDto resItem = new MemberQuestListResponseDto(
                    findedQuest.getMemberPet().getNickname(),
                    findedQuest.getId(),
                    findedQuest.getQuest().getId(),
                    findedQuest.getGoal(),
                    findedQuest.getProgress(),
                    findedQuest.isDone()
            );
            resList.add(resItem);
        }
        return resList;
    }

    @Override
    public Integer getQuestReward(Long memberId, Long memberQuestId) {

        // 퀘스트 없을때
        MemberQuest findedMemberQuest = memberQuestRepository.findById(memberQuestId).orElseThrow(()
        -> new MemberQuestException(MemberQuestError.NOT_FOUND_MEMBER_QUEST));

        // 호출한 퀘스트가 사용자의 것이 아닐때
        if(!Objects.equals(memberId, findedMemberQuest.getMember().getId())){
            throw new MemberQuestException(MemberQuestError.NOT_MATCH_QUEST);
        }

        // 퀘스트 진행상황이 목표에 도달하지 못했을 때
        if(findedMemberQuest.getGoal() > findedMemberQuest.getProgress()){
            throw new MemberQuestException((MemberQuestError.QUEST_NOT_COMPLETED));
        }

        // isdone = true로 설정해서 새로 MemberQuest에 넣어주기
        findedMemberQuest.updateIsDone();
        // 퀘스트의 횟수와 가중치에 맞게 보상 설정
        int reward = 10 * findedMemberQuest.getGoal();
        if(findedMemberQuest.getQuest().getId() == 1){
            reward *= 24;
        }else if(findedMemberQuest.getQuest().getId() == 2){
            reward *= 4;
        }else if(findedMemberQuest.getQuest().getId() == 3){
            reward *= 3;
        }else if(findedMemberQuest.getQuest().getId() == 4){
            reward *= 12;
        }else if(findedMemberQuest.getQuest().getId() == 5){
            reward *= 8;
        }

        //유저정보에 보상 저장
        MemberInfo memberInfo = memberInfoRepository.findTopByMemberIdOrderByIdDesc(memberId);
        MemberInfo newMemberInfo = MemberInfo.builder()
                .profilePetId(memberInfo.getProfilePetId())
                .exp(memberInfo.getExp())
                .member(memberInfo.getMember())
                .currency(memberInfo.getCurrency() + reward)
                .build();
        memberInfoRepository.save(newMemberInfo);

        return reward;
    }

    @Override

    public void addQuestList(Long memberId) {
        //사용자아이디
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new MemberException(MemberError.NOT_FOUND_MEMBER));

        // 리스트 중 랜덤하게, 혹은 사용자 맞춤으로 3개 뽑을것
        Random random = new Random();
        List<Integer> questIdList = new ArrayList<>();

        while(true){
            //1~5 랜덤수
            int questId = random.nextInt(1, 5);
            // 만약 리스트에 존재하면 통과
            if(questIdList.contains(questId)){
                continue;
            }
            //아니라면 리스트에 추가
            questIdList.add(questId);
            //3개 뽑으면 종료
            if(questIdList.size() == 3) break;
        }

        List<Quest> questInfoList = new ArrayList<>();
        for (Integer i : questIdList) {
            questInfoList.add(
                    questRepository.findById(
                            Long.valueOf(i)
                    ).get()
            );
        }

        // 펫 중 랜덤하게, 혹은 사용자 맞춤으로 1개 뽑을것
        List<MemberPet> petList = memberPetRepository.findByMemberId(memberId);
        //랜덤으로
        MemberPet selectedPet = petList.get(random.nextInt(0,petList.size()));

        for(int i=0; i<3; i++){
            MemberQuest memberQuest = MemberQuest.builder()
                    .member(member)
                    .memberPet(selectedPet)
                    .quest(questInfoList.get(i))
                    .goal(random.nextInt(questInfoList.get(i).getGoalMin(),questInfoList.get(i).getGoalMax())) // 사용자 맞춤으로 계수를 곱하고, 그렇게 나온 Max, Min의 랜덤값으로 지정
                    .progress(0)
                    .isDone(false)
                    .build();
            memberQuestRepository.save(memberQuest);
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * MON")
//    @Scheduled(cron = "0 17 10 8 5 ?")
    public void addQuestListSchedule() {
        List<Member> memberList = memberRepository.findAll();
        for(Member memberItem : memberList){
            addQuestList(memberItem.getId());
        }
    }

}
