// JwtUtil.java
package com.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES = "roles";
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expireMillis;

    public JwtUtil(String secret, long expireMillis) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
        this.expireMillis = expireMillis;
    }

    public String generateToken(Long userId, List<Long> roleIds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMillis);
        return JWT.create()
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .withClaim(CLAIM_USER_ID, userId)
                .withArrayClaim(CLAIM_ROLES, roleIds.stream().map(String::valueOf).toArray(String[]::new))
                .sign(algorithm);
    }

    public DecodedJWT verifyToken(String token) {
        return verifier.verify(token);
    }

    public Long getUserId(DecodedJWT jwt) {
        // if stored as number
        if (jwt.getClaim(CLAIM_USER_ID).isNull()) return null;
        try { return jwt.getClaim(CLAIM_USER_ID).asLong(); } catch (Exception e) {
            // fallback to string
            String s = jwt.getClaim(CLAIM_USER_ID).asString();
            return s == null ? null : Long.valueOf(s);
        }
    }

    public List<Long> getRoleIds(DecodedJWT jwt) {
        try {
            // we stored as array of strings
            List<String> list = jwt.getClaim(CLAIM_ROLES).asList(String.class);
            return list == null ? List.of() : list.stream().map(Long::valueOf).collect(Collectors.toList());
        } catch (Exception e) {
            try {
                return jwt.getClaim(CLAIM_ROLES).asList(Long.class);
            } catch (Exception ex) {
                return List.of();
            }
        }
    }
}
