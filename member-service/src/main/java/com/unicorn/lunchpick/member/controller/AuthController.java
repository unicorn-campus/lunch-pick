package com.unicorn.lunchpick.member.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.member.dto.request.KakaoLoginRequest;
import com.unicorn.lunchpick.member.dto.response.AuthResponse;
import com.unicorn.lunchpick.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 컨트롤러
 *
 * <p>카카오 소셜 로그인 API를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "인증 API", description = "카카오 소셜 로그인")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 소셜 로그인
     *
     * <p>기존 회원이면 200 OK ({@code isNewUser: false}),
     * 신규 회원이면 201 Created ({@code isNewUser: true})를 반환합니다.</p>
     *
     * @param request 카카오 인증코드 요청
     * @return JWT 액세스 토큰 및 회원 정보
     */
    @Operation(summary = "카카오 소셜 로그인", description = "카카오 인증코드로 로그인하거나 신규 회원을 가입시킵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기존 회원 로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신규 회원 가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "카카오 인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<AuthResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request) {
        AuthResponse response = authService.kakaoLogin(request);
        HttpStatus status = response.isNewUser() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(response));
    }
}
