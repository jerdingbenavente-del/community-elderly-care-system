package com.eldercare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eldercare.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.perm_code
            FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.status = 1
              AND p.deleted = 0
            """)
    List<String> selectPermCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(DISTINCT u.id)
            FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE u.deleted = 0
              AND u.status = 1
              AND r.deleted = 0
              AND r.status = 1
              AND r.role_code = 'ADMIN'
            """)
    long countEnabledAdmins();

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.role_code = 'ADMIN'
              AND r.deleted = 0
              AND r.status = 1
            """)
    long countAdminRoleOfUser(@Param("userId") Long userId);
}
