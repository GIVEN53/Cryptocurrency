package OneCoin.Server.config.auth.dto;

import lombok.Getter;

/**
 * <pre>
 *     username, password 를  Security Filter 에서 사용할 수 있도록 역직렬화
 * </pre>
 */
@Getter
public class LoginDto {
    private String email;    // email
    private String password;
}
