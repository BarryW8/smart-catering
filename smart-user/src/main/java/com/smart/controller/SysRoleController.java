package com.smart.controller;

import cn.hutool.core.date.DatePattern;
import com.smart.base.BaseCommonController;
import com.smart.base.BaseController;
import com.smart.base.PageView;
import com.smart.base.SimpleModel;
import com.smart.dto.SysRoleDTO;
import com.smart.dto.SysRolePageDTO;
import com.smart.dto.SysUserRoleDTO;
import com.smart.model.LoginUser;
import com.smart.model.user.SysRole;
import com.smart.model.user.SysRoleMenu;
import com.smart.model.user.SysUserRole;
import com.smart.service.SysRoleMenuService;
import com.smart.service.SysRoleService;
import com.smart.service.SysUserService;
import com.smart.uid.impl.CachedUidGenerator;
import com.smart.util.JsonData;
import com.smart.vo.SysUserRoleVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sysRole")
@Slf4j
public class SysRoleController extends BaseController implements BaseCommonController<SysRole, SysRolePageDTO> {

    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysRoleMenuService sysRoleMenuService;

    @PostMapping("saveRoleUser")
    public JsonData saveRoleUser(@RequestBody SysUserRoleDTO dto) {
        Long roleId = dto.getRoleId();
        if (roleId == null) {
            return JsonData.buildError("角色不能为空");
        }
        dto.setCurrentUser(getCurrentUser());
        dto.setCurrentDate(getCurrentDate());
        // 1. 查询用户-角色关联信息，用于刷新缓存（前置用于删除缓存）
        List<SysUserRoleVO> oldUserRoles = sysRoleService.findRoleUser(roleId);
        // 2. 保存用户-角色关联信息（不用判断是否成功，支持保存空值）
        sysRoleService.saveRoleUser(dto);
        if (!CollectionUtils.isEmpty(oldUserRoles)) {
            // 3. 刷新系统用户缓存
            List<Long> userIds = oldUserRoles.stream().map(SysUserRoleVO::getUserId).distinct().collect(Collectors.toList());
            sysUserService.setUserCache(userIds, 1);
        }
        return JsonData.buildSuccess();
    }

    /**
     * 查询用户授权角色信息
     */
    @GetMapping("findRoleUser")
    public JsonData findRoleUser(@RequestParam Long modelId) {
        List<SysUserRoleVO> list = sysRoleService.findRoleUser(modelId);
        return JsonData.buildSuccess(list);
    }

    @GetMapping("findById")
    @Override
    public JsonData findById(@RequestParam Long modelId) {
        // 1. 获取角色详情
        SysRole sysRole = sysRoleService.findById(modelId);
        if (sysRole == null) {
            return JsonData.buildError("角色不存在!");
        }
        // 2. 获取角色菜单权限
        List<String> permList = new ArrayList<>();
        List<SysRoleMenu> list = sysRoleMenuService.findList(" and role_id = '"+modelId+"'");
        if (!CollectionUtils.isEmpty(list)) {
            for (SysRoleMenu roleMenu : list) {
                String perm = roleMenu.getMenuAction();
                if (StringUtils.isNotEmpty(perm)){
                    if (perm.contains(",")) {
                        String[] split = perm.split(",");
                        for (String s : split) {
                            permList.add(roleMenu.getMenuId() + "-" + s);
                        }
                    } else {
                        permList.add(roleMenu.getMenuId() + "-" + perm);
                    }
                } else {
                    permList.add(roleMenu.getMenuId().toString());
                }
            }
        }
        // 3. 封装返回参数
        Map<String, Object> map = new HashMap<>();
        map.put("roleInfo", sysRole);
        map.put("permList", permList);
        return JsonData.buildSuccess(map);
    }

    @Override
    public JsonData save(SysRole sysRole) {
        return null;
    }

    @PostMapping("save")
    public JsonData save(@RequestBody SysRoleDTO dto) {
        dto.setCurrentUser(getCurrentUser());
        dto.setCurrentDate(getCurrentDate());
        int result = sysRoleService.saveRole(dto);
        if (result > 0) {
            // TODO 刷新缓存
            return JsonData.buildSuccess();
        }
        return JsonData.buildError("保存失败!");
    }

    @PostMapping("findPage")
    @Override
    public JsonData findPage(@RequestBody SysRolePageDTO dto) {
        String condition = this.queryCondition(dto);
        PageView<SysRole> pageList = sysRoleService.findPage(dto.getPageNum(), dto.getPageSize(), condition, null);
        return JsonData.buildSuccess(pageList);
    }

    @GetMapping("deleteById")
    @Override
    public JsonData deleteById(@RequestParam Long modelId) {
        LoginUser currentUser = getCurrentUser();
        // 删角色主表和中间表
        SimpleModel simpleModel = new SimpleModel();
        simpleModel.setModelId(modelId);
        simpleModel.setDelUser(currentUser.getUserId());
        simpleModel.setDelUserName(currentUser.getRealName());
        simpleModel.setDelDate(getCurrentDate());
        int result = sysRoleService.deleteBySm(simpleModel);
        if (result > 0) {
            return JsonData.buildSuccess();
        }
        return JsonData.buildError("删除失败!");
    }

    private String queryCondition(SysRolePageDTO dto) {
        String keyword = dto.getKeyword();
        StringBuilder sqlBf = new StringBuilder();
        if (dto.getStatus() != null) {
            sqlBf.append(" and status =").append(dto.getStatus());
        }
        if (StringUtils.isNotEmpty(keyword)) {
            sqlBf.append(" and ( role_name like '%").append(keyword).append("%'").append(")");
        }
        return sqlBf.toString();
    }
}
