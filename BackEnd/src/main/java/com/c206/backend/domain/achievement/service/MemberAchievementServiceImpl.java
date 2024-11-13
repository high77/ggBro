package com.c206.backend.domain.achievement.service;


import com.c206.backend.domain.achievement.dto.response.MemberAchievementListResponseDto;
import com.c206.backend.domain.achievement.dto.response.NewGoalResponseDto;
import com.c206.backend.domain.achievement.entity.MemberAchievement;
import com.c206.backend.domain.achievement.entity.enums.Reward;
import com.c206.backend.domain.achievement.exception.MemberAchievementError;
import com.c206.backend.domain.achievement.exception.MemberAchievementException;
import com.c206.backend.domain.achievement.repository.MemberAchievementRepository;
import com.c206.backend.domain.member.entity.Member;
import com.c206.backend.domain.member.entity.MemberInfo;
import com.c206.backend.domain.member.exception.member.MemberError;
import com.c206.backend.domain.member.exception.member.MemberException;
import com.c206.backend.domain.member.repository.MemberInfoRepository;
import com.c206.backend.domain.member.repository.MemberRepository;
import com.c206.backend.domain.pet.entity.MemberPet;
import com.c206.backend.domain.pet.entity.Pet;
import com.c206.backend.domain.pet.entity.enums.PetType;
import com.c206.backend.domain.pet.repository.MemberPetRepository;
import com.c206.backend.domain.pet.repository.PetRepository;
import com.c206.backend.domain.pet.service.MemberPetServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberAchievementServiceImpl implements MemberAchievementService {

    private final MemberAchievementRepository memberAchievementRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberRepository memberRepository;
    private final MemberPetRepository memberPetRepository;
    private final PetRepository petRepository;
    private final MemberPetServiceImpl memberPetServiceImpl;

    @Override
    public void  updateMemberAchievement(Long memberId, Long achievementId,int progress) {
        memberAchievementRepository.findByMemberIdAndAchievementId(memberId,achievementId).updateProgress(progress);
    }

    @Override
    public List<MemberAchievementListResponseDto> getMemberAchievementList(Long memberId) {
        List<MemberAchievement> memberAchievements = memberAchievementRepository.findByMemberId(memberId);
        List<MemberAchievementListResponseDto> memberAchievementListResponseDtoList = new ArrayList<>();
        for (MemberAchievement memberAchievement : memberAchievements) {
            MemberAchievementListResponseDto memberAchievementListResponseDto = new MemberAchievementListResponseDto(
                    memberAchievement.getId(),
                    memberAchievement.getAchievement().getId(),
                    memberAchievement.getGoalMultiply() * memberAchievement.getAchievement().getGoalCoefficient(),
                    memberAchievement.getProgress()
            );
            memberAchievementListResponseDtoList.add(memberAchievementListResponseDto);
        }
        return memberAchievementListResponseDtoList;
    }

    @Override
    public NewGoalResponseDto getReward(Long memberId, Long memberAchievementId) {
        MemberAchievement memberAchievement = memberAchievementRepository.findById(memberAchievementId).orElseThrow(()
                -> new MemberAchievementException(MemberAchievementError.NOT_FOUND_MEMBER_ACHIEVEMENT));
        // 요청한 회원과 업적의 회원이 일치한지 확인
        if (memberId != memberAchievement.getMember().getId()) {
            throw new MemberAchievementException(MemberAchievementError.NOT_MATCH_ACHIEVEMENT);
        }
        // 회원의 목표가 진행상황을 초과하면 업적 미완료
        if (memberAchievement.getGoalMultiply() * memberAchievement.getAchievement().getGoalCoefficient() > memberAchievement.getProgress()) {
            throw new MemberAchievementException(MemberAchievementError.ACHIEVEMENT_NOT_COMPLETED);
        }
        // 배수 올리기
        memberAchievement.updateGoalMultiply();
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new MemberException(MemberError.NOT_FOUND_MEMBER));
        // 보상이 1. 재화인 경우 ,
        if (memberAchievement.getAchievement().getReward() == Reward.CURRENCY) {
            MemberInfo memberInfo = memberInfoRepository.findTopByMemberIdOrderByIdDesc(memberId);
            MemberInfo newMemberInfo = MemberInfo.builder()
                    .member(member)
                    .profilePetId(memberInfo.getProfilePetId())
                    .exp(memberInfo.getExp())
                    .currency(memberInfo.getCurrency() + 5000)
                    .build();
            memberInfoRepository.save(newMemberInfo);
        // 보상이 2. 펫인 경우
        } else if (memberAchievement.getAchievement().getReward() == Reward.PET) {
            List<MemberPet> memberPets = memberPetRepository.findByMemberId(memberId);
            List<Pet> pets = petRepository.findByPetTypeIs(PetType.ACHIEVEMENT);
            memberPetServiceImpl.createMemberPet(memberPets, pets, member);
        }
        return new NewGoalResponseDto(
                memberAchievement.getGoalMultiply() * memberAchievement.getAchievement().getGoalCoefficient()
        );
    }
}
