package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.Article;
import edu.ucsb.cs156.example.repositories.ArticleRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = ArticlesController.class)
@Import(TestConfig.class)
public class ArticlesControllerTests extends ControllerTestCase {
  @MockitoBean ArticleRepository articleRepository;

  @MockitoBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/articles/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/articles/all")).andExpect(status().is(200)); // logged
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_articles() throws Exception {

    Article article1 =
        Article.builder()
            .title("Test Article 1")
            .url("https://example.com/article1")
            .explanation("This is the first test article.")
            .email("email")
            .dateAdded(LocalDateTime.parse("2022-04-20T00:00:00"))
            .build();

    Article article2 =
        Article.builder()
            .title("Test Article 2")
            .url("https://example.com/article2")
            .explanation("This is the second test article.")
            .email("email")
            .dateAdded(LocalDateTime.parse("2022-04-20T00:00:00"))
            .build();

    ArrayList<Article> expectedArticles = new ArrayList<>();
    expectedArticles.addAll(Arrays.asList(article1, article2));

    when(articleRepository.findAll()).thenReturn(expectedArticles);

    // act
    MvcResult response =
        mockMvc.perform(get("/api/articles/all")).andExpect(status().isOk()).andReturn();

    // assert

    verify(articleRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedArticles);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_article() throws Exception {
    // arrange

    Article article1 =
        Article.builder()
            .title("Test Article 1")
            .url("https://example.com/article1")
            .explanation("This is the first test article.")
            .email("email")
            .dateAdded(LocalDateTime.parse("2022-04-20T00:00:00"))
            .build();

    when(articleRepository.save(any())).thenReturn(article1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/articles/post")
                    .param("title", "Test Article 1")
                    .param("url", "https://example.com/article1")
                    .param("explanation", "This is the first test article.")
                    .param("email", "email")
                    .param("dateAdded", "2022-04-20T00:00:00")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository, times(1)).save(articleCaptor.capture());

    Article savedArticle = articleCaptor.getValue();
    assertEquals("Test Article 1", savedArticle.getTitle());
    assertEquals("https://example.com/article1", savedArticle.getUrl());
    assertEquals("This is the first test article.", savedArticle.getExplanation());
    assertEquals("email", savedArticle.getEmail());
    assertEquals(LocalDateTime.parse("2022-04-20T00:00:00"), savedArticle.getDateAdded());

    String expectedJson = mapper.writeValueAsString(article1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/articles/post")
                .param("title", "Test Article 1")
                .param("url", "https://example.com/article1")
                .param("explanation", "This is the first test article.")
                .param("email", "email")
                .param("dateAdded", "2022-04-20T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/articles/post")
                .param("title", "Test Article 1")
                .param("url", "https://example.com/article1")
                .param("explanation", "This is the first test article.")
                .param("email", "email")
                .param("dateAdded", "2022-04-20T00:00:00")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/articles").param("id", "7"))
        .andExpect(status().is(403)); // logged out users can't get by id
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {

    // arrange

    Article article =
        Article.builder()
            .title("Test Article 1")
            .url("https://example.com/article1")
            .explanation("This is the first test article.")
            .email("email")
            .dateAdded(LocalDateTime.parse("2022-04-20T00:00:00"))
            .build();

    when(articleRepository.findById(eq(7L))).thenReturn(Optional.of(article));

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/articles").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(articleRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(article);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {

    // arrange

    when(articleRepository.findById(eq(7L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/articles").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert

    verify(articleRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("Article with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_article() throws Exception {
    // arrange

    Article articleOrig =
        Article.builder()
            .title("Test Article 1")
            .url("https://example.com/article1")
            .explanation("This is the first test article.")
            .email("email")
            .dateAdded(LocalDateTime.parse("2022-04-20T00:00:00"))
            .build();

    Article articleEdited =
        Article.builder()
            .title("Updated Article")
            .url("https://example.com/updated")
            .explanation("Updated explanation.")
            .email("updated@email.com")
            .dateAdded(LocalDateTime.parse("2023-04-20T00:00:00"))
            .build();

    String requestBody = mapper.writeValueAsString(articleEdited);

    when(articleRepository.findById(eq(67L))).thenReturn(Optional.of(articleOrig));
    when(articleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/articles")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
    verify(articleRepository, times(1)).save(articleCaptor.capture());

    Article savedArticle = articleCaptor.getValue();
    assertEquals("Updated Article", savedArticle.getTitle());
    assertEquals("https://example.com/updated", savedArticle.getUrl());
    assertEquals("Updated explanation.", savedArticle.getExplanation());
    assertEquals("updated@email.com", savedArticle.getEmail());
    assertEquals(LocalDateTime.parse("2023-04-20T00:00:00"), savedArticle.getDateAdded());
    String responseString = response.getResponse().getContentAsString();
    String expectedJson = mapper.writeValueAsString(savedArticle);
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_article_that_does_not_exist() throws Exception {
    // arrange

    Article articleDate =
        Article.builder()
            .title("Test Article 1")
            .url("https://example.com/article1")
            .explanation("This is the first test article.")
            .email("email")
            .dateAdded(LocalDateTime.parse("2022-04-20T00:00:00"))
            .build();

    String requestBody = mapper.writeValueAsString(articleDate);

    when(articleRepository.findById(eq(67L))).thenReturn(Optional.empty());

    // act
    MvcResult response =
        mockMvc
            .perform(
                put("/api/articles")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    // assert
    verify(articleRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("Article with id 67 not found", json.get("message"));
  }
}
