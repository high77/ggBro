package com.c206.backend.domain.plogging.repository;

import com.c206.backend.domain.plogging.entity.Trash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrashRepository extends JpaRepository<Trash, Long> {

    @Query("SELECT t.trashType, COUNT(t) FROM Trash t WHERE t.plogging.id = :ploggingId GROUP BY t.trashType")
    List<Object[]> countTrashByPloggingId(Long ploggingId);

//     @Query(value = "SELECT trash_type, COUNT(*) FROM trash WHERE ST_Distance_Sphere(point(:longitude, :latitude), location) <= :radius GROUP BY trash_type", nativeQuery = true)
//     List<Object[]> countTrashByTypeWithinDistance(double latitude, double longitude, double radius);

    // 공간함수를 활용하여 일정 거리 내 종류별 쓰래기 개수 조회
    @Query(value = """
        SELECT trash_type, COUNT(*)
        FROM trash
        WHERE ST_Distance_Sphere(POINT(:longitude, :latitude), location) <= :radius
        GROUP BY trash_type
        """, nativeQuery = true)
    List<Object[]> countTrashByTypeWithinDistance(double latitude, double longitude, double radius);


    List<Trash> findByPloggingId(Long ploggingId);

}
