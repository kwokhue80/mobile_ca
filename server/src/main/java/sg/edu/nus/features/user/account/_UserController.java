package sg.edu.nus.features.user.account;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.features.user.account.dto._UserResponse;

/*
*   AUTHOR: Amelia
*   PURPOSE: User controller for client to access API on user profile changes/logout
*/
// @RestController
// @RequestMapping("/api/user")
@RequiredArgsConstructor
@Deprecated
public class _UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/profile")
    public ResponseEntity<_UserResponse> profile(Authentication authentication) {
        User user = userService.getByEmail(authentication.getName());
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

}
