package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SegmentWordAccuracyRepository extends JpaRepository<SegmentWordAccuracy, Long> {
    List<SegmentWordAccuracy> findAllByUserSegmentProgressIdInOrderByIdAsc(Collection<Long> progressIds);

    List<SegmentWordAccuracy> findAllByUserSegmentProgressIdOrderByIdAsc(Long progressId);

    // flushAutomatically = true để đẩy các INSERT đang chờ xuống trước khi DELETE.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM SegmentWordAccuracy w WHERE w.userSegmentProgress.id = :progressId")
    void deleteAllByUserSegmentProgressId(Long progressId);

    // Xóa tất cả word accuracy liên quan đến danh sách segmentId (dùng khi xóa segment)
    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM SegmentWordAccuracy w
            WHERE w.userSegmentProgress.id IN (
                SELECT p.id FROM UserSegmentProgress p WHERE p.segment.id IN :segmentIds)
            """)
    void deleteAllBySegmentIds(@Param("segmentIds") Collection<Long> segmentIds);
}
