package com.app.webxulyand.service;

import com.app.webxulyand.domain.User;
import com.app.webxulyand.domain.response.PaginationDTO;
import com.app.webxulyand.domain.response.user.CreateUserDTO;
import com.app.webxulyand.domain.response.user.UpdateUserDTO;
import com.app.webxulyand.domain.response.user.UserDTO;
import com.app.webxulyand.mapper.UserMapper;
import com.app.webxulyand.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final UserMapper userMapper;

    public User create(User user) {
        //hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        return this.userRepository.save(user);
    }

    public boolean isExistedEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public boolean isExistedId(long id) {
        return this.userRepository.existsById(id);
    }

    public void delete(long id) {
        this.userRepository.deleteById(id);
    }

    public User getUserById(long id) {
        return this.userRepository.findById(id).orElse(null);
    }

    public User update(User reqUser) {
        User currentUser = this.getUserById(reqUser.getId());
        if (currentUser != null) {
            //currentUser.setEmail(reqUser.getEmail());
            currentUser.setName(reqUser.getName());
            currentUser.setAddress(reqUser.getAddress());
            currentUser.setPhone(reqUser.getPhone());
            currentUser.setAvatarUrl(reqUser.getAvatarUrl());
            currentUser.setStatus(reqUser.getStatus());
            currentUser.setPassword(passwordEncoder.encode(reqUser.getPassword()));
            currentUser = this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    public User getUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.getUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRFTokenAndEmail(String email, String token) {
        return this.userRepository.findByEmailAndRefreshToken(email, token);
    }

    public PaginationDTO fetchAllUser(Specification<User> specification, Pageable pageable) {
        Page<User> userPage = this.userRepository.findAll(pageable);

        PaginationDTO p = new PaginationDTO();
        PaginationDTO.Meta meta = new PaginationDTO.Meta();

        if(pageable.getPageNumber() == 0) {
            meta.setPage(pageable.getPageNumber() + 1);
        }
        else {
            meta.setPage(pageable.getPageNumber());
        }
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(userPage.getTotalPages());
        meta.setTotal(userPage.getTotalElements());

        p.setMeta(meta);
        System.out.println("Content: " + userPage.getContent());
        // remove sensitive data like password
        List<UserDTO> listUser = userPage.getContent()
                .stream().map(userMapper::toUserDTO)
                .collect(Collectors.toList());

        p.setResult(listUser);
        return p;
    }
}

