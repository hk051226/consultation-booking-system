package hu.konzultacio.service;

import hu.konzultacio.domain.User;
import hu.konzultacio.dto.Dtos.TeacherView;
import hu.konzultacio.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TeacherService {
    private final UserRepository users;

    public TeacherService(UserRepository users) { this.users = users; }

    /** Csak id és név: az e-mail cím nem publikus. */
    @Transactional(readOnly = true)
    public List<TeacherView> list() {
        return users.findByRoleOrderByFullName(User.Role.TEACHER).stream()
                .map(u -> new TeacherView(u.id, u.fullName)).toList();
    }
}
