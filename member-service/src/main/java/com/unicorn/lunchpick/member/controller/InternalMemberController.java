package com.unicorn.lunchpick.member.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.member.dto.response.TasteProfileResponse;
import com.unicorn.lunchpick.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 내부 API 컨트롤러
 *
 * <p>추천 서비스 등 내부 마이크로서비스에서 호출하는 API를 제공합니다.
 * VPC 내부 네트워크에서만 접근 가능하도록 SecurityConfig에서 {@code /internal/**}를 permitAll로 설정합니다.</p>
 *
 * <p><b>주의사항:</b> 외부 인터넷에 노출되면 안 됩니다. 인프라 수준의 접근 제어 필수.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "내부 API", description = "서비스 간 내부 통신 전용 (VPC 내부에서만 접근)")
@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class InternalMemberController {

    private final MemberService memberService;

    /**
     * 취향 프로파일 조회 (추천 서비스 전용)
     *
     * <p>추천 서비스가 개인화 추천 생성 시 호출합니다.
     * 온보딩 미완료 회원의 경우 빈 취향 벡터와 {@code isColdStart: true}를 반환합니다.</p>
     *
     * @param memberId 회원 도메인 식별자 (UUID)
     * @return 취향 프로파일 응답 (tasteVector, allergenFilter, dietType, isColdStart)
     */
    @Operation(summary = "취향 프로파일 조회", description = "추천 서비스가 개인화 추천 생성 시 호출하는 내부 API입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{memberId}/taste-profile")
    public ResponseEntity<ApiResponse<TasteProfileResponse>> getTasteProfile(
            @PathVariable String memberId) {
        TasteProfileResponse response = memberService.getTasteProfile(memberId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
