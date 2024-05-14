package com.pundix.wallet.task.service;

import com.pundix.wallet.task.entity.User;
import com.pundix.wallet.task.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    public void testGetUsers() {
        List<User> mockUsers = Arrays.asList(new User(1, "user1"), new User(2, "user2"));
        when(userRepository.findAll()).thenReturn(mockUsers);

        List<User> users = userService.getUsers();

        assertEquals(mockUsers.size(), users.size());
        assertEquals(mockUsers.get(0), users.get(0));
        assertEquals(mockUsers.get(1), users.get(1));
    }

    @Test
    public void testCreateUser() {
        String userName = "John";
        when(userRepository.findByName(userName)).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1);
            return savedUser;
        });

        User createdUser = userService.createUser(userName);

        assertNotNull(createdUser);
        assertEquals(userName, createdUser.getName());
    }
}
