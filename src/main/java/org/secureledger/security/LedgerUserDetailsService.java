package org.secureledger.security;

import lombok.RequiredArgsConstructor;
import org.secureledger.dao.UserDAO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LedgerUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userDAO.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(AuthenticatedUser::withCredentials)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
