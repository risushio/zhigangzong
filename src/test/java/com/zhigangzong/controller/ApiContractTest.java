package com.zhigangzong.controller;

import com.zhigangzong.exception.GlobalExceptionHandler;
import com.zhigangzong.service.OrganizationService;
import com.zhigangzong.service.impl.MatchingServiceImpl;
import com.zhigangzong.mapper.MatchFeedbackMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiContractTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(
                new OrganizationController(mock(OrganizationService.class)),
                new MatchingController(new MatchingServiceImpl(mock(MatchFeedbackMapper.class))))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void invalidDtoReturnsUnified400() throws Exception {
        mvc.perform(post("/api/schools").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void malformedJsonReturnsUnified400() throws Exception {
        mvc.perform(post("/api/schools").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void plannedRecommendationDoesNotPretendSuccess() throws Exception {
        mvc.perform(get("/api/recommendations").param("studentId", "1"))
                .andExpect(status().isNotImplemented()).andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
    }

    @Test
    void missingRequiredParameterReturnsUnified400() throws Exception {
        mvc.perform(get("/api/recommendations"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }
}
