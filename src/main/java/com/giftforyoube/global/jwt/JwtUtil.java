package com.giftforyoube.global.jwt;

import com.giftforyoube.global.exception.BaseException;
import com.giftforyoube.global.exception.BaseResponseStatus;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${spring.jwt.secret.key}")
    private String secretKey;
    private Key key;
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private final long TOKEN_TIME = 24 * 60 * 60 * 1000L;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // 1. 쿠키에서 JWT 토큰을 가져오는 경우
    public String getTokenFromCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_HEADER)) {
                    try {
                        String decodeToken = URLDecoder.decode(cookie.getValue(), "UTF-8");
                        log.info("[getTokenFromCookie] decodeToken: " + decodeToken);
                        return decodeToken;
                    } catch (UnsupportedEncodingException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    // 2. 헤더에서 JWT 토큰을 가져오는 경우
    public String getTokenFromRequestHeader(HttpServletRequest httpServletRequest) {
        String authorizationHeader = httpServletRequest.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader != null) {
            try {
                String decodeToken = URLDecoder.decode(authorizationHeader, "UTF-8");
                log.info("[getTokenFromRequestHeader] decodeToken: " + decodeToken);
                return decodeToken;
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return null;
    }

    public static String substringToken(String decodeToken) {
        if (StringUtils.hasText(decodeToken) && decodeToken.startsWith(BEARER_PREFIX)) {
            String tokenValue = decodeToken.substring(7);
            return tokenValue;
        }
        throw new BaseException(BaseResponseStatus.NOT_FOUND_TOKEN);
    }

    public boolean validateToken(String tokenValue) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(tokenValue);
            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            throw new BaseException(BaseResponseStatus.INVALID_TOKEN);
        } catch (ExpiredJwtException e) {
            throw new BaseException(BaseResponseStatus.EXPIRED_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new BaseException(BaseResponseStatus.NOT_FOUND_TOKEN);
        }
    }

    public Claims getUserInfoFromToken(String tokenValue) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(tokenValue)
                .getBody();
    }

    public String createToken(String email) {
        Date date = new Date();
        String token = BEARER_PREFIX +
                Jwts.builder()
                        .setSubject(email)
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME))
                        .setIssuedAt(date)
                        .signWith(key, signatureAlgorithm)
                        .compact();
        log.info("[createToken] JWT 생성 완료: " + token);
        return token;
    }

    // 기존 코드
//    public static String addJwtToCookie(String token,
//                                        HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
//        String encodeToken = URLEncoder.encode(token, "utf-8").replaceAll("\\+", "%20");
//
//        Cookie cookie = new Cookie(AUTHORIZATION_HEADER, encodeToken);
//        cookie.setPath("/");
//        cookie.setHttpOnly(true);
//        cookie.setSecure(true);
////        cookie.setDomain("giftipie.me"); // 쿠키가 전송될 도메인 설정 - eventSource 의 쿠키 전달을 위함
//        httpServletResponse.addCookie(cookie);
//        log.info("[addJwtToCookie] JWT 쿠키 전달 완료: " + encodeToken);
//        return encodeToken;
//    }

    // 테스트를 위한 ResponseCookie 사용
    public static String addJwtToCookie(String token,
                                        HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
        String encodeToken = URLEncoder.encode(token, "utf-8").replaceAll("\\+", "%20");

        ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_HEADER, encodeToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None") // SameSite 설정 추가
                .build();

        httpServletResponse.setHeader("Set-Cookie", cookie.toString());
        log.info("[addJwtToCookie] JWT 쿠키 전달 완료: " + encodeToken);
        return encodeToken;
    }

    public void logout(HttpServletRequest req, HttpServletResponse res) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(AUTHORIZATION_HEADER)) {
                    cookie.setValue(""); // 쿠키 값 비우기
                    cookie.setPath("/");
                    cookie.setMaxAge(0); // 쿠키 만료
                    res.addCookie(cookie); // 응답에 쿠키 추가
                    break;
                }
            }
        }
    }
}