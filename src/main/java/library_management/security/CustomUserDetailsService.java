package library_management.security;

import library_management.models.User;
import library_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String mssv) throws UsernameNotFoundException {
        User user = userRepository.findByMssv(mssv)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy sinh viên có mã: " + mssv));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getMssv())
                .password(user.getPassword())
                .roles(user.getRole()) // Spring sẽ tự thêm tiền tố ROLE_
                .build();
    }
}