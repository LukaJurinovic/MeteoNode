package com.example.meteonode.service.application;

import com.example.meteonode.model.dto.response.AuthResponse;
import com.example.meteonode.model.dto.request.LoginRequest;
import com.example.meteonode.model.dto.request.RefreshTokenRequest;
import com.example.meteonode.model.dto.request.RegisterRequest;
import com.example.meteonode.service.domain.RefreshTokenService;
import com.example.meteonode.service.domain.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        var user = userService.getUserByUsername(request.username());
        return new AuthResponse(
                jwtService.generateAccessToken(request.username()),
                refreshTokenService.create(user.id()),
                user.username(),
                user.role().name()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        var encodedPassword = passwordEncoder.encode(request.password());
        var user = userService.createUser(request, encodedPassword);
        return new AuthResponse(
                jwtService.generateAccessToken(request.username()),
                refreshTokenService.create(user.id()),
                user.username(),
                user.role().name()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        var result = refreshTokenService.rotate(request.refreshToken());
        var user = userService.getUserByUsername(result.username());
        return new AuthResponse(
                jwtService.generateAccessToken(result.username()),
                result.newToken(),
                user.username(),
                user.role().name()
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.delete(request.refreshToken());
    }
}
