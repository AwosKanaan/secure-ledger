package org.secureledger.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.secureledger.dto.LoginRequest;
import org.secureledger.security.AuthenticatedUser;
import org.secureledger.security.JwtService.IssuedToken;
import org.secureledger.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService service;

    @Test
    void login_validCredentials_returnsAccessToken() {
        UUID id = UUID.randomUUID();
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.id()).thenReturn(id);
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        when(jwtService.issue(id)).thenReturn(new IssuedToken("jwt", Duration.ofMinutes(15)));

        var token = service.login(new LoginRequest("gilbert@gmail.com", "gilbert123"));

        assertThat(token.accessToken()).isEqualTo("jwt");
    }
}
