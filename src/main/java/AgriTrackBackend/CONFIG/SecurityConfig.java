//package AgriTrackBackend.CONFIG;
//
//import AgriTrackBackend.SECURITY.JwtFilter;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//public class SecurityConfig {
//
//    @Autowired
//    private JwtFilter jwtFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//                .csrf(csrf -> csrf.disable())
//
//                .authorizeHttpRequests(auth -> auth
//
//                        // ✅ PUBLIC APIs
//                        .requestMatchers(
//                                "/api/v1/user/register",
//                                "/api/v1/user/login",
//
//                                // ✅ ONBOARDING APIs
//                                "/api/v1/onboarding/getall",
//                                "/api/v1/onboarding/getbyid/**"
//                        ).permitAll()
//
//                        // ✅ OWNER APIs
//                        .requestMatchers("/api/v1/owner/**")
//                        .hasAuthority("OWNER")
//
//                        // ✅ DRIVER APIs
//                        .requestMatchers("/api/v1/driver/**")
//                        .hasAuthority("DRIVER")
//
//                        // ✅ CUSTOMER APIs
//                        .requestMatchers("/api/v1/customer/**")
//                        .hasAuthority("CUSTOMER")
//
//                        // 🔒 ALL OTHER APIs NEED TOKEN
//                        .anyRequest().authenticated()
//                )
//
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//}

package AgriTrackBackend.CONFIG;

import AgriTrackBackend.SECURITY.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // ✅ ALLOW ALL APIs
                        .anyRequest().permitAll()
                );

        // ✅ JWT filter optional now
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}