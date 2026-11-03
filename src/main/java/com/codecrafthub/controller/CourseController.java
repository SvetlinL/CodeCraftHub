package com.codecrafthub.controller;
import com.codecrafthub.model.*;
import com.codecrafthub.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
// Spring converts returned courses to JSON and binds incoming JSON to CourseRequest.
@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;
    public CourseController(CourseService service) { this.service = service; }
    @GetMapping
    public List<Course> list() throws IOException { return service.findAll(); }
    @GetMapping("/{id}")
    public Course get(@PathVariable Long id) throws IOException { return service.findById(id); }
    // @Valid rejects missing/blank fields before the service is called.
    @PostMapping
    public ResponseEntity<Course> create(@Valid @RequestBody CourseRequest request) throws IOException {
        Course course = service.create(request);
        return ResponseEntity.created(URI.create("/api/courses/" + course.id())).body(course);
    }
    // PUT replaces all editable fields. A missing course returns 404.
    @PutMapping("/{id}")
    public Course update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) throws IOException {
        return service.update(id, request);
    }
    // A successful DELETE returns HTTP 204 with no response body.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
