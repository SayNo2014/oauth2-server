package xhyg.oauth.user;

/**
 * 用户信息
 * @author zhaidehui
 * @since 2018年03月31日
 */
public class UserInfo {

	/**
	 * 用户名
	 */
	private String userName;

    /**
     * 用户手机号
     */
    private String phone;

    public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

}
