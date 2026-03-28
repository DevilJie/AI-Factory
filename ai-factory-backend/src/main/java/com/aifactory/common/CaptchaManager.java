package com.aifactory.common;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.util.IdUtil;
import com.aifactory.dto.CaptchaVo;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码工具类
 *
 * @Author CaiZy
 * @Date 2025-01-20
 * @Wechat DevilJieH
 * @Email a314170122@outlook.com
 */
public class CaptchaManager {

    /**
     * 验证码存储（生产环境建议使用Redis）
     */
    private static final Map<String, String> CAPTCHA_CACHE = new HashMap<>();

    /**
     * 验证码过期时间（秒）
     */
    private static final int EXPIRE_SECONDS = 300;

    /**
     * 生成验证码
     */
    public static CaptchaVo generateCaptcha() {
        // 定义图形验证码的长、宽、验证码字符数、干扰元素个数
        CircleCaptcha captcha = cn.hutool.captcha.CaptchaUtil.createCircleCaptcha(200, 100, 4, 20);

        // 获取验证码的文本
        String captchaCode = captcha.getCode();

        // 生成UUID
        String captchaUuid = IdUtil.simpleUUID();

        // 存储验证码
        CAPTCHA_CACHE.put(captchaUuid, captchaCode);

        // 返回结果
        CaptchaVo captchaVo = new CaptchaVo();
        captchaVo.setCaptchaBase64Image("data:image/png;base64," + captcha.getImageBase64());
        captchaVo.setCaptchaUuid(captchaUuid);
        captchaVo.setExpireSeconds(EXPIRE_SECONDS);

        return captchaVo;
    }

    /**
     * 验证验证码
     */
    public static boolean verifyCaptcha(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }

        String cachedCode = CAPTCHA_CACHE.get(uuid);
        if (cachedCode == null) {
            return false;
        }

        // 验证后删除验证码（一次性使用）
        CAPTCHA_CACHE.remove(uuid);

        // 忽略大小写比较
        return cachedCode.equalsIgnoreCase(code);
    }
}
