package sevin.dev.blog.domain.auth.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import sevin.dev.blog.domain.auth.entity.Admin;
import sevin.dev.blog.domain.auth.repository.AdminRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private CustomOAuth2UserService service;

    private final String adminUsername = "sevin98";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "adminGithubUsername", adminUsername);
    }

    @Test
    @DisplayName("최초 로그인 시 admin 레코드를 생성한다")
    void provisionAdmin_firstLogin_createsRecord() {
        when(adminRepository.findByGithubUsername(adminUsername)).thenReturn(Optional.empty());

        ReflectionTestUtils.invokeMethod(service, "provisionAdmin", adminUsername);

        verify(adminRepository).saveAndFlush(any(Admin.class));
    }

    @Test
    @DisplayName("이미 admin 레코드가 존재하면 저장을 시도하지 않는다")
    void provisionAdmin_alreadyExists_doesNotSave() {
        when(adminRepository.findByGithubUsername(adminUsername))
                .thenReturn(Optional.of(Admin.of(adminUsername)));

        ReflectionTestUtils.invokeMethod(service, "provisionAdmin", adminUsername);

        verify(adminRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("동시 최초 로그인으로 unique 충돌이 발생해도 예외를 전파하지 않는다")
    void provisionAdmin_concurrentCreation_handlesGracefully() {
        when(adminRepository.findByGithubUsername(adminUsername)).thenReturn(Optional.empty());
        when(adminRepository.saveAndFlush(any(Admin.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value"));

        assertThatNoException().isThrownBy(
                () -> ReflectionTestUtils.invokeMethod(service, "provisionAdmin", adminUsername));
    }
}
