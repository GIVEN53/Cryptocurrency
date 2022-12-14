package OneCoin.Server.user.controller;

import OneCoin.Server.config.auth.jwt.JwtTokenizer;
import OneCoin.Server.dto.PageResponseDto;
import OneCoin.Server.dto.SingleResponseDto;
import OneCoin.Server.user.dto.UserDto;
import OneCoin.Server.user.entity.User;
import OneCoin.Server.user.mapper.UserMapper;
import OneCoin.Server.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Validated
@Slf4j
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    @Value("${spring.client.ip}")
    private String baseURL;
    private final JwtTokenizer jwtTokenizer;

    public UserController(UserService userService, UserMapper userMapper, JwtTokenizer jwtTokenizer) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtTokenizer = jwtTokenizer;
    }

    // 이메일 인증
    @PostMapping("/authentication-email")
    public ResponseEntity authenticationEmail(@Valid @RequestBody UserDto.Post requestBody) {
        userService.authenticationEmail(userMapper.userPostToUser(requestBody));

        return new ResponseEntity<>(
                new SingleResponseDto<>("Send Email"), HttpStatus.CREATED
        );
    }

    @PostMapping
    public ResponseEntity postUser(@Valid @RequestBody UserDto.Post requestBody) {
        User user = userService.createUser(userMapper.userPostToUser(requestBody));

        userService.authenticationEmail(user);

        return new ResponseEntity<>(
                new SingleResponseDto<>(userMapper.userToUserResponse(user)), HttpStatus.CREATED
        );
    }

    @PatchMapping("/display-name")
    public ResponseEntity patchUser(
            @Valid @RequestBody UserDto.Patch requestBody,
            @AuthenticationPrincipal Map<String, Object> userInfo) {
        User user = userMapper.userPatchToUser(requestBody);
        user.setUserId(Long.parseLong(userInfo.get("id").toString()));

        User updatedUser = userService.updateUser(user);

        return new ResponseEntity<>(
                new SingleResponseDto<>(userMapper.userToUserResponse(updatedUser)), HttpStatus.OK
        );
    }

    // 비밀번호 재설정
    @PatchMapping("/reset-passwords")
    public ResponseEntity patchUser(
            @Valid @RequestBody UserDto.Password requestBody,
            @AuthenticationPrincipal Map<String, Object> userInfo) {
        User user = userMapper.userPasswordToUser(requestBody);
        user.setUserId(Long.parseLong(userInfo.get("id").toString()));

        User updatedUser = userService.resetPassword(user);

        return new ResponseEntity<>(
                new SingleResponseDto<>(userMapper.userToUserResponse(updatedUser)), HttpStatus.OK
        );
    }

    // 닉네임 중복 체크
    @GetMapping("/duplicate-display-name")
    public ResponseEntity checkDisplayName(@Valid @RequestParam String displayName) {
        return new ResponseEntity<>(userService.checkDuplicateDisplayName(displayName), HttpStatus.OK);
    }

    // 이메일 중복 체크 error 로 변경
    @GetMapping("/duplicate-email")
    public ResponseEntity checkEmail(@Valid @RequestParam String email) {
        return new ResponseEntity<>(userService.checkDuplicateEmail(email), HttpStatus.OK);
    }

    // 모든 회원 정보
    @GetMapping
    public ResponseEntity getUsers(@Positive @RequestParam int page,
                                   @Positive @RequestParam int size) {
        Page<User> userPage = userService.findUsers(page - 1, size);
        List<User> users = userPage.getContent();
        return new ResponseEntity<>(
                new PageResponseDto<>(userMapper.usersToUserResponses(users),
                        userPage),
                HttpStatus.OK);
    }

    // 토큰으로 단일 회원 정보
    @GetMapping("/my-information")
    public ResponseEntity getUser( @AuthenticationPrincipal Map<String, Object> userInfo) {
        User user = userService.findUser(Long.parseLong(userInfo.get("id").toString()));
        return new ResponseEntity<>(new SingleResponseDto<>(userMapper.userToUserResponse(user)), HttpStatus.OK);
    }

    // 단일 회원 정보
    @GetMapping("/{user-id}")
    public ResponseEntity getUser(@PathVariable("user-id") @Positive long userId) {
        User user = userService.findUser(userId);
        return new ResponseEntity<>(new SingleResponseDto<>(userMapper.userToUserResponse(user)), HttpStatus.OK);
    }

    // 이메일 인증 확인
    @GetMapping("/authentication-email/signup/{user-id}/{password}")
    public ResponseEntity authenticationEmail(@PathVariable("user-id") @Positive long userId,
                                              @PathVariable("password") String password) throws URISyntaxException {
        userService.confirmEmail(userId, password);

        URI redirect = new URI("http://" + baseURL + "/login");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(redirect);

        return new ResponseEntity(httpHeaders, HttpStatus.SEE_OTHER);
    }

    // 비밀번호 변경 이메일 인증 확인
    @GetMapping("/authentication-email/password/{auth-id}/{password}")
    public ResponseEntity authenticationEmailByPassword(@PathVariable("auth-id") @Positive long authId,
                                                        @PathVariable("password") String password) throws URISyntaxException {
        User user = userService.confirmEmailByPassword(authId, password);

        String accessToken = userService.delegateAccessToken(user, jwtTokenizer);
        String refreshToken = userService.delegateRefreshToken(user, jwtTokenizer);

        URI redirect = new URI("http://" + baseURL + "/token/password?authorization="
                + accessToken + "&refresh="
                + refreshToken);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(redirect);

        return new ResponseEntity(httpHeaders, HttpStatus.SEE_OTHER);
    }

    // 비밀번호 찾기 이메일
    @GetMapping("/find-password")
    public ResponseEntity findPassword(@Valid @RequestParam String email) {
        User user = userService.findVerifiedUserByEmail(email);
        userService.authenticationEmailForPassword(user);

        return new ResponseEntity<>("Send Email", HttpStatus.OK);
    }

    // 프로필 사진
    @GetMapping(value = "/images/{user-id}",
            produces={MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE})
    public ResponseEntity<byte[]> getProfileImage(@PathVariable("user-id") @Positive int userId) throws IOException {
        User user = userService.findUser(userId);

        byte[] image = userService.pathToImage(user.getImagePath());

        return new ResponseEntity<byte[]>(
                image,
                HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity deleteUser(@AuthenticationPrincipal Map<String, Object> userInfo) {
        userService.deleteUser(Long.parseLong(userInfo.get("id").toString()));
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
}
