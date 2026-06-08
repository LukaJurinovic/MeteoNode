package com.example.meteonode.service.domain;

import com.example.meteonode.exception.InvalidTokenException;
import com.example.meteonode.model.entity.RefreshToken;
import com.example.meteonode.model.entity.User;
import com.example.meteonode.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public record RotateResult(String username, String newToken) {}

    @Transactional
    public String create(Integer userId) {
        User user = new User();
        user.setId(userId);

        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(rawToken);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RotateResult rotate(String token) {
        RefreshToken stored = getByToken(token);

        if (stored.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new InvalidTokenException("Refresh token expired");
        }

        String username = stored.getUser().getUsername();
        User user = stored.getUser();

        refreshTokenRepository.delete(stored);

        String newRaw = UUID.randomUUID().toString();
        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setToken(newRaw);
        newToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));
        refreshTokenRepository.save(newToken);

        return new RotateResult(username, newRaw);
    }

    @Transactional
    public void delete(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    private RefreshToken getByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or invalid"));
    }
}
