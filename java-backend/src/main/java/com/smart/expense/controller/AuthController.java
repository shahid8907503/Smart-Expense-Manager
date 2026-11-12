package com.smart.expense.controller;

import com.smart.expense.model.User;
import com.smart.expense.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> req) {
        String name = req.getOrDefault("name", "User");
        String email = req.getOrDefault("email", "");
        String password = req.getOrDefault("password", "");
        String lang = req.getOrDefault("preferredLanguage", "en");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(name, email, password, lang)));

        return buildAuthResponse(user, "Registration successful");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> req) {
        String email = req.getOrDefault("email", "demo@expense.com");
        String password = req.getOrDefault("password", "");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User("Smart Expense User", email, password, "en")));

        return buildAuthResponse(user, "Login successful");
    }

    @GetMapping("/profile")
    public Map<String, Object> profile() {
        User user = userRepository.findAll().stream().findFirst()
                .orElse(new User("Demo User", "demo@expense.com", "", "en"));
        return buildAuthResponse(user, "Profile loaded");
    }

    private Map<String, Object> buildAuthResponse(User user, String msg) {
        String rawToken = user.getId() + ":" + user.getEmail() + ":" + System.currentTimeMillis();
        String token = "java-jwt-" + Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> userData = new HashMap<>();
        userData.put("_id", String.valueOf(user.getId()));
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());
        userData.put("preferredLanguage", user.getPreferredLanguage());
        userData.put("token", token);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", msg);
        res.put("data", userData);
        return res;
    }
}

