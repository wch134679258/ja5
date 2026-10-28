package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.AuthSupport;
import com.fenglin.springboottest.service.ProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口（需要登录）。
 *
 * <pre>
 * GET /api/user/profile
 *   -> { user: { id, name, joinedAt },
 *        stats: { favorites, history, likes },
 *        game: { best, rank, playCount } }
 * </pre>
 *
 * 一次请求拿齐个人页需要的全部统计，避免前端连发多次请求。
 */
@RestController
@RequestMapping("/api/user")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthSupport authSupport;

    public ProfileController(ProfileService profileService, AuthSupport authSupport) {
        this.profileService = profileService;
        this.authSupport = authSupport;
    }

    @GetMapping("/profile")
    public Object profile(@RequestHeader(value = "Authorization", required = false) String auth) {
        return profileService.profile(authSupport.requireUserId(auth));
    }
}
