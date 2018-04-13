package xhyg.oauth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import xhyg.oauth.repository.UserMapper;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BizSsoDemoApplicationTests {

	@Autowired
	private UserMapper userMapper;

	@Test
	@Rollback
	public void testUserMapper() throws Exception {
		System.out.print(userMapper.findByUserName("admin"));
	}

}
