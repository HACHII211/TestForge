import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.security.JwtUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    @Test
    void generateAndVerifyToken_shouldContainUserIdAndRoles_withLogging() {
        JwtUtil jwtUtil = new JwtUtil("test-secret-123", 24L * 3600 * 1000); // 24h

        Long userId = 42L;
        List<Long> roleIds = List.of(1L, 2L);

        String token = jwtUtil.generateToken(userId, roleIds);
        assertNotNull(token);

        System.out.println("=== JWT GENERATED ===");
        System.out.println(token);
        System.out.println("======================");

        // decode & verify
        DecodedJWT decoded = jwtUtil.verifyToken(token);
        assertNotNull(decoded);

        // Print header, payload timestamps and raw claims
        System.out.println("Header (Base64 JSON): " + decoded.getHeader());
        System.out.println("IssuedAt: " + decoded.getIssuedAt());
        System.out.println("ExpiresAt: " + decoded.getExpiresAt());

        Map<String, Claim> claims = decoded.getClaims();
        System.out.println("Raw Claims:");
        claims.forEach((k, v) -> {
            try {
                // 尝试以多种方式展示
                String asString = v.asString();
                System.out.println("  " + k + " -> asString(): " + asString);
            } catch (Exception ignored) {}
            try {
                System.out.println("  " + k + " -> asLong(): " + v.asLong());
            } catch (Exception ignored) {}
            try {
                System.out.println("  " + k + " -> asList(String.class): " + v.asList(String.class));
            } catch (Exception ignored) {}
            try {
                System.out.println("  " + k + " -> asList(Long.class): " + v.asList(Long.class));
            } catch (Exception ignored) {}
        });

        // 使用 JwtUtil 提取
        Long extractedUserId = jwtUtil.getUserId(decoded);
        List<Long> extractedRoles = jwtUtil.getRoleIds(decoded);

        System.out.println("Extracted by JwtUtil:");
        System.out.println("  userId = " + extractedUserId);
        System.out.println("  roles  = " + extractedRoles);

        assertEquals(userId, extractedUserId);
        assertEquals(roleIds.size(), extractedRoles.size());
        assertTrue(extractedRoles.containsAll(roleIds));
    }

    @Test
    void expiredToken_shouldThrow_withLogging() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil("s", 1000); // 1s
        String token = jwtUtil.generateToken(1L, List.of(1L));
        assertNotNull(token);

        System.out.println("Generated short-lived token: " + token);
        Thread.sleep(2000); // wait to expire

        System.out.println("Verifying after sleep (should throw TokenExpiredException)...");
        assertThrows(com.auth0.jwt.exceptions.TokenExpiredException.class, () -> jwtUtil.verifyToken(token));
        System.out.println("expired test: exception thrown as expected");
    }
}
