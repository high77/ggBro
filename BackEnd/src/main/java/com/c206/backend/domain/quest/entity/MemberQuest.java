package com.c206.backend.domain.quest.entity;

import com.c206.backend.domain.member.entity.Member;
import com.c206.backend.domain.pet.entity.MemberPet;
import com.c206.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberQuest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberQuestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pet_id")
    private MemberPet memberPet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    private int goal;

    private int progress;

    private boolean isDone;

    public void updateProgress(int progress) {
        this.progress = progress;
    }

    public void updateIsDone() {
        this.isDone = true;
    }
}
