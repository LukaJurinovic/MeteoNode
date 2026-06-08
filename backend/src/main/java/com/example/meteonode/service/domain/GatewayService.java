package com.example.meteonode.service.domain;

import com.example.meteonode.model.entity.Gateway;
import com.example.meteonode.repository.GatewayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GatewayService {

    private final GatewayRepository gatewayRepository;

    @Transactional(readOnly = true)
    public Optional<Gateway> findByApiKey(String apiKey) {
        return gatewayRepository.findByApiKey(apiKey);
    }

    @Transactional
    public void updateUrl(Integer id, String url) {
        gatewayRepository.findById(id).ifPresent(g -> g.setGatewayUrl(url));
    }

}
