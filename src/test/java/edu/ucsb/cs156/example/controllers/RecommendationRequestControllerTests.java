package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = RecommendationRequestController.class)
@Import(TestConfig.class)
public class RecommendationRequestControllerTests extends ControllerTestCase {

  @MockitoBean RecommendationRequestRepository recommendationRequestRepository;

  @MockitoBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/recommendationrequest/all")).andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/recommendationrequest/all")).andExpect(status().is(200));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendationrequest/post")
                .param("requesterEmail", "cgaucho@ucsb.edu")
                .param("professorEmail", "phtcon@ucsb.edu")
                .param("explanation", "BS/MS program")
                .param("dateRequested", "2022-04-20T00:00:00")
                .param("dateNeeded", "2022-05-01T00:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendationrequest/post")
                .param("requesterEmail", "cgaucho@ucsb.edu")
                .param("professorEmail", "phtcon@ucsb.edu")
                .param("explanation", "BS/MS program")
                .param("dateRequested", "2022-04-20T00:00:00")
                .param("dateNeeded", "2022-05-01T00:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_recommendation_requests() throws Exception {
    LocalDateTime requested1 = LocalDateTime.parse("2022-04-20T00:00:00");
    LocalDateTime needed1 = LocalDateTime.parse("2022-05-01T00:00:00");
    LocalDateTime requested2 = LocalDateTime.parse("2022-05-20T00:00:00");
    LocalDateTime needed2 = LocalDateTime.parse("2022-11-15T00:00:00");

    RecommendationRequest request1 =
        RecommendationRequest.builder()
            .id(1L)
            .requesterEmail("cgaucho@ucsb.edu")
            .professorEmail("phtcon@ucsb.edu")
            .explanation("BS/MS program")
            .dateRequested(requested1)
            .dateNeeded(needed1)
            .done(false)
            .build();

    RecommendationRequest request2 =
        RecommendationRequest.builder()
            .id(2L)
            .requesterEmail("ldelplaya@ucsb.edu")
            .professorEmail("richert@ucsb.edu")
            .explanation("PhD CS Stanford")
            .dateRequested(requested2)
            .dateNeeded(needed2)
            .done(false)
            .build();

    ArrayList<RecommendationRequest> expectedRequests = new ArrayList<>();
    expectedRequests.addAll(Arrays.asList(request1, request2));

    when(recommendationRequestRepository.findAll()).thenReturn(expectedRequests);

    MvcResult response =
        mockMvc
            .perform(get("/api/recommendationrequest/all"))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendation_request() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2022-04-20T00:00:00");
    LocalDateTime needed = LocalDateTime.parse("2022-05-01T00:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("cgaucho@ucsb.edu")
            .professorEmail("phtcon@ucsb.edu")
            .explanation("BS/MS program")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(false)
            .build();

    when(recommendationRequestRepository.save(eq(recommendationRequest)))
        .thenReturn(recommendationRequest);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequest/post")
                    .param("requesterEmail", "cgaucho@ucsb.edu")
                    .param("professorEmail", "phtcon@ucsb.edu")
                    .param("explanation", "BS/MS program")
                    .param("dateRequested", "2022-04-20T00:00:00")
                    .param("dateNeeded", "2022-05-01T00:00:00")
                    .param("done", "false")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).save(recommendationRequest);
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendation_request_marked_done() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2022-05-20T00:00:00");
    LocalDateTime needed = LocalDateTime.parse("2022-11-15T00:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("ldelplaya@ucsb.edu")
            .professorEmail("richert@ucsb.edu")
            .explanation("PhD CS Stanford")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(true)
            .build();

    when(recommendationRequestRepository.save(eq(recommendationRequest)))
        .thenReturn(recommendationRequest);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/recommendationrequest/post")
                    .param("requesterEmail", "ldelplaya@ucsb.edu")
                    .param("professorEmail", "richert@ucsb.edu")
                    .param("explanation", "PhD CS Stanford")
                    .param("dateRequested", "2022-05-20T00:00:00")
                    .param("dateNeeded", "2022-11-15T00:00:00")
                    .param("done", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).save(recommendationRequest);
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
