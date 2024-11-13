package com.c206.backend.domain.plogging.service.Impl;


import com.c206.backend.domain.achievement.service.MemberAchievementService;
import com.c206.backend.domain.member.entity.Member;
import com.c206.backend.domain.member.exception.member.MemberError;
import com.c206.backend.domain.member.exception.member.MemberException;
import com.c206.backend.domain.member.repository.MemberRepository;
import com.c206.backend.domain.member.service.RedisService;
import com.c206.backend.domain.pet.dto.response.MemberPetListResponseDto;
import com.c206.backend.domain.pet.entity.MemberPet;
import com.c206.backend.domain.pet.exception.PetError;
import com.c206.backend.domain.pet.exception.PetException;
import com.c206.backend.domain.pet.repository.MemberPetRepository;
import com.c206.backend.domain.pet.service.MemberPetService;
import com.c206.backend.domain.plogging.dto.LocationInfo;
import com.c206.backend.domain.plogging.dto.request.FinishPloggingRequestDto;
import com.c206.backend.domain.plogging.dto.response.FinishPloggingResponseDto;
import com.c206.backend.domain.plogging.entity.Plogging;
import com.c206.backend.domain.plogging.entity.PloggingRoute;
import com.c206.backend.domain.plogging.entity.enums.TrashType;
import com.c206.backend.domain.plogging.exception.PloggingError;
import com.c206.backend.domain.plogging.exception.PloggingException;
import com.c206.backend.domain.plogging.repository.PloggingRepository;
import com.c206.backend.domain.plogging.repository.PloggingRouteRepository;
import com.c206.backend.domain.plogging.repository.TrashRepository;
import com.c206.backend.domain.plogging.service.PloggingService;
import com.c206.backend.domain.quest.service.MemberQuestService;
import com.c206.backend.domain.quest.service.MemberQuestServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PloggingServiceImpl implements PloggingService {

    private final MemberRepository memberRepository;
    private final MemberPetRepository memberPetRepository;
    private final PloggingRepository ploggingRepository;
    private final TrashRepository trashRepository;
    private final PloggingRouteRepository ploggingRouteRepository;
    private final MemberAchievementService memberAchievementService;
    private final MemberQuestService memberQuestService;
    private final RedisService redisService;
    private final MemberPetService memberPetService;

    @Override
    public Long createPlogging(Long memberPetId, Long memberId) {

        // memberPetId 가 -1 일 경우 redis에 저장된 가장 최근 선택된 펫 ID로 지정하는 로직
        if (memberPetId == -1) {
            try{
                memberPetId = Long.valueOf(redisService.getValues("latest pet id "+ memberId));
            } catch (Exception e){
                List<MemberPetListResponseDto> mPList = memberPetService.getMemberPetList(memberId);
                memberPetId = mPList.get(0).getMemberPetId();
//                throw new PetException(PetError.NOT_FOUND_PET_IN_REDIS);
            }
        }

        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new MemberException(MemberError.NOT_FOUND_MEMBER));
        MemberPet memberPet = memberPetRepository.findById(memberPetId).orElseThrow(()
                -> new PetException(PetError.NOT_FOUND_MEMBER_PET));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime updatedTime = now.plusHours(9);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        Plogging plogging = ploggingRepository.save(Plogging.builder()
                .member(member)
                .memberPet(memberPet)
                .time(updatedTime.format(formatter) + " ~ ")
                .distance(0)
                .build());

        // 가장 최근 선택한 펫 ID를 Redis에 저장하기
        redisService.setValues("latest pet id "+ memberId, String.valueOf(memberPetId), 14*24*60*60*1000L);

        return plogging.getId();
    }

    @Override
    public FinishPloggingResponseDto finishPlogging(Long ploggingId, FinishPloggingRequestDto finishPloggingRequestDto) {
        List<Object[]> results = trashRepository.countTrashByPloggingId(ploggingId);
        int normal=0,plastic = 0,can=0,glass=0;
        for (Object[] result : results) {
            int count = ((Number) result[1]).intValue();
            switch ((TrashType) result[0]) {
                case NORMAL -> normal = count;
                case PLASTIC -> plastic = count;
                case CAN -> can = count;
                case GLASS -> glass = count;
            }
        }
        Plogging plogging = ploggingRepository.findById(ploggingId).orElseThrow(()
                -> new PloggingException(PloggingError.NOT_FOUND_PLOGGING));
        plogging.updateDistance(finishPloggingRequestDto.getDistance());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime updatedTime = now.plusHours(9);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        plogging.updateTime(updatedTime.format(formatter));
//       플로깅 1회 업적, 퀘스트 진행상황 동기화
        memberAchievementService.updateMemberAchievement(plogging.getMember().getId(),1L,1);
        memberQuestService.updateMemberQuest(plogging.getMember().getId(),plogging.getMemberPet().getId(),1L);
//        플로깅한 거리
        memberAchievementService.updateMemberAchievement(plogging.getMember().getId(),
                2L,
                finishPloggingRequestDto.getDistance());

        for (LocationInfo path : finishPloggingRequestDto.getPath()) {
            ploggingRouteRepository.save(PloggingRoute.builder()
                    .plogging(plogging)
                    .latitude(path.getLatitude())
                    .longitude(path.getLongitude())
                    .build());
        }
        return new FinishPloggingResponseDto(
                normal,
                plastic,
                can,
                glass,
                plogging.getTime()
        );
    }
}
