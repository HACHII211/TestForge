//package com.example.config;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.web.servlet.HandlerInterceptor;
//import com.auth0.jwt.interfaces.Claim;
//import com.example.utils.JwtUtil;
//
//import org.springframework.http.HttpStatus;
//
//import java.util.Map;
//
//public class JwtInterceptor implements HandlerInterceptor {
//
//    @Override
//    public boolean preHandle(HttpServletRequest request,
//                             HttpServletResponse response,
//                             Object handler) throws Exception {
//        String token = request.getHeader("Authorization");
//
//        if (token != null && token.startsWith("Bearer ")) {
//            String jwtToken = token.substring(7);
//            Map<String, Claim> claims = JwtUtil.verifyToken(jwtToken);
//            if (claims != null && !claims.isEmpty()) {
//                request.setAttribute("role", claims.get("role").asString());
//                return true;
//            } else {
//                // Token 过期或无效
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
//                response.setContentType("application/json;charset=UTF-8");
//                response.getWriter().write("{\"code\":401,\"message\":\"登录状态失效，请重新登录\"}");
//                return false;
//            }
//        } else {
//            // 没有 Token
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
//            response.setContentType("application/json;charset=UTF-8");
//            response.getWriter().write("{\"code\":401,\"message\":\"未登录或缺少Token\"}");
//            return false;
//        }
//    }
//}
