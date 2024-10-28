package com.app.webxulyand.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Entity
@Table(name = "users")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @NotBlank(message = "Tên không được để trống")
    String name;

    @NotBlank(message = "Không được để trống email")
    @Email(message = "Email không đúng định dạng")
    @Column(unique = true)
    String email;

    @NotBlank(message = "Không được để trống password")
    String password;

    int status;

    String phone;

    @Column(columnDefinition = "MEDIUMTEXT")
    String address;

    String avatarUrl;

    @Column(columnDefinition = "MEDIUMTEXT")
    String refreshToken;

    @ManyToOne
    @JoinColumn(name = "role_id")
    Role role;
}
