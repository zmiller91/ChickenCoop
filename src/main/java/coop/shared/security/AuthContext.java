package coop.shared.security;

import coop.shared.database.table.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthContext {


    public boolean isRequestAuthenticated() {
        return getAuthentication().isAuthenticated();
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public Class<? extends Authentication> getAuthenticatedClass() {
        return SecurityContextHolder.getContext().getAuthentication().getClass();
    }

    public boolean isUserAuthenticationType() {
        return UserAuthenticationToken.class.equals(getAuthenticatedClass());
    }

    public User getCurrentUser() {
        if (!isUserAuthenticationType()) {
            return null;
        }

        return ((UserAuthenticationToken) getAuthentication()).getPrincipal();
    }




}
