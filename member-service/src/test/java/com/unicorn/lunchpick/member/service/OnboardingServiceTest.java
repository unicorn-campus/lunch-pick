package com.unicorn.lunchpick.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unicorn.lunchpick.member.dto.request.CardSwipeResult;
import com.unicorn.lunchpick.member.dto.request.OnboardingProgressRequest;
import com.unicorn.lunchpick.member.dto.request.OnboardingRequest;
import com.unicorn.lunchpick.member.dto.response.OnboardingProgressSaveResponse;
import com.unicorn.lunchpick.member.dto.response.OnboardingResponse;
import com.unicorn.lunchpick.member.exception.MemberException;
import com.unicorn.lunchpick.member.repository.entity.MemberEntity;
import com.unicorn.lunchpick.member.repository.jpa.DietaryRestrictionRepository;
import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import com.unicorn.lunchpick.member.repository.jpa.TasteProfileRepository;
import com.unicorn.lunchpick.member.service.impl.OnboardingServiceImpl;
import com.unicorn.lunchpick.member.service.impl.TasteVectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * OnboardingService 단위 테스트
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService 단위 테스트")
class OnboardingServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TasteProfileRepository tasteProfileRepository;

    @Mock
    private DietaryRestrictionRepository dietaryRestrictionRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OnboardingService onboardingService;

    private static final String MEMBER_ID = "member-uuid-test";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        TasteVectorService tasteVectorService = new TasteVectorServiceImpl(objectMapper);
        onboardingService = new OnboardingServiceImpl(
                memberRepository, tasteProfileRepository, dietaryRestrictionRepository,
                tasteVectorService, stringRedisTemplate, objectMapper);
    }

    @Test
    @DisplayName("스와이프 결과가 7장 미만이면 INSUFFICIENT_SWIPES 예외가 발생한다")
    void completeOnboarding_lessThanSevenSwipes_throwsInsufficientSwipes() {
        // given
        List<CardSwipeResult> swipeResults = buildSwipeResults(6);
        OnboardingRequest request = new OnboardingRequest(swipeResults, true);

        // when & then
        assertThatThrownBy(() -> onboardingService.completeOnboarding(MEMBER_ID, request))
                .isInstanceOf(MemberException.class)
                .hasMessageContaining("7장 이상");
    }

    @Test
    @DisplayName("건강 정보 동의가 false이면 HEALTH_INFO_CONSENT_REQUIRED 예외가 발생한다")
    void completeOnboarding_healthInfoConsentFalse_throwsConsentRequired() {
        // given
        List<CardSwipeResult> swipeResults = buildSwipeResults(7);
        OnboardingRequest request = new OnboardingRequest(swipeResults, false);

        // when & then
        assertThatThrownBy(() -> onboardingService.completeOnboarding(MEMBER_ID, request))
                .isInstanceOf(MemberException.class)
                .hasMessageContaining("동의");
    }

    @Test
    @DisplayName("7장 이상 스와이프 + 동의 시 온보딩이 완료되고 topCategories를 반환한다")
    void completeOnboarding_validRequest_completesAndReturnsTopCategories() {
        // given
        List<CardSwipeResult> swipeResults = List.of(
                new CardSwipeResult("c1", true, "한식"),
                new CardSwipeResult("c2", true, "한식"),
                new CardSwipeResult("c3", true, "일식"),
                new CardSwipeResult("c4", true, "일식"),
                new CardSwipeResult("c5", true, "중식"),
                new CardSwipeResult("c6", false, "양식"),
                new CardSwipeResult("c7", true, "한식")
        );
        OnboardingRequest request = new OnboardingRequest(swipeResults, true);
        MemberEntity member = MemberEntity.builder()
                .memberId(MEMBER_ID).kakaoId("k1").email("e@e.com").nickname("닉")
                .onboardingCompleted(false).locationEnabled(false)
                .recommendationAlert(true).feedbackReminder(true).build();

        given(memberRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.of(member));
        given(tasteProfileRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        given(tasteProfileRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(dietaryRestrictionRepository.findByMemberId(MEMBER_ID)).willReturn(Optional.empty());
        given(dietaryRestrictionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(stringRedisTemplate.delete(anyString())).willReturn(true);

        // when
        OnboardingResponse response = onboardingService.completeOnboarding(MEMBER_ID, request);

        // then
        assertThat(response.tasteVectorCreated()).isTrue();
        assertThat(response.topCategories()).isNotEmpty();
        assertThat(response.topCategories().get(0)).isEqualTo("한식");
        assertThat(member.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("온보딩 진행 상태 저장 시 savedCount를 올바르게 반환한다")
    void saveOnboardingProgress_validRequest_returnsSavedCount() {
        // given
        List<CardSwipeResult> swipeResults = buildSwipeResults(4);
        OnboardingProgressRequest request = new OnboardingProgressRequest(swipeResults);

        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        OnboardingProgressSaveResponse response = onboardingService.saveOnboardingProgress(MEMBER_ID, request);

        // then
        assertThat(response.savedCount()).isEqualTo(4);
        assertThat(response.message()).isNotBlank();
    }

    /**
     * 테스트용 스와이프 결과 생성 헬퍼
     *
     * @param count 생성할 스와이프 수
     * @return CardSwipeResult 목록
     */
    private List<CardSwipeResult> buildSwipeResults(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new CardSwipeResult("card-" + i, i % 2 == 0, "한식"))
                .toList();
    }
}
