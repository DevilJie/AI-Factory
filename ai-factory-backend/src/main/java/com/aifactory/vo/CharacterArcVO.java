package com.aifactory.vo;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 人物弧光VO
 *
 * @Author AI Factory
 * @Date 2025-02-07
 */
@Data
@Builder
public class CharacterArcVO {

    /**
     * 初始状态
     */
    private String startingState;

    /**
     * 转折事件
     */
    private String catalystEvent;

    /**
     * 弧光阶段列表
     */
    private List<ArcStageVO> stages;

    /**
     * 最终状态
     */
    private String endState;
}
