package coop.security;

import com.google.common.collect.Streams;
import coop.config.pi.PiRequestWrapper;
import coop.database.repository.PiRepository;
import coop.database.repository.UserRepository;
import coop.database.table.Pi;
import coop.database.table.User;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
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
import java.nio.charset.StandardCharsets;
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

        HttpServletRequest requestToFilter = request;

        try {

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
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
            }


            if (authHeader != null && authHeader.startsWith("AsymmetricKey ")) {
                requestToFilter = authorizeAsymmetricKey(request, authHeader);
            }

        } catch (JwtException e) {

        }

        filterChain.doFilter(requestToFilter, response);

    }

    private HttpServletRequest authorizeAsymmetricKey(HttpServletRequest request, String authHeader) {

        try {

            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            String piId = authHeader.replaceFirst("AsymmetricKey ", "");
            Pi pi = piRepository.findById(piId);
            PublicKey publicKey = pi.getPublicKey().getPublicKey();

            String encoded = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            byte[] data = Base64.getDecoder().decode(encoded);

            CMSSignedData signedData = new CMSSignedData(data);
            System.out.println(signedData.isDetachedSignature());
            SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(publicKey);

            boolean valid = Streams.stream(signedData.getSignerInfos().iterator())
                    .allMatch(info -> verify(info, verifier));

            // Because we read the request body above, we drained the request's input stream and we need to wrap it
            // with a new request so that subsequent calls to fetch the input stream dont throw exceptions. Additionally,
            // we need to transform the base64 encoded and signed data with the original data so that it can be mapped
            // appropriately in the controller
            byte[] newRequestBody = encoded.getBytes();
            if (valid) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                PiAuthenticationToken token = new PiAuthenticationToken(pi);
                token.setAuthenticated(true);
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(token);
                SecurityContextHolder.setContext(context);

                newRequestBody = (byte[])signedData.getSignedContent().getContent();
            }

            PiRequestWrapper requestWrapper = new PiRequestWrapper(request);
            requestWrapper.setData(newRequestBody);
            return requestWrapper;

        } catch (IOException | CMSException | OperatorCreationException e) {
            throw new RuntimeException(e);
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
