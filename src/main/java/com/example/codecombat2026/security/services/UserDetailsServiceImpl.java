package com.example.codecombat2026.security.services;

import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Check if user is enabled
        if (user.getEnabled() != null && !user.getEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("User account is disabled");
        }

        return UserDetailsImpl.build(user);
    }
}
