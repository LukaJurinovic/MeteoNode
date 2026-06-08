package com.example.meteonode.service.domain;

import com.example.meteonode.exception.ConflictException;
import com.example.meteonode.exception.ResourceNotFoundException;
import com.example.meteonode.mapper.UserMapper;
import com.example.meteonode.model.dto.request.RegisterRequest;
import com.example.meteonode.model.dto.request.UpdateRoleRequest;
import com.example.meteonode.model.dto.response.UserDTO;
import com.example.meteonode.model.entity.User;
import com.example.meteonode.model.enums.Role;
import com.example.meteonode.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            var user = getByUsername(username);
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPasswordHash())
                    .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    .build();
        } catch (ResourceNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        return userMapper.toDTO(getByUsername(username));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> getAllUserIds() {
        return userRepository.findAllIds();
    }

    @Transactional
    public UserDTO createUser(RegisterRequest request, String encodedPassword) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered: " + request.email());
        }
        var user = userMapper.toEntity(request);
        user.setPasswordHash(encodedPassword);
        user.setRole(Role.USER);
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public void setUserRole(UpdateRoleRequest request) {
        getByUsername(request.username()).setRole(request.role());
    }

    @Transactional
    public void deleteUser(String username) {
        userRepository.delete(getByUsername(username));
    }

    private User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
