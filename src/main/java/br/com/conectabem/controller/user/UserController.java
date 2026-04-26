package br.com.conectabem.controller.user;

import br.com.conectabem.dto.user.UpdateProfileRequest;
import br.com.conectabem.dto.user.UserProfileResponse;
import br.com.conectabem.service.CurrentUserService;
import br.com.conectabem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @GetMapping("/profile")
    public UserProfileResponse getProfile() {
        var userId = currentUserService.requireUserId();
        var user = userService.findById(userId);
        return UserProfileResponse.from(user);
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileRequest request) {
        var userId = currentUserService.requireUserId();
        userService.updateProfile(userId, request);
        return ResponseEntity.noContent().build();
    }
}