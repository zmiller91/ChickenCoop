package coop.security;

import coop.database.table.Pi;
import coop.database.table.PublicKey;
import coop.database.table.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class PiAuthenticationToken extends AbstractAuthenticationToken {

    private final Pi pi;

    public PiAuthenticationToken(Pi pi) {
        super(List.of(new SimpleGrantedAuthority("PI")));
        this.pi = pi;
    }

    @Override
    public PublicKey getCredentials() {
        return pi.getPublicKey();
    }

    @Override
    public Pi getPrincipal() {
        return pi;
    }

}