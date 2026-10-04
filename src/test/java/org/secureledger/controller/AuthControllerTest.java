package org.secureledger.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.secureledger.dto.LoginRequest;
import org.secureledger.service.AuthService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService service;
    @InjectMocks AuthController controller;

    @Test
    void createToken_validRequest_delegatesToAuthService() {
        LoginRequest request = new LoginRequest("gilbert@gmail.com", "gilbert123");
        controller.createToken(request);
        verify(service).login(request);
    }
}
