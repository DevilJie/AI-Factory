package com.aifactory.controller;

import com.aifactory.dto.PowerSystemSaveRequest;
import com.aifactory.entity.NovelPowerSystem;
import com.aifactory.response.Result;
import com.aifactory.service.PowerSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 力量体系管理 Controller
 *
 * @Author AI Factory
 * @Date 2026-03-30
 */
@Slf4j
@RestController
@RequestMapping("/api/novel/{projectId}/power-system")
public class PowerSystemController {

    @Autowired
    private PowerSystemService powerSystemService;

    @GetMapping("/list")
    public Result<List<NovelPowerSystem>> list(@PathVariable Long projectId) {
        try {
            return Result.ok(powerSystemService.listByProjectId(projectId));
        } catch (Exception e) {
            log.error("查询力量体系失败", e);
            return Result.error("查询力量体系失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<NovelPowerSystem> getById(@PathVariable Long projectId, @PathVariable Long id) {
        try {
            return Result.ok(powerSystemService.getById(id));
        } catch (Exception e) {
            log.error("查询力量体系详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public Result<NovelPowerSystem> save(@PathVariable Long projectId, @RequestBody PowerSystemSaveRequest request) {
        try {
            request.setProjectId(projectId);
            return Result.ok(powerSystemService.savePowerSystem(request));
        } catch (Exception e) {
            log.error("保存力量体系失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long projectId, @PathVariable Long id) {
        try {
            powerSystemService.deleteById(id);
            return Result.ok("删除成功");
        } catch (Exception e) {
            log.error("删除力量体系失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
