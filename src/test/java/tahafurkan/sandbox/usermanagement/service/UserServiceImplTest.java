package tahafurkan.sandbox.usermanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.modelmapper.ModelMapper;
import tahafurkan.sandbox.usermanagement.dto.UserDto;
import tahafurkan.sandbox.usermanagement.entities.Address;
import tahafurkan.sandbox.usermanagement.entities.User;
import tahafurkan.sandbox.usermanagement.exception.NoSuchUserExistsException;
import tahafurkan.sandbox.usermanagement.exception.UsernameIsInUseException;
import tahafurkan.sandbox.usermanagement.exception.UsernameUnavailableException;
import tahafurkan.sandbox.usermanagement.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
class UserServiceImplTest {

    @Spy
    private UserRepository userRepository;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private UserServiceImpl userService;


    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void checkUsername_GivenUsernameObama_ThrowsException() {
        assertThrows(UsernameUnavailableException.class, () -> {
            String username = "obama";
            userService.checkUsername(username);
        });
    }

    @Test
    void checkUsername_GivenUsernameUppercaseObama_ThrowsException() {
        assertThrows(UsernameUnavailableException.class, () -> {
            String username = "Obama";
            userService.checkUsername(username);
        });
    }

    @Test
    void checkUsername_GivenUsernameAlreadyExistInDb_ThrowsException() {
        assertThrows(UsernameIsInUseException.class, () -> {
            String username = "taha.furkan";
            given(userRepository.existsByUsername(username)).willReturn(true);
            userService.checkUsername(username);
        });
    }

    @Test
    void checkUsername_GivenUsernameDoesNotExistInDb_DoNothing() {
        String username = "another-username";
        given(userRepository.existsByUsername(username)).willReturn(false);
        userService.checkUsername(username);
    }

    @Test
    void create_GivenProhibitedUsername_ThrowsException() {
        String username = "obama";
        List<Address> addressList = new ArrayList<>();
        addressList.add(new Address(1, "home", "New York", "New York"));
        addressList.add(new Address(2, "work", "Texas", "Texas"));

        User user = new User(1, "Barrack", "Obama", "barrack.obama@hotmail.com", username, addressList);

        assertThrows(UsernameUnavailableException.class, () -> {
            userService.create(user);
        });
    }

    @Test
    void create_GivenAllowedUsername_ReturnsSavedUser() {
        String username = "taha.furkan";
        List<Address> addressList = new ArrayList<>();
        addressList.add(new Address(1, "home", "Istanbul", "Turkey"));
        addressList.add(new Address(2, "work", "Istanbul", "Turkey"));
        User user = new User(1, "Taha", "Unsal", "taha.f.unsal@gmail.com", username, addressList);
        userService.create(user);
        Mockito.verify(userRepository, times(1)).save(user);
    }

    @Test
    void get_WithAnIdNotExist_ThrowsException() {
        assertThrows(NoSuchUserExistsException.class, () -> {
            int id = 1;
            User user = new User();
            user.setId(id);
            userService.get(id);
        });
    }

    @Test
    void get_WithAnIdExists_ReturnUser() {
        int id = 1;
        User user = new User();
        user.setId(id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
        UserDto userDto = userService.get(id);
        assertNotNull(userDto);
        assertEquals(1, userDto.getId());
    }

    @Test
    void getAll_ReturnsEmptyUserList() {
        List<UserDto> userList = userService.getAll();
        Mockito.verify(userRepository, times(1)).findAll();
        assertEquals(0, userList.size());
    }

    @Test
    void getAll_ReturnsPresentUserList() {
        List<User> records = new ArrayList<>();

        List<Address> addressList = new ArrayList<>();
        addressList.add(new Address(1, "home", "Istanbul", "Turkey"));
        User user_1 = new User(1, "Taha", "Unsal", "taha.f.unsal@gmail.com", "taha.furkan", addressList);
        User user_2 = new User(2, "Zehra", "Unsal", "zehra.unsal@gmail.com", "zehra.unsal", addressList);
        records.add(user_1);
        records.add(user_2);

        given(userRepository.findAll()).willReturn(records);
        List<UserDto> users = userService.getAll();
        Mockito.verify(userRepository, times(1)).findAll();
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    void delete_WithNonExistentId_ThrowsException() {
        assertThrows(NoSuchUserExistsException.class, () -> {
            int id = 1;
            User user = new User();
            user.setId(id);
            given(userRepository.findById(id)).willReturn(Optional.empty());
            userService.delete(id);

            verify(userRepository, never()).deleteById(id);
        });
    }

    @Test
    void delete_WithExistentId_DoNothing() {
        int id = 1;
        User user = new User();
        user.setId(id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
        userService.delete(id);
        verify(userRepository, times(1)).deleteById(id);
    }

    @Test
    void getByUsername_GivenUsernameExists_ReturnsUser() {
        String username = "taha.furkan";
        User user = new User();
        user.setUsername(username);
        given(userRepository.findByUsername(username)).willReturn(user);
        User user1 = userService.getByUsername(username);
        assertEquals(username, user1.getUsername());
    }

    @Test
    void getByUsername_GivenUsernameNonExists_DoNothing() {
        String username = "taha.furkan";
        User user = new User();
        user.setUsername(username);
        given(userRepository.findByUsername(username)).willReturn(null);
        User user_1 = userService.getByUsername(username);
        assertEquals(null, user_1);
    }

    @Test
    void update_UpdatesLastName_ReturnsUpdatedUser() {
        List<Address> addressList = new ArrayList<>();
        addressList.add(new Address(1, "home", "Istanbul", "Turkey"));
        User user = new User(1, "Taha", "Unsal", "taha.f.unsal@gmail.com", "taha.furkan", addressList);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

        UserDto updateUser = new UserDto();
        updateUser.setLastName("furkan");
        user.setLastName(updateUser.getLastName());

        given(userRepository.save(user)).willReturn(user);

        UserDto userDto = userService.update(user.getId(), updateUser);
        assertNotNull(userDto);
        assertEquals("furkan", userDto.getLastName());
    }
}


