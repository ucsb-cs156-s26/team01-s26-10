package edu.ucsb.cs156.example.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.ucsb.cs156.example.entities.UCSBDate;
import edu.ucsb.cs156.example.repositories.UCSBDateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** This is a REST controller for UCSBDates */
@Tag(name = "UCSBDates")
@RequestMapping("/api/ucsbdates")
@RestController
@Slf4j
public class UCSBDatesController extends ApiController {

  @Autowired UCSBDateRepository ucsbDateRepository;

  /**
   * List all UCSB dates
   *
   * @return an iterable of UCSBDate
   */
  @Operation(summary = "List all ucsb dates")
  @PreAuthorize("hasRole('ROLE_USER')")
  @GetMapping("/all")
  public Iterable<UCSBDate> allUCSBDates() {
    Iterable<UCSBDate> dates = ucsbDateRepository.findAll();
    return dates;
  }

  /**
   * Create a new date
   *
   * @param quarterYYYYQ the quarter in the format YYYYQ
   * @param name the name of the date
   * @param localDateTime the date
   * @return the saved ucsbdate
   */
  @Operation(summary = "Create a new date")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  @PostMapping("/post")
  public UCSBDate postUCSBDate(
      @Parameter(name = "quarterYYYYQ") @RequestParam String quarterYYYYQ,
      @Parameter(name = "name") @RequestParam String name,
      @Parameter(
              name = "localDateTime",
              description =
                  "date (in iso format, e.g. YYYY-mm-ddTHH:MM:SS; see https://en.wikipedia.org/wiki/ISO_8601)")
          @RequestParam("localDateTime")
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime localDateTime)
      throws JsonProcessingException {

    // For an explanation of @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    // See: https://www.baeldung.com/spring-date-parameters

    log.info("localDateTime={}", localDateTime);

    UCSBDate ucsbDate = new UCSBDate();
    ucsbDate.setQuarterYYYYQ(quarterYYYYQ);
    ucsbDate.setName(name);
    ucsbDate.setLocalDateTime(localDateTime);

    UCSBDate savedUcsbDate = ucsbDateRepository.save(ucsbDate);

    return savedUcsbDate;
  }
}
