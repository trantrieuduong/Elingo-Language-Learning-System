package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.entity.CefrLevel;
import com.elingo.vocabulary.mapper.CefrLevelMapper;
import com.elingo.vocabulary.repository.CefrLevelRepository;
import com.elingo.vocabulary.service.impl.CefrLevelServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CefrLevelServiceTest {

    @Mock
    private CefrLevelRepository cefrLevelRepository;

    @Mock
    private CefrLevelMapper cefrLevelMapper;

    @InjectMocks
    private CefrLevelServiceImpl cefrLevelService;

    @Nested
    @DisplayName("getAllCefrLevels")
    class GetAllCefrLevelsTests {

        @Test
        @DisplayName("Get all CEFR levels successfully: maps every entity")
        void getAllCefrLevels_Success() {
            CefrLevel a1 = CefrLevel.builder().id(1L).code("A1").label("Beginner").build();
            CefrLevel b2 = CefrLevel.builder().id(2L).code("B2").label("Upper-Intermediate").build();
            CefrLevelResponse a1Response = new CefrLevelResponse(1L, "A1", "Beginner");
            CefrLevelResponse b2Response = new CefrLevelResponse(2L, "B2", "Upper-Intermediate");

            when(cefrLevelRepository.findAll()).thenReturn(List.of(a1, b2));
            when(cefrLevelMapper.toCefrLevelResponse(a1)).thenReturn(a1Response);
            when(cefrLevelMapper.toCefrLevelResponse(b2)).thenReturn(b2Response);

            List<CefrLevelResponse> result = cefrLevelService.getAllCefrLevels();

            assertThat(result).containsExactly(a1Response, b2Response);
            verify(cefrLevelRepository).findAll();
        }

        @Test
        @DisplayName("Get all CEFR levels successfully: returns empty list when there is no data")
        void getAllCefrLevels_Success_Empty() {
            when(cefrLevelRepository.findAll()).thenReturn(List.of());

            assertThat(cefrLevelService.getAllCefrLevels()).isEmpty();
        }
    }
}
