package com.elingo.user.controller;

import com.elingo.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Slf4j(topic = "USER-CONTROLLER")
@RequiredArgsConstructor
@Tag(name = "User Controller")
public class UserController {
    private final UserService userService;
}
