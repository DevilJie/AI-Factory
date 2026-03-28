package com.aifactory.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.aifactory.common.CaptchaManager;
import com.aifactory.common.PasswordUtil;
import com.aifactory.common.TokenUtil;
import com.aifactory.dto.UserLoginDto;
import com.aifactory.dto.UserLoginResultVo;
import com.aifactory.dto.UserInfoDetailVo;
import com.aifactory.dto.UserRegisterDto;
import com.aifactory.dto.UserUpdateDto;
import com.aifactory.entity.User;
import com.aifactory.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserExtService userExtService;

    /**
     * 用户注册
     */
    public String register(UserRegisterDto registerDto) {
        // 检查登录名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLoginName, registerDto.getLoginName());
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new RuntimeException("登录名已存在");
        }

        // 检查手机号是否已存在
        if (StrUtil.isNotBlank(registerDto.getPhone())) {
            LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
            phoneQuery.eq(User::getPhone, registerDto.getPhone());
            Long phoneCount = userMapper.selectCount(phoneQuery);
            if (phoneCount > 0) {
                throw new RuntimeException("手机号已被注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setUserUid(UUID.randomUUID().toString(true));
        user.setLoginName(registerDto.getLoginName());
        user.setLoginPwd(PasswordUtil.hashPassword(registerDto.getPassword()));
        user.setActualName(registerDto.getActualName());
        user.setPhone(registerDto.getPhone());
        user.setEmail(registerDto.getEmail());
        user.setNickname(StrUtil.isNotBlank(registerDto.getNickname()) ? registerDto.getNickname() : registerDto.getActualName());
        user.setGender(0);
        user.setDisabledFlag(0);
        user.setDeletedFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 插入用户
        userMapper.insert(user);

        return "注册成功";
    }

    /**
     * 用户登录
     */
    public UserLoginResultVo login(UserLoginDto loginDto) {
        // 验证验证码
        if (!CaptchaManager.verifyCaptcha(loginDto.getCaptchaUuid(), loginDto.getCaptchaCode())) {
            throw new RuntimeException("验证码错误");
        }

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLoginName, loginDto.getLoginName());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("登录名或密码错误");
        }

        // 验证密码
        if (!PasswordUtil.verifyPassword(loginDto.getPassword(), user.getLoginPwd())) {
            throw new RuntimeException("登录名或密码错误");
        }

        // 检查账号状态
        if (user.getDisabledFlag() == 1) {
            throw new RuntimeException("账号已被禁用");
        }

        if (user.getDeletedFlag() == 1) {
            throw new RuntimeException("账号已被删除");
        }

        // 生成Token
        String token = TokenUtil.generateToken(user.getUserId(), user.getLoginName());

        // 返回结果
        UserLoginResultVo result = new UserLoginResultVo();
        result.setToken(token);
        result.setUserId(user.getUserId());
        result.setLoginName(user.getLoginName());
        result.setActualName(user.getActualName());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setGender(user.getGender());

        return result;
    }

    /**
     * 获取登录用户信息
     */
    public UserLoginResultVo getLoginInfo(String token) {
        Long userId = TokenUtil.getUserId(token);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserLoginResultVo result = new UserLoginResultVo();
        result.setUserId(user.getUserId());
        result.setLoginName(user.getLoginName());
        result.setActualName(user.getActualName());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setGender(user.getGender());

        return result;
    }

    /**
     * 根据用户ID获取登录用户信息
     */
    public UserLoginResultVo getLoginInfoById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserLoginResultVo result = new UserLoginResultVo();
        result.setUserId(user.getUserId());
        result.setLoginName(user.getLoginName());
        result.setActualName(user.getActualName());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setGender(user.getGender());

        return result;
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfo(String token, UserUpdateDto updateDto) {
        Long userId = TokenUtil.getUserId(token);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新字段
        if (updateDto.getNickname() != null) {
            user.setNickname(updateDto.getNickname());
        }
        if (updateDto.getPhone() != null) {
            user.setPhone(updateDto.getPhone());
        }
        if (updateDto.getEmail() != null) {
            user.setEmail(updateDto.getEmail());
        }

        userMapper.updateById(user);
    }

    /**
     * 根据用户ID更新用户信息
     */
    public void updateUserInfoById(Long userId, UserUpdateDto updateDto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新字段
        if (updateDto.getNickname() != null) {
            user.setNickname(updateDto.getNickname());
        }
        if (updateDto.getPhone() != null) {
            user.setPhone(updateDto.getPhone());
        }
        if (updateDto.getEmail() != null) {
            user.setEmail(updateDto.getEmail());
        }

        userMapper.updateById(user);
    }

    /**
     * 获取用户详细信息（包含扩展信息）
     */
    public UserInfoDetailVo getUserDetailInfo(String token) {
        Long userId = TokenUtil.getUserId(token);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 获取用户扩展信息
        com.aifactory.entity.UserExt userExt = userExtService.getUserExt(userId);

        // 组装返回结果
        UserInfoDetailVo result = new UserInfoDetailVo();
        result.setUserId(user.getUserId());
        result.setLoginName(user.getLoginName());
        result.setActualName(user.getActualName());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setGender(user.getGender());

        // 扩展信息
        result.setLanguage(userExt.getLanguage());
        result.setTheme(userExt.getTheme());
        result.setEmailNotification(userExt.getEmailNotification());
        result.setBrowserNotification(userExt.getBrowserNotification());
        result.setProjectNotification(userExt.getProjectNotification());
        result.setAutoSave(userExt.getAutoSave());
        result.setBio(userExt.getBio());

        return result;
    }

    /**
     * 根据用户ID获取用户详细信息（包含扩展信息）
     */
    public UserInfoDetailVo getUserDetailInfoById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 获取用户扩展信息
        com.aifactory.entity.UserExt userExt = userExtService.getUserExt(userId);

        // 组装返回结果
        UserInfoDetailVo result = new UserInfoDetailVo();
        result.setUserId(user.getUserId());
        result.setLoginName(user.getLoginName());
        result.setActualName(user.getActualName());
        result.setNickname(user.getNickname());
        result.setAvatar(user.getAvatar());
        result.setPhone(user.getPhone());
        result.setEmail(user.getEmail());
        result.setGender(user.getGender());

        // 扩展信息
        result.setLanguage(userExt.getLanguage());
        result.setTheme(userExt.getTheme());
        result.setEmailNotification(userExt.getEmailNotification());
        result.setBrowserNotification(userExt.getBrowserNotification());
        result.setProjectNotification(userExt.getProjectNotification());
        result.setAutoSave(userExt.getAutoSave());
        result.setBio(userExt.getBio());

        return result;
    }

    /**
     * 用户退出
     */
    public String logout() {
        // 如果使用Redis存储Token，这里需要删除Token
        // 当前使用JWT无状态认证，前端删除Token即可
        return "退出成功";
    }
}
