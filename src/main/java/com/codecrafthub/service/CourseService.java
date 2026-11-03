package com.codecrafthub.service;
import com.codecrafthub.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CourseService {
    private final ObjectMapper mapper;
    private final Path file;

    // Spring injects its configured Jackson mapper and the optional storage path.
    public CourseService(ObjectMapper mapper, @Value("${courses.file:courses.json}") String filename) {
        this.mapper = mapper;
        this.file = Path.of(filename).toAbsolutePath();
    }
    // Synchronization keeps each read-modify-write operation together in this app instance.
    public synchronized List<Course> findAll() throws IOException { return loadCourses(); }
    public synchronized Course findById(Long id) throws IOException {
        return loadCourses().stream().filter(c -> c.id().equals(id)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }
    public synchronized Course create(CourseRequest request) throws IOException {
        List<Course> courses = loadCourses();
        // Choose the next available number from the courses saved on disk.
        long id = Math.addExact(courses.stream().mapToLong(Course::id).max().orElse(0L), 1L);
        Course course = fromRequest(id, request, LocalDateTime.now().withNano(0));
        courses.add(course);
        saveCourses(courses);
        return course;
    }
    public synchronized Course update(Long id, CourseRequest request) throws IOException {
        List<Course> courses = loadCourses();
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).id().equals(id)) {
                // Updating a course preserves its original ID and creation time.
                Course course = fromRequest(id, request, courses.get(i).createdAt());
                courses.set(i, course);
                saveCourses(courses);
                return course;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
    }
    public synchronized void delete(Long id) throws IOException {
        List<Course> courses = loadCourses();
        if (!courses.removeIf(c -> c.id().equals(id)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        saveCourses(courses);
    }
    private Course fromRequest(Long id, CourseRequest r, LocalDateTime createdAt) {
        return new Course(id, r.name().trim(), r.description().trim(), r.targetDate(), r.status(), createdAt);
    }

    // Read on every operation so the JSON file remains the source of truth.
    private List<Course> loadCourses() throws IOException {
        Files.createDirectories(file.getParent());
        try {
            // CREATE_NEW initializes a missing file without overwriting existing data.
            Files.writeString(file, "[]\n", StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
            // Existing files are parsed below, including validation of malformed JSON.
        }
        List<Course> courses = mapper.readValue(file.toFile(), new TypeReference<List<Course>>() {});
        if (courses == null) throw new IOException("Course file must contain a JSON array");
        return courses;
    }

    // Write to a sibling temporary file first to protect the original during serialization.
    private void saveCourses(List<Course> courses) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), "courses-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), courses);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temporary); }
    }
}
