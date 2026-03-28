package com.aifactory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户扩展信息实体
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
@Data
@TableName("t_user_ext")
public class UserExt implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 语言
     */
    private String language;

    /**
     * 主题
     */
    private String theme;

    /**
     * 邮件通知
     */
    private Integer emailNotification;

    /**
     * 浏览器通知
     */
    private Integer browserNotification;

    /**
     * 项目通知
     */
    private Integer projectNotification;

    /**
     * 自动保存
     */
    private Integer autoSave;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;
}
