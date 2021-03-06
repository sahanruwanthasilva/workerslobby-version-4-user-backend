package com.sahan.workerslobby.Utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.sahan.workerslobby.Constants.SecurityConstants.*;
import static java.util.Arrays.stream;

@Component
public class JWTTokenProvider
{
    @Value("${jwt.secret}")
    private String secret;

    /* Generate Jwt Token */
    public String generateJwtToken(UserPrincipal userPrincipal)
    {
        String[] claims = getClaimsFromUser(userPrincipal);
        return JWT.create()
                .withIssuer(SAHAN_SYSTEMS)
                .withAudience(SAHAN_SYSTEMS_ADMINISTRATION)
                .withIssuedAt(new Date())
                .withSubject(userPrincipal.getUsername())
                .withArrayClaim(AUTHORITIES, claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC512(secret.getBytes()));
    }

    /* get authorities of the user from the token */
    public List<GrantedAuthority> getAuthorities(String token)
    {
        String[] claims = getClaimsFromToken(token);
        return stream(claims).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    /* set authorities to the user in application scope (spring security context)  */
    public Authentication getAuthentication(String username, List<GrantedAuthority> authorities, HttpServletRequest request)
    {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new
                UsernamePasswordAuthenticationToken(username, null, authorities);
        usernamePasswordAuthenticationToken
                .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return usernamePasswordAuthenticationToken;
    }

    /* check the token is valid or not */
    public boolean isTokenValid(String username, String token)
    {
        JWTVerifier verifier = getJWTVerifier();
        return StringUtils.isNotEmpty(username) && !isTokenExpired(verifier, token);
    }

    /* get the username(subject) from the token */
    public String getSubject(String token)
    {
        JWTVerifier verifier = getJWTVerifier();
        return verifier.verify(token).getSubject();
    }

    /* check if the token expired or not*/
    private boolean isTokenExpired(JWTVerifier verifier, String token)
    {
        Date expiration = verifier.verify(token).getExpiresAt();
        return expiration.before(new Date());
    }


    /* get claims from the token */
    private String[] getClaimsFromToken(String token)
    {
        JWTVerifier verifier = getJWTVerifier();
        return verifier.verify(token).getClaim(AUTHORITIES).asArray(String.class);
    }

    /* verify jwt token */
    private JWTVerifier getJWTVerifier()
    {
        JWTVerifier verifier;
        try {
            Algorithm algorithm = Algorithm.HMAC512(secret);
            verifier = JWT.require(algorithm).withIssuer(SAHAN_SYSTEMS).build();
        }catch (JWTVerificationException exception)
        {
            throw new JWTVerificationException(TOKEN_CANNOT_BE_VERIFIED);
        }
        return verifier;
    }


    /* get Claims from userPrincipal*/
    private String[] getClaimsFromUser(UserPrincipal user)
    {
        List<String> authorities= new ArrayList<>();
        for (GrantedAuthority grantedAuthority : user.getAuthorities())
        {
            authorities.add(grantedAuthority.getAuthority());
        }
        return authorities.toArray(new String[0]);
    }
}
