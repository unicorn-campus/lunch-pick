package com.unicorn.lunchpick.member.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.member.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.member.dto.request.DietaryRestrictionsRequest;
import com.unicorn.lunchpick.member.dto.request.LocationConsentRequest;
import com.unicorn.lunchpick.member.dto.request.UpdateProfileRequest;
import com.unicorn.lunchpick.member.dto.response.DietaryRestrictionsResponse;
import com.unicorn.lunchpick.member.dto.response.LocationConsentResponse;
import com.unicorn.lunchpick.member.dto.response.MemberProfileResponse;
import com.unicorn.lunchpick.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 프로필 컨트롤러
 *
 * <p>프로필 조회/수정, 위치 동의, 식이 제한 설정 API를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "프로필 API", description = "회원 프로필 조회/수정, 위치 동의, 식이 제한 설정")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ProfileController {

    private final MemberService memberService;

    /**
     * 회원 프로필 조회
     *
     * @param principal 인증된 사용자 정보
     * @return 회원 프로필 응답
     */
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 회원의 프로필 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        MemberProfileResponse response = memberService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 회원 프로필 수정 (닉네임, 알림 설정)
     *
     * @param principal 인증된 사용자 정보
     * @param request   프로필 수정 요청
     * @return 수정된 회원 프로필 응답
     */
    @Operation(summary = "내 프로필 수정", description = "닉네임 또는 알림 설정을 수정합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "닉네임 형식 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        MemberProfileResponse response = memberService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 위치 정보 동의 처리
     *
     * @param principal 인증된 사용자 정보
     * @param request   위치 동의 요청
     * @return 위치 동의 처리 결과
     */
    @Operation(summary = "위치 동의 설정", description = "위치 기반 추천을 위한 위치 정보 동의 여부를 설정합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공")
    @PostMapping("/me/location-consent")
    public ResponseEntity<ApiResponse<LocationConsentResponse>> updateLocationConsent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LocationConsentRequest request) {
        LocationConsentResponse response = memberService.updateLocationConsent(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 식이 제한 정보 설정
     *
     * @param principal 인증된 사용자 정보
     * @param request   식이 제한 설정 요청
     * @return 식이 제한 설정 결과
     */
    @Operation(summary = "식이 제한 설정", description = "알레르기 항목 및 식이 유형을 설정합니다. 건강 정보 동의 필수.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "설정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "건강 정보 동의 미완료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/me/dietary-restrictions")
    public ResponseEntity<ApiResponse<DietaryRestrictionsResponse>> updateDietaryRestrictions(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DietaryRestrictionsRequest request) {
        DietaryRestrictionsResponse response = memberService.updateDietaryRestrictions(
                principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
