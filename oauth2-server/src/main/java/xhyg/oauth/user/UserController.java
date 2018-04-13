package xhyg.oauth.user;

import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@RequestMapping("/get_user_info")
	public @ResponseBody UserInfo getUserInfo(@RequestHeader("Authorization") String token) {
		Jwt jwt = JwtHelper.decode(token);
		System.out.println(jwt.getClaims());
		UserInfo userInfo = new UserInfo();
		userInfo.setUserName("xhyg");
		userInfo.setPhone("18973221341");
		return userInfo;
	}
	
}
