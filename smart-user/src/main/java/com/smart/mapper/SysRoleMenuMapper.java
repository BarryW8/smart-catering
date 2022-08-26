package com.smart.mapper;

import com.smart.base.BaseMapper;
import com.smart.model.user.SysRoleMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色菜单中间表
 *
 */
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    int saveList(@Param("list") List<SysRoleMenu> list);

    int delByRoleId(Long id);
}
