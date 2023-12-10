//package coop.config.pi;
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//public class PiFilter extends OncePerRequestFilter {
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String path = request.getRequestURI();
//        if(path.startsWith("/pi")) {
//            filterChain.doFilter(new PiRequestWrapper(request), response);
//            return;
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}
