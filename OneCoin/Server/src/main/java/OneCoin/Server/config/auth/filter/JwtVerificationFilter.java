package OneCoin.Server.config.auth.filter;

import OneCoin.Server.config.auth.jwt.JwtTokenizer;
import OneCoin.Server.config.auth.utils.CustomAuthorityUtils;
import OneCoin.Server.user.entity.Role;
import OneCoin.Server.user.entity.User;
import OneCoin.Server.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *     JWT 검증 EntryPoint
 *     OncePerRequestFilter : request 당 한번만 실행되는 Security Filter 를 구현 가능
 * </pre>
 */
public class JwtVerificationFilter extends OncePerRequestFilter {
    private final JwtTokenizer jwtTokenizer;
    private final CustomAuthorityUtils customAuthorityUtils;
    private final UserService userService;

    public JwtVerificationFilter(JwtTokenizer jwtTokenizer, CustomAuthorityUtils customAuthorityUtils, UserService userService) {
        this.jwtTokenizer = jwtTokenizer;
        this.customAuthorityUtils = customAuthorityUtils;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        Map<String, Object> claims = verifyJws(request);
//        setAuthenticationToContext(claims);

        try {
            Map<String, Object> claims = verifyJws(request);
            System.out.println("Claim : " + claims.get("roles"));
            setAuthenticationToContext(claims);
        } catch (SignatureException se) {
            request.setAttribute("exception", se);
        } catch (ExpiredJwtException ee) {          // 유효기간 만료된 경우
            try {
                // 리프레시 토큰 체크
                Map<String, Object> claims = verifyRefreshJws(request);

                // DB에 리프레시를 저장하여 판단하는 경우도 있지만, 일단 이렇게 구현
                User user = userService.findVerifiedUserByEmail(claims.get("sub").toString());


                String accessToken = userService.delegateAccessToken(user, jwtTokenizer);
                String refreshToken = userService.delegateRefreshToken(user, jwtTokenizer);

                response.setHeader("Authorization", "Bearer " + accessToken);
                response.setHeader("Refresh", refreshToken);
            } catch (Exception e) {
                request.setAttribute("exception", e);
            }
        } catch (Exception e) {
            request.setAttribute("exception", e);
        }

        // JWT 검증과 Authentication 객체를 저장 완료였으므로 다음 필터 실행
        filterChain.doFilter(request, response);
    }

    /**
     * true 면 해당 필터 동작을 수행하지 않고 다음 필터로 건너뜀
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String authorization = request.getHeader("Authorization");

        return authorization == null || !authorization.startsWith("Bearer");  // Bearer 이 아니면 true(현재 필터 스킵)
    }

    /**
     * Access Token 검증
     */
    private Map<String, Object> verifyJws(HttpServletRequest request) {
        String jws = request.getHeader("Authorization").replace("Bearer ", "");
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey()); // SecretKey 파싱
        Map<String, Object> claims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();   // Claims 파싱

        return claims;
    }

    /**
     * Refresh Token 검증
     */
    private Map<String, Object> verifyRefreshJws(HttpServletRequest request) {
        String jws = request.getHeader("Refresh");
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey()); // SecretKey 파싱
        Map<String, Object> claims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();

        return claims;
    }

    /**
     * Authentication(인증) 객체를 SecurityContext 에 저장
     */
    private void setAuthenticationToContext(Map<String, Object> claims) {
//        String username = (String) claims.get("username");
        List<GrantedAuthority> authorities = customAuthorityUtils.createAuthorities(Role.valueOf((String) claims.get("roles")));
        System.out.println("Auth : " + authorities.get(0));
        System.out.println("Auth : " + authorities.get(1));
//        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(claims, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext 에 Authentication 저장
    }
}
