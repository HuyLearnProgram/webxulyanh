package com.app.webxulyand.domain.response.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserDTO {
    long id;

    String name;

    String email;

    int status;

}
