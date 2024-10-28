package com.app.webxulyand.controller;


import com.app.webxulyand.domain.User;
import com.app.webxulyand.domain.request.EmailRequestDTO;
import com.app.webxulyand.domain.request.LoginDTO;
import com.app.webxulyand.domain.response.user.CreateUserDTO;
import com.app.webxulyand.domain.response.user.ResLoginDTO;
import com.app.webxulyand.mapper.UserMapper;
import com.app.webxulyand.service.*;
import com.app.webxulyand.util.SecurityUtil;
import com.app.webxulyand.util.annotation.ApiMessage;

import com.app.webxulyand.util.exception.ResourceInvalidException;
import jakarta.security.auth.message.AuthException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v2")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    @Autowired
    private final UserMapper userMapper;

    @Value("${jwt.refreshtoken-validity-in-seconds}")
    private long refreshTokenExpiration;


    public AuthController(AuthenticationManager authenticationManager, SecurityUtil securityUtil, UserService userService, UserMapper userMapper) {
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("auth/login")
    @ApiMessage("Login")
    public ResponseEntity<ResLoginDTO> login(@RequestBody LoginDTO loginDTO) throws AuthException {
        User currentUserDB = this.userService.getUserByUsername(loginDTO.getEmail());

        if (currentUserDB != null && currentUserDB.getStatus() == 0) {
           throw new AuthException("Tài khoản của bạn đã bị khóa. Không thể đăng nhập");
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        ResLoginDTO res = new ResLoginDTO();

        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole());
            res.setUser(userLogin);
        }

        String accessToken = this.securityUtil.createAccessToken(authentication.getName(), res);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        res.setAccessToken(accessToken);
        String refresh_token = this.securityUtil.createRefreshToken(loginDTO.getEmail(), res);
        this.userService.updateUserToken(refresh_token, loginDTO.getEmail());
        ResponseCookie responseCookie = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString()).body(res);
    }

    @GetMapping("auth/account")
    @ApiMessage("Get user")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() throws AuthException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        // Lấy thông tin người dùng trong db
        User currentUserDB = this.userService.getUserByUsername(email);
        if (currentUserDB != null && currentUserDB.getStatus() == 0) {
            throw new AuthException("Tài khoản của bạn đã bị khóa. Không thể đăng nhập");
        }

        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();

        if (currentUserDB != null) {
            userLogin.setId(currentUserDB.getId());
            userLogin.setEmail(currentUserDB.getEmail());
            userLogin.setName(currentUserDB.getName());
            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok(userGetAccount);
    }

    @GetMapping("auth/refresh")
    @ApiMessage("Get new token")
    public ResponseEntity<ResLoginDTO> getNewRefreshToken(@CookieValue(name = "refresh_token", defaultValue = "none") String refreshToken) throws ResourceInvalidException, AuthException {
        if (refreshToken.equals("none")) {
            throw new ResourceInvalidException("Vui lòng đăng nhập");
        }

        // Check RFtoken hợp lệ
        Jwt decodedToken = this.securityUtil.checkValidToken(refreshToken);
        String email = decodedToken.getSubject();
        User currentUser = this.userService.getUserByRFTokenAndEmail(email, refreshToken);
        if (currentUser == null) {
            throw new ResourceInvalidException("Refresh token không hợp lệ");
        }
        else {
            if (currentUser.getStatus() == 0){
                throw new AuthException("Tài khoản bị khóa");
            }
        }

        // Tạo lại RF token và set cookies
        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.getUserByUsername(email);
        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole());
            res.setUser(userLogin);
        }

        // create access token
        String access_token = this.securityUtil.createAccessToken(email, res);
        res.setAccessToken(access_token);

        // create refresh token
        String new_refresh_token = this.securityUtil.createRefreshToken(email, res);

        // update user
        this.userService.updateUserToken(new_refresh_token, email);

        // set cookies
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("auth/logout")
    @ApiMessage("Logout")
    public ResponseEntity<Void> logout() throws ResourceInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        if (email.isEmpty()) {
            throw new ResourceInvalidException("Accesstoken không hợp lệ");
        }

        this.userService.updateUserToken(null, email);

        //Xóa cookie
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString()).body(null);
    }

    @PostMapping("auth/register")
    @ApiMessage("Register a user")
    public ResponseEntity<CreateUserDTO> register(@Valid @RequestBody User user) throws ResourceInvalidException {
        if (this.userService.isExistedEmail(user.getEmail())) {
            throw new ResourceInvalidException("Email " + user.getEmail() + " đã tồn tại");
        }

        User newUser = this.userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toCreateUserDTO(newUser));
    }

    @PostMapping("auth/forgot")
    @ApiMessage("Forgot password")
    public ResponseEntity<Void> forgotPassword(@RequestBody EmailRequestDTO emailRequest) throws ResourceInvalidException {
        if (!this.userService.isExistedEmail(emailRequest.getEmail())) {
            throw new ResourceInvalidException("Email " + emailRequest.getEmail() + " không tồn tại");
        }
        String uuid = String.valueOf(UUID.randomUUID());
        String token = this.securityUtil.createResetPasswordToken(emailRequest.getEmail(), uuid);
        return ResponseEntity.ok(null);
    }
}
