package com.pundix.wallet.task.controller;

import com.pundix.wallet.task.dto.UserCreateRequest;
import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.service.UserService;
import com.pundix.wallet.task.utils.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    public void testListUsers() {
        List<User> mockUsers = Arrays.asList(new User(1, "user1"), new User(2, "user2"));
        when(userService.getUsers()).thenReturn(mockUsers);

        ApiResponse response = userController.listUsers();

        assertEquals("查询成功", response.getMessage());
        assertEquals(mockUsers, response.getData());
    }

    @Test
    public void testCreateUser() {
        UserCreateRequest createUserRequest = new UserCreateRequest("John");
        User createdUser = new User(1, "John");
        when(userService.createUser(createUserRequest.getName())).thenReturn(createdUser);

        ApiResponse response = userController.createUser(createUserRequest);

        assertEquals("创建成功", response.getMessage());
        assertEquals(createdUser, response.getData());
    }
}
