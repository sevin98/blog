package sevin.dev.blog.domain.auth.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sevin.dev.blog.domain.auth.AuthConstants;
import sevin.dev.blog.domain.auth.entity.Admin;
import sevin.dev.blog.domain.auth.repository.AdminRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AdminRepository adminRepository;

    @Value("${app.admin.github-username}")
    private String adminGithubUsername;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String githubUsername = oAuth2User.getAttribute(AuthConstants.GITHUB_USERNAME_ATTR);
        log.info("OAuth2 login attempt: {}", githubUsername);

        if (!adminGithubUsername.equals(githubUsername)) {
            log.warn("Unauthorized login attempt by: {}", githubUsername);
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "허가되지 않은 사용자입니다.");
        }

        provisionAdmin(githubUsername);
        return oAuth2User;
    }

    private void provisionAdmin(String githubUsername) {
        if (adminRepository.findByGithubUsername(githubUsername).isPresent()) {
            return;
        }
        try {
            adminRepository.saveAndFlush(Admin.of(githubUsername));
            log.info("First login — admin record created for: {}", githubUsername);
        } catch (DataIntegrityViolationException e) {
            // 동시 최초 로그인 시 unique 충돌 — 이미 다른 요청이 저장 완료
            log.warn("Admin record already exists (concurrent creation), skipping: {}", githubUsername);
        }
    }
}
