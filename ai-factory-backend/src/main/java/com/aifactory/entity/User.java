package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("t_user")
public class User {

    /**
     * 用户ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long userId;

    /**
     * 用户UID（唯一标识）
     */
    private String userUid;

    /**
     * 登录名
     */
    private String loginName;

    /**
     * 登录密码（加密后）
     */
    private String loginPwd;

    /**
     * 真实姓名
     */
    private String actualName;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 禁用标志：0-正常，1-禁用
     */
    private Integer disabledFlag;

    /**
     * 删除标志：0-正常，1-删除
     */
    private Integer deletedFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
