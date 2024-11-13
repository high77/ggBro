package com.c206.backend.domain.plogging.service.Impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.c206.backend.domain.achievement.repository.MemberAchievementRepository;
import com.c206.backend.domain.achievement.service.MemberAchievementService;
import com.c206.backend.domain.achievement.service.MemberAchievementServiceImpl;
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
import com.c206.backend.domain.pet.service.MemberPetService;
import com.c206.backend.domain.pet.service.MemberPetServiceImpl;
import com.c206.backend.domain.plogging.dto.LocationInfo;
import com.c206.backend.domain.plogging.dto.request.CreateTrashRequestDto;
import com.c206.backend.domain.plogging.dto.request.GetTrashRequestDto;
import com.c206.backend.domain.plogging.dto.response.CreateTrashResponseDto;
import com.c206.backend.domain.plogging.dto.response.GetTrashResponseDto;
import com.c206.backend.domain.plogging.entity.Plogging;
import com.c206.backend.domain.plogging.entity.Trash;
import com.c206.backend.domain.plogging.entity.enums.TrashType;
import com.c206.backend.domain.plogging.exception.PloggingError;
import com.c206.backend.domain.plogging.exception.PloggingException;
import com.c206.backend.domain.plogging.repository.PloggingRepository;
import com.c206.backend.domain.plogging.repository.TrashRepository;
import com.c206.backend.domain.plogging.service.TrashService;
import com.c206.backend.domain.quest.repository.MemberQuestRepository;
import com.c206.backend.domain.quest.service.MemberQuestService;
import com.c206.backend.domain.quest.service.MemberQuestServiceImpl;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TrashServiceImpl implements TrashService {

    private final PloggingRepository ploggingRepository;
    private final MemberInfoRepository memberInfoRepository;
    private final MemberAchievementRepository memberAchievementRepository;
    private final MemberQuestRepository memberQuestRepository;
    private final MemberPetRepository memberPetRepository;
    private final PetRepository petRepository;
    private final MemberRepository memberRepository;
    private final MemberPetService memberPetService;
    private final TrashRepository trashRepository;
    private final MemberAchievementService memberAchievementService;
    private final MemberQuestService memberQuestService;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;

    private final WebClient webClient;

    public TrashType classifyTrash(String imageUrl) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("url", imageUrl);
            Map<String, String> response = webClient.post()
                    .uri("") // Flask 서버의 엔드포인트 URI 설정
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            if (response == null) {
                throw new PloggingException(PloggingError.FLASK_SERVER_ERROR);
            }

            String type = response.get("type");
            if (type == null) {
                throw new PloggingException(PloggingError.FLASK_SERVER_ERROR);
            } else if (type.equals("NONE")) {
                throw new PloggingException(PloggingError.TRASH_NOT_DETECTED);
            }
            return TrashType.valueOf(type); // 응답을 Enum으로 변환

        } catch (Exception e) {
            throw new PloggingException(PloggingError.FLASK_SERVER_ERROR);
        }
    }


    private String generateFileName(Long ploggingId) {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd_HHmmssSSS");
        String formattedDateTime = currentTime.format(formatter);
        return "trash_" + ploggingId + "_" + formattedDateTime + ".jpg";
    }

    private String uploadImageAndGetUrl(String fileName, byte[] imageData) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageData.length);
        // S3에 이미지 업로드
        amazonS3.putObject(bucket, fileName, new ByteArrayInputStream(imageData), metadata);
        URL imageUrl = amazonS3.getUrl(bucket, fileName);
        return imageUrl.toString();
    }

    private CreateTrashResponseDto trashLogic(
            Plogging plogging, TrashType trashType, String imageUrl, double longitude, double latitude) {

        // 쓰레기 저장
        // Coordinate 객체를 사용하여 Point 생성
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate coord = new Coordinate(longitude, latitude);
        Point point = geometryFactory.createPoint(coord);
        Trash trash = Trash.builder()
                .plogging(plogging)
                .trashType(trashType)
                .location(point)
                .image(imageUrl)
                .build();
        trashRepository.save(trash);
        // 보상 정의 하기
        // 사람에게 경험치가 들어가야한다 조건과 상관없이 exp(일반 55,플라스틱 66,캔 111, 유리 199)
        // 펫 isActive = 재화(일반 100,플라스틱 110,캔 160, 유리 270)
        // notActive = pet exp(일반 50,플라스틱 60,캔 100, 유리 200)
        // 펫이 처치한 몬스터수 증가시키기
        boolean petActive = plogging.getMemberPet().isActive();
        MemberPet memberPet = plogging.getMemberPet();
        Long memberId = plogging.getMember().getId();
        int exp = 0, value = 0;
        switch (trashType) {
            case NORMAL -> {
                exp = 55 ;
                value=petActive ? 100 : 50;memberPet.addNormal();
                memberAchievementService.updateMemberAchievement(memberId, 4L,1);
                memberQuestService.updateMemberQuest(memberId,memberPet.getId(),3L);}
            case PLASTIC -> {
                exp = 66;
                value=petActive ? 110 : 60;memberPet.addPlastic();
                memberAchievementService.updateMemberAchievement(memberId, 5L,1);
                memberQuestService.updateMemberQuest(memberId,memberPet.getId(),2L);}
            case CAN -> {
                exp = 111;
                value=petActive ? 160 : 100;memberPet.addCan();
                memberAchievementService.updateMemberAchievement(memberId, 6L,1);
                memberQuestService.updateMemberQuest(memberId,memberPet.getId(),5L);}
            case GLASS -> {
                exp = 199;
                value =petActive ? 270 : 200;memberPet.addGlass();
                memberAchievementService.updateMemberAchievement(memberId, 7L,1);
                memberQuestService.updateMemberQuest(memberId,memberPet.getId(),4L);}
        };

        int currency = petActive ? value:0;
        // notActive 일때 펫 경험치 주기
        if (!petActive) {
            plogging.getMemberPet().addExp(value);
        }
        // 새로운 회원정보 저장
        MemberInfo memberInfo = memberInfoRepository.findTopByMemberIdOrderByIdDesc(memberId);
        MemberInfo newMemberInfo = MemberInfo.builder()
                .member(plogging.getMember())
                .profilePetId(memberInfo.getProfilePetId())
                .exp(memberInfo.getExp()+exp)
                .currency(memberInfo.getCurrency() + currency)
                .build();
        memberInfoRepository.save(newMemberInfo);
        Member member = memberRepository.findById(memberId).orElseThrow(()
                -> new MemberException(MemberError.NOT_FOUND_MEMBER));
        List<MemberPet> memberPets = memberPetRepository.findByMemberIdAndPetPetType(memberId,PetType.NORMAL);
        List<Pet> pets = petRepository.findByPetTypeIs(PetType.NORMAL);
        // pet 획득 여부 획득 확률 5%
        boolean result = Math.random() < 0.05;
        // 펫 구출과는 조금 다르다
        // 펫구출시 모든 펫을 소지중이면 모두 에러로 보낸다
        // 하지만 플로깅시에는 쓰레기 종류를 반환해야한다
        // 따라서 펫 구출에 실패했다고 하고 에러로 보내지 않는다
        if (memberPets.size() == pets.size()) {
            result = false;
        }
        if (result) {
            memberPetService.createMemberPet(memberPets, pets, member);
        }
        return new CreateTrashResponseDto(
                trashType,
                value,
                petActive,
                result
        );

    }

    @Override
    public CreateTrashResponseDto createTrash(Long ploggingId, CreateTrashRequestDto createTrashRequestDto) {
        Plogging plogging = ploggingRepository.findById(ploggingId).orElseThrow(()
                -> new PloggingException(PloggingError.NOT_FOUND_PLOGGING));
        // 파일 이름 생성
        String fileName = generateFileName(ploggingId);
        byte[] imageData = createTrashRequestDto.getImage();
        // 이미지 s3로 업로드
        String imageUrl = uploadImageAndGetUrl(fileName, imageData);
        TrashType trashType = classifyTrash(imageUrl);
        return trashLogic(plogging,
                trashType, imageUrl, createTrashRequestDto.getLongitude(), createTrashRequestDto.getLatitude());
    }

    @Override
    public CreateTrashResponseDto createTrashTest(Long ploggingId, LocationInfo locationInfo) {
        Plogging plogging = ploggingRepository.findById(ploggingId).orElseThrow(()
                -> new PloggingException(PloggingError.NOT_FOUND_PLOGGING));

        String imageUrl = "https://ggbro.s3.ap-northeast-2.amazonaws.com/test/test_image.png";
        TrashType[] trashTypes = {TrashType.NORMAL, TrashType.PLASTIC, TrashType.CAN, TrashType.GLASS};
        Random random = new Random();
        int randomIndex = random.nextInt(trashTypes.length);
        TrashType trashType = trashTypes[randomIndex];
        return trashLogic(plogging,
                trashType, imageUrl, locationInfo.getLongitude(), locationInfo.getLatitude());
    }


    @Override
    public GetTrashResponseDto getTrashList(GetTrashRequestDto getTrashRequestDto) {
        List<Object[]> results = trashRepository.countTrashByTypeWithinDistance(
                getTrashRequestDto.getLatitude(),
                getTrashRequestDto.getLongitude(),
                getTrashRequestDto.getRadius());
        int normal=0,plastic = 0,can=0,glass=0;
        for (Object[] result : results) {
            int count = ((Number) result[1]).intValue();
            switch ((String) result[0]) {
                case "NORMAL" -> normal = count;
                case "PLASTIC" -> plastic = count;
                case "CAN" -> can = count;
                case "GLASS" -> glass = count;
            }
        }
        return new GetTrashResponseDto(
                normal,
                plastic,
                can,
                glass);
    }
}
