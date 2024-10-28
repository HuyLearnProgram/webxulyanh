package com.app.webxulyand.mapper;

import com.app.webxulyand.domain.User;
import com.app.webxulyand.domain.response.user.CreateUserDTO;
import com.app.webxulyand.domain.response.user.UpdateUserDTO;
import com.app.webxulyand.domain.response.user.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;



@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(CreateUserDTO request);

    CreateUserDTO toCreateUserDTO(User user);

    UserDTO toUserDTO(User user);

    UpdateUserDTO toUpdateUserDTO(User user);

}