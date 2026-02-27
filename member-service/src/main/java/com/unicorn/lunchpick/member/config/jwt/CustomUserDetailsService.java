package com.unicorn.lunchpick.member.config.jwt;

import com.unicorn.lunchpick.member.repository.jpa.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 커스텀 UserDetailsService 구현체
 *
 * <p>Spring Security의 인증 처리를 위해 memberId로 회원 정보를 로드합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /**
     * memberId로 사용자 상세 정보 로드
     *
     * @param memberId 회원 도메인 식별자 (UUID 문자열)
     * @return UserDetails 인증 사용자 정보
     * @throws UsernameNotFoundException 회원을 찾을 수 없을 때
     */
    @Override
    public UserDetails loadUserByUsername(String memberId) throws UsernameNotFoundException {
        return memberRepository.findByMemberId(memberId)
                .map(member -> User.builder()
                        .username(member.getMemberId())
                        .password("")
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("USER")))
                        .build())
                .orElseThrow(() -> {
                    log.warn("회원 조회 실패 — memberId: {}", memberId);
                    return new UsernameNotFoundException("회원을 찾을 수 없습니다: " + memberId);
                });
    }
}
