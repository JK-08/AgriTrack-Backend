package AgriTrackBackend.USERS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService service;

    // ✅ REGISTER
    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return service.register(user);
    }

    // ✅ LOGIN
    // ✅ LOGIN
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        return service.login(
                request.getUsername(),
                request.getPassword(),
                request.getRole()
        );
    }


    // ✅ GET ALL
    @GetMapping("/getall")
    public List<User> getAll() {
        return service.getAll();
    }

    // ✅ GET BY ID
    @GetMapping("/getbyid/{id}")
    public User getById(@PathVariable Long id) {
        return service.getById(id);
    }

    // ✅ DELETE
    @DeleteMapping("/deletebyid/{id}")
    public String delete(@PathVariable Long id) {
        service.deleteById(id);
        return "User deleted successfully";
    }
}