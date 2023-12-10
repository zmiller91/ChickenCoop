package coop.security;

import coop.database.table.Pi;
import coop.database.table.User;
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

    public boolean isPiAuthenticationType() {
        return PiAuthenticationToken.class.equals(getAuthenticatedClass());
    }

    public Pi getCurrentPi() {
        if (!isPiAuthenticationType()) {
            return null;
        }

        return ((PiAuthenticationToken) getAuthentication()).getPrincipal();
    }




}
