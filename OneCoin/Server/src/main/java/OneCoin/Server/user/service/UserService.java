package OneCoin.Server.user.service;

import OneCoin.Server.config.auth.utils.CustomAuthorityUtils;
import OneCoin.Server.exception.BusinessLogicException;
import OneCoin.Server.exception.ExceptionCode;
import OneCoin.Server.user.entity.User;
import OneCoin.Server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.internet.MimeMessage;
import java.util.Optional;

@Transactional
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthorityUtils customAuthorityUtils;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String authEmail;

    @Value("${spring.mail.password}")
    private String authPassword;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CustomAuthorityUtils customAuthorityUtils, JavaMailSender javaMailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customAuthorityUtils = customAuthorityUtils;
        this.javaMailSender = javaMailSender;
    }

    /**
     * 유저 생성
     */
    @Transactional
    public User createUser(User user) {
        // 계정 존재 여부 조회
        if (!hasAccount(user.getEmail())) {
            //패스워드 암호화
            String encryptedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encryptedPassword);

            // email 로 role 생성
            user.setUserRole(customAuthorityUtils.createRoles(user.getEmail()));

            // 계정 생성
            return userRepository.save(user);
        }

        return null;
    }

    /**
     *  <pre>
     *      이메일 인증
     *  </pre>
     */
    @Transactional
    public void authenticationEmail(User user) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        // 인증 링크
        String link = "http://linktest.com";

        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 메세지 바디 설정
            StringBuilder body = new StringBuilder();
            body.append("<html> <body><h1>OneCoin 이메일 인증</h1>");
            body.append("<div>다음 링크로 이동하여 인증을 완료하십시오.</div>");
            body.append("<div><a href=\"" + link + "\" target=\"_blank\">인증 완료하기</a></div> </body></html>");

            System.out.println(body);

            // MimeMessageHelper 설정
            mimeMessageHelper.setFrom(authEmail);    // 보내는 사람
            mimeMessageHelper.setTo(user.getEmail());   // 받는 사람
            mimeMessageHelper.setSubject("OneCoin 이메일 인증");     // 제목
            mimeMessageHelper.setText(body.toString(), true);       // 내용, html true

            // 전송
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <pre>
     *     회원 정보 변경
     *     displayName, email, password 만 변경 가능
     * </pre>
     */
    @Transactional
    public User updateUser(User user) {
        User findUser = findVerifiedUser(user.getUserId());

        findUser.setDisplayName(user.getDisplayName());
        findUser.setEmail(user.getEmail());
        findUser.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(findUser);
    }

    /**
     * <pre>
     *     비밀번호 재설정
     * </pre>
     */
    @Transactional
    public User resetPassword(User user) {
        User findUser = findVerifiedUser(user.getUserId());

        findUser.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(findUser);
    }

    /**
     * <pre>
     *     단일 회원 정보 가져오기
     * </pre>
     */
    @Transactional(readOnly = true)
    public User findUser(long userId) {
        return findVerifiedUser(userId);
    }

    /**
     * <pre>
     *     회원 정보 리스트 가져오기
     * </pre>
     */
    @Transactional(readOnly = true)
    public Page<User> findUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("userId")));
    }

    /**
     * <pre>
     *     닉네임 중복 체크 서비스
     *     닉네임이 동일한 계정이 있으면 에러
     * </pre>
     */
    @Transactional
    public Boolean checkDuplicateDisplayName(String displayName) {
        if (userRepository.existsByDisplayName(displayName)) {
            throw new BusinessLogicException(ExceptionCode.USER_EXISTS);
        }
        return false;
    }

    /**
     * <pre>
     *     이메일 중복 체크 서비스
     *     이메일이 동일한 계정이 있으면 에러
     * </pre>
     */
    @Transactional
    public Boolean checkDuplicateEmail(String email) {
        if (hasAccount(email)) {
            throw new BusinessLogicException(ExceptionCode.USER_EXISTS);
        }
        return false;
    }


    /**
     * <pre>
     *     회원 정보 삭제
     * </pre>
     */
    @Transactional
    public void deleteUser(long userId) {
        User findUser = findVerifiedUser(userId);
        userRepository.delete(findUser);
    }

    /**
     * <pre>
     *     userId로 단일 회원 정보 가져오기
     * </pre>
     */
    @Transactional(readOnly = true)
    public User findVerifiedUser(long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        User findUser = optionalUser.orElseThrow(() -> new BusinessLogicException(ExceptionCode.USER_NOT_FOUND));
        return findUser;
    }

    /**
     * <pre>
     *     계정 존재 여부 조회
     *     이메일이 동일한 계정이 있으면 true
     * </pre>
     */
    @Transactional
    public Boolean hasAccount(String email) {
        return userRepository.existsByEmail(email);
    }
}