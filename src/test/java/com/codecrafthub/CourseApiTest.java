package com.codecrafthub;

import com.codecrafthub.service.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CourseApiTest {
    static final Path file;
    static {
        try { file = Files.createTempDirectory("codecrafthub-test-").resolve("courses.json"); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) { registry.add("courses.file", file::toString); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Test void crudPersistsAndValidates() throws Exception {
        // The first read automatically creates a missing courses.json file.
        assertFalse(Files.exists(file));
        mvc.perform(get("/api/courses")).andExpect(status().isOk()).andExpect(content().json("[]"));
        assertTrue(Files.exists(file));
        String body = """
            {"name":"Java", "description":"Learn REST", "target_date":"2026-12-31", "status":"Not Started"}
            """;
        String json = mvc.perform(post("/api/courses").contentType("application/json").content(body))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(json).get("id").asLong();
        assertEquals(1L, id);
        String createdAt = mapper.readTree(json).get("created_at").asText();
        assertTrue(createdAt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
        assertEquals("2026-12-31", mapper.readTree(json).get("target_date").asText());
        // A fresh service reads the saved data from disk, not in-memory state.
        assertEquals(1, new CourseService(mapper, file.toString()).findAll().size());
        mvc.perform(get("/api/courses")).andExpect(jsonPath("$[0].id").value((int) id));
        mvc.perform(get("/api/courses/" + id)).andExpect(status().isOk());
        mvc.perform(put("/api/courses/" + id).contentType("application/json")
            .content(body.replace("Not Started", "Completed")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Completed"))
            .andExpect(jsonPath("$.created_at").value(createdAt));
        mvc.perform(post("/api/courses").contentType("application/json").content(body.replace("Java", " ")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").contentType("application/json").content(body.replace("Not Started", "INVALID")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").contentType("application/json").content(body.replace("2026-12-31", "2026-02-30")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").contentType("application/json").content(body.replace("Not Started", "not started")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/courses").contentType("application/json").content(body.replace("\"Not Started\"", "0")))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/courses/999").contentType("application/json").content(body))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/courses/999")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/courses/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/courses/" + id)).andExpect(status().isNotFound());
        assertTrue(new CourseService(mapper, file.toString()).findAll().isEmpty());
        Files.writeString(file, "broken json");
        mvc.perform(post("/api/courses").contentType("application/json").content(body))
            .andExpect(status().isInternalServerError());
        assertEquals("broken json", Files.readString(file));
    }
}
