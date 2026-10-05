package hu.konzultacio.controller;

import hu.konzultacio.dto.Dtos.TeacherView;
import hu.konzultacio.service.TeacherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {
    private final TeacherService teachers;

    public TeacherController(TeacherService teachers) { this.teachers = teachers; }

    @GetMapping
    public List<TeacherView> list() { return teachers.list(); }
}
