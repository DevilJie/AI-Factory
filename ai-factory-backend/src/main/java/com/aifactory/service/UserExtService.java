package com.aifactory.service;

import cn.hutool.core.util.StrUtil;
import com.aifactory.common.TokenUtil;
import com.aifactory.dto.UserExtDto;
import com.aifactory.entity.UserExt;
import com.aifactory.mapper.UserExtMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户扩展信息Service
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Service
public class UserExtService {

    @Autowired
    private UserExtMapper userExtMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取用户扩展信息
     */
    public UserExt getUserExt(Long userId) {
        LambdaQueryWrapper<UserExt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserExt::getUserId, userId);
        UserExt userExt = userExtMapper.selectOne(wrapper);

        // 如果不存在，创建默认配置
        if (userExt == null) {
            userExt = new UserExt();
            userExt.setUserId(userId);
            userExt.setLanguage("zh-CN");
            userExt.setTheme("dark");
            userExt.setEmailNotification(1);
            userExt.setBrowserNotification(1);
            userExt.setProjectNotification(1);
            userExt.setAutoSave(1);
            userExt.setBio("");
            userExt.setCreateTime(DATE_FORMATTER.format(LocalDateTime.now()));
            userExt.setUpdateTime(DATE_FORMATTER.format(LocalDateTime.now()));
            userExtMapper.insert(userExt);
        }

        return userExt;
    }

    /**
     * 更新用户扩展信息
     */
    public void updateUserExt(String token, UserExtDto dto) {
        Long userId = TokenUtil.getUserId(token);

        UserExt userExt = getUserExt(userId);

        if (StrUtil.isNotBlank(dto.getLanguage())) {
            userExt.setLanguage(dto.getLanguage());
        }
        if (StrUtil.isNotBlank(dto.getTheme())) {
            userExt.setTheme(dto.getTheme());
        }
        if (dto.getEmailNotification() != null) {
            userExt.setEmailNotification(dto.getEmailNotification());
        }
        if (dto.getBrowserNotification() != null) {
            userExt.setBrowserNotification(dto.getBrowserNotification());
        }
        if (dto.getProjectNotification() != null) {
            userExt.setProjectNotification(dto.getProjectNotification());
        }
        if (dto.getAutoSave() != null) {
            userExt.setAutoSave(dto.getAutoSave());
        }
        if (dto.getBio() != null) {
            userExt.setBio(dto.getBio());
        }

        userExt.setUpdateTime(DATE_FORMATTER.format(LocalDateTime.now()));
        userExtMapper.updateById(userExt);
    }
}
