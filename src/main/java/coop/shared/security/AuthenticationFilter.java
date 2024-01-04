package coop.shared.security;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.UserRepository;
import coop.shared.database.table.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoopRepository piRepository;

    @Autowired
    private JwtTokenUtil jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    authorizeBearerToken(request, authHeader);
                }
            }

        filterChain.doFilter(request, response);

    }

    private void authorizeBearerToken(HttpServletRequest request, String authHeader) {
        try {

            String authToken = authHeader.replaceFirst("Bearer ", "");
            String userName = jwt.getUsernameFromToken(authToken);
            User user = userRepository.findByUsername(userName);
            if (jwt.validateToken(authToken, user)) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                UserAuthenticationToken token = UserAuthenticationToken.create(user);
                token.setAuthenticated(true);
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(token);
                SecurityContextHolder.setContext(context);
            }

        } catch (JwtException e) {
            //TODO: ...
        }
    }

    private static boolean verify(SignerInformation signerInfo, SignerInformationVerifier verifier) {
        try {
            return signerInfo.verify(verifier);
        } catch (CMSException e) {
            throw new RuntimeException(e);
        }
    }

}
