package com.elingo.learning.lesson.repository;

import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SegmentWordAccuracyRepository extends JpaRepository<SegmentWordAccuracy, Long> {
    List<SegmentWordAccuracy> findAllByUserSegmentProgressIdInOrderByIdAsc(Collection<Long> progressIds);

    List<SegmentWordAccuracy> findAllByUserSegmentProgressIdOrderByIdAsc(Long progressId);

    // Không đặt clearAutomatically = true: sẽ detach UserSegmentProgress đang được quản lý, field sửa sau đó không được lưu.
    // flushAutomatically = true để đẩy các INSERT đang chờ xuống trước khi DELETE.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM SegmentWordAccuracy w WHERE w.userSegmentProgress.id = :progressId")
    void deleteAllByUserSegmentProgressId(Long progressId);
}
