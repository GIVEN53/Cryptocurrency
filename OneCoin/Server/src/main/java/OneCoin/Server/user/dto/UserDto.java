package OneCoin.Server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;

public class UserDto {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Post {
        // 닉네임 : 영어로만 가능하고, 2글자 이상 최대 길이 16자 이하
        @NotBlank
        @Pattern(regexp = "^[A-Za-z\\d]{2,16}$")
        private String displayName;

        // 이메일 : 이메일 형식이며, 최대 길이 50자 이하
        @NotBlank
        @Email
        @Size(min=3, max=50)
        private String email;

        // 정규표현식 제약조건 추가 필요
        // 비밀번호 : 숫자+영문자+특수문자 최소길이 8자 이상 최대길이 25자 이하
        @NotBlank
        @Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[$@!%*?&])[A-Za-z\\d$@!%*?&]{8,25}")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Patch {
        // 닉네임 : 영어로만 가능하고, 2글자 이상 최대 길이 16자 이하
        @NotBlank
        @Size(min=2, max=16)
        private String displayName;

        // 이메일 : 이메일 형식이며, 최대 길이 50자 이하
        @NotBlank
        @Email
        @Size(min=3, max=50)
        private String email;

        // 정규표현식 제약조건 추가 필요
        // 비밀번호 : 숫자+영문자+특수문자 최소길이 8자 이상 최대길이 25자 이하
        @NotBlank
        @Pattern(regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[$@!%*?&])[A-Za-z\\d$@!%*?&]{8,25}")
        private String password;

//        @PositiveOrZero
//        private long balance;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private long id;

        private String displayName;

        private String email;
    }

}