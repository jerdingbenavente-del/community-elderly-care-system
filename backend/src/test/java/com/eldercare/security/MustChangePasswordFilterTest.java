package com.eldercare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MustChangePasswordFilterTest {

    private final MustChangePasswordFilter filter = new MustChangePasswordFilter(new ObjectMapper());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void loginMustChange() {
        LoginUser user = new LoginUser(9L, "zs@12345", "x", true, true,
                List.of("FAMILY"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void allowChangePassword() throws Exception {
        loginMustChange();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/system/users/change-password");
        request.setServletPath("/api/system/users/change-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void allowMe() throws Exception {
        loginMustChange();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/system/users/me");
        request.setServletPath("/api/system/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void blockBusinessApi() throws Exception {
        loginMustChange();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/family/elders");
        request.setServletPath("/api/family/elders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class));
        assertEquals(403, response.getStatus());
        assertTrueContains(response.getContentAsString(), "首次登录请先修改密码");
    }

    private void assertTrueContains(String body, String part) {
        if (body == null || !body.contains(part)) {
            throw new AssertionError("expected body to contain: " + part + ", actual=" + body);
        }
    }
}
