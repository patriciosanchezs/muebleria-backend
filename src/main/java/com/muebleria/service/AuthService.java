package com.muebleria.service;

import com.muebleria.dto.AuthResponse;
import com.muebleria.dto.LoginRequest;
import com.muebleria.dto.RegisterRequest;
import com.muebleria.exception.BadRequestException;
import com.muebleria.model.Role;
import com.muebleria.model.User;
import com.muebleria.repository.UserRepository;
import com.muebleria.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El username ya está en uso");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.VENDEDOR)
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();
        
        userRepository.save(user);
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);
        
        List<String> locales = user.getLocalIds() != null 
                ? user.getLocalIds()
                : new ArrayList<>();
        
        List<String> subRoles = user.getSubRoles() != null
                ? user.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                : new ArrayList<>();
        
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole().name(), locales, subRoles);
    }
    
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername().toLowerCase(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);
        
        User user = userRepository.findByUsername(request.getUsername().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        
        List<String> locales = user.getLocalIds() != null 
                ? user.getLocalIds()
                : new ArrayList<>();
        
        List<String> subRoles = user.getSubRoles() != null
                ? user.getSubRoles().stream().map(Enum::name).collect(Collectors.toList())
                : new ArrayList<>();
        
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole().name(), locales, subRoles);
    }
}
