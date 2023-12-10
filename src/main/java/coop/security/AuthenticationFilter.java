package coop.security;

import com.google.common.collect.Streams;
import coop.database.repository.PiRepository;
import coop.database.repository.UserRepository;
import coop.database.table.Pi;
import coop.database.table.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.security.Security;
import java.util.Base64;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PiRepository piRepository;

    @Autowired
    private JwtTokenUtil jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    authorizeBearerToken(request, authHeader);
                }

                if (authHeader.startsWith("AsymmetricKey ")) {
                    authorizeAsymmetricKey(request, authHeader);
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

    private void authorizeAsymmetricKey(HttpServletRequest request, String authHeader) {

        try {

            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            String header = authHeader.replaceFirst("AsymmetricKey ", "");
            CMSSignedData signedPiId = new CMSSignedData(Base64.getDecoder().decode(header));
            String piId = new String((byte[]) signedPiId.getSignedContent().getContent());

            Pi pi = piRepository.findById(piId);
            PublicKey publicKey = pi.getPublicKey().getPublicKey();
            SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(publicKey);

            boolean valid = Streams.stream(signedPiId.getSignerInfos().iterator())
                    .allMatch(info -> verify(info, verifier));

            if (valid) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                PiAuthenticationToken token = new PiAuthenticationToken(pi);
                token.setAuthenticated(true);
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(token);
                SecurityContextHolder.setContext(context);
            }

        } catch (CMSException | OperatorCreationException e) {
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
