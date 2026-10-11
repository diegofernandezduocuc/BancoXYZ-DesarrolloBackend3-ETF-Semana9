package cl.duoc.bancoxyz.pagos;
    import jakarta.servlet.DispatcherType;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.http.HttpMethod;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.web.SecurityFilterChain;
    @Configuration
    public class SecurityConfig {
      @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
              .requestMatchers(HttpMethod.GET,"/actuator/health").permitAll()
              .requestMatchers(HttpMethod.GET,"/api/**").hasAuthority("SCOPE_bank.read")
              .requestMatchers("/api/**").hasAuthority("SCOPE_bank.write")
              .anyRequest().denyAll())
            .oauth2ResourceServer(o->o.jwt(j->{}));
        return http.build();
      }
    }
