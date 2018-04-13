package xhyg.oauth.repository;

import xhyg.oauth.domain.SysUser;

public interface UserMapper {
    public SysUser findByUserName(String username);
}
