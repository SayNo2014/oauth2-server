package xhyg.oauth.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import xhyg.oauth.domain.SysUser;
import xhyg.oauth.repository.UserMapper;

@Service("userDetailsService")
public class Oauth2UserDetailsService implements UserDetailsService {

	@Autowired
	private UserMapper userMapper;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		SysUser user = userMapper.findByUserName(username);
		if (user == null) {
			throw new UsernameNotFoundException(username);
		}
		return User
				.withUsername(user.getUsername())
				.password(user.getPassword())
				.authorities(user.getAuthority())
				.roles("USER")
				.build();
	}

}
