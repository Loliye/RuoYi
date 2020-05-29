package com.mikufans.service.role;

import com.mikufans.common.constant.UserConstants;
import com.mikufans.common.exception.BusinessException;
import com.mikufans.common.util.Convert;
import com.mikufans.common.util.ShiroUtils;
import com.mikufans.common.util.SpringUtils;
import com.mikufans.common.util.StringUtils;
import com.mikufans.domain.role.Role;
import com.mikufans.domain.role.RoleDept;
import com.mikufans.domain.role.RoleMenu;
import com.mikufans.domain.user.UserRole;
import com.mikufans.mapper.role.RoleDeptMapper;
import com.mikufans.mapper.role.RoleMapper;
import com.mikufans.mapper.role.RoleMenuMapper;
import com.mikufans.mapper.user.UserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色  业务层
 */
@Service
public class RoleServiceImpl implements IRoleService
{

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleDeptMapper roleDeptMapper;


    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return
     */
    @Override
    public List<Role> selectRoleList(Role role)
    {
        return roleMapper.selectRoleList(role);
    }

    /**
     * 根据用户id查询权限
     *
     * @param userId 用户ID
     * @return
     */
    @Override
    public Set<String> selectRoleKeys(Long userId)
    {
        List<Role> perms = roleMapper.selectRolesByUserId(userId);
        Set<String> permsSet = new HashSet<>();

        for (Role perm : perms)
        {
            if (StringUtils.isNotNull(perm))
            {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根骨用户id查询角色
     *
     * @param userId 用户ID
     * @return
     */
    @Override
    public List<Role> selectRolesByUserId(Long userId)
    {
        List<Role> userRoles = roleMapper.selectRolesByUserId(userId);
        List<Role> roles = this.selectRoleAll();
        for (Role role : roles)
        {
            for (Role userRole : userRoles)
            {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue())
                {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * todo
     * 查询所有角色
     *
     * @return
     */
    @Override
    public List<Role> selectRoleAll()
    {
        return SpringUtils.getAopProxy(this).selectRoleList(new Role());
    }

    /**
     * 通过角色id查询角色
     *
     * @param roleId 角色ID
     * @return
     */
    @Override
    public Role selectRoleById(Long roleId)
    {
        return roleMapper.selectRoleById(roleId);
    }


    /**
     * 通过id删除角色
     *
     * @param roleId 角色ID
     * @return
     */
    @Override
    public boolean deleteRoleById(Long roleId)
    {
        return roleMapper.deleteRoleById(roleId) > 0;
    }

    /**
     * 批量删除角色
     *
     * @param ids 需要删除的数据ID
     * @return
     * @throws Exception
     */
    @Override
    public int deleteRoleByIds(String ids) throws Exception
    {
        Long[] roleIds = Convert.toLongArray(ids);
        for (Long roleId : roleIds)
        {
            this.checkRoleAllowed(new Role(roleId));
            Role role = this.selectRoleById(roleId);
            if (this.countUserRoleByRoleId(roleId) > 0)
                throw new BusinessException(String.format("%1$s已分配,不能删除", role.getRoleName()));

        }
        return roleMapper.deleteRoleByIds(roleIds);
    }

    /**
     * 插入角色信息
     *
     * @param role 角色信息
     * @return
     */
    @Override
    @Transactional
    public int insertRole(Role role)
    {
        role.setCreateBy(ShiroUtils.getLoginName());
        roleMapper.insertRole(role);
        ShiroUtils.clearCachedAuthorizationInfo();
        return this.insertRoleMenu(role);
    }

    /**
     * 新增角色菜单信息
     */
    public int insertRoleMenu(Role role)
    {
        int rows = 1;
        List<RoleMenu> list = new ArrayList<>();
        for (Long menuId : role.getMenuIds())
        {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(role.getRoleId());
            roleMenu.setMenuId(menuId);
            list.add(roleMenu);
        }
        if (list.size() > 0)
            rows = roleMenuMapper.batchRoleMenu(list);
        return rows;
    }

    @Override
    @Transactional
    public int updateRole(Role role)
    {
        role.setUpdateBy(ShiroUtils.getLoginName());
        // 修改角色信息
        roleMapper.updateRole(role);
        ShiroUtils.clearCachedAuthorizationInfo();
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenuByRoleId(role.getRoleId());
        return insertRoleMenu(role);
    }

    /**
     * 修改数据权限信息
     *
     * @param role 角色信息
     * @return
     */
    @Override
    @Transactional
    public int authDataScope(Role role)
    {
        role.setUpdateBy(ShiroUtils.getLoginName());
        // 修改角色信息
        roleMapper.updateRole(role);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDeptByRoleId(role.getRoleId());
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(role);
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     */
    public int insertRoleDept(Role role)
    {
        int rows = 1;
        // 新增角色与部门（数据权限）管理
        List<RoleDept> list = new ArrayList<>();
        for (Long deptId : role.getDeptIds())
        {
            RoleDept rd = new RoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        if (list.size() > 0)
        {
            rows = roleDeptMapper.batchRoleDept(list);
        }
        return rows;
    }


    /**
     * 校验用户名是否唯一
     *
     * @param role 角色信息
     * @return
     */
    @Override
    public String checkRoleNameUnique(Role role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        Role info = roleMapper.checkRoleNameUnique(role.getRoleName());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.ROLE_NAME_NOT_UNIQUE;
        }
        return UserConstants.ROLE_NAME_UNIQUE;
    }

    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public String checkRoleKeyUnique(Role role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        Role info = roleMapper.checkRoleKeyUnique(role.getRoleKey());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.ROLE_KEY_NOT_UNIQUE;
        }
        return UserConstants.ROLE_KEY_UNIQUE;
    }

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(Role role)
    {
        if (StringUtils.isNotNull(role.getRoleId()) && role.isAdmin())
        {
            throw new BusinessException("不允许操作超级管理员角色");
        }
    }


    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public int countUserRoleByRoleId(Long roleId)
    {
        return userRoleMapper.countUserRoleByRoleId(roleId);
    }

    /**
     * 角色状态修改
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public int changeStatus(Role role)
    {
        return roleMapper.updateRole(role);
    }

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    public int deleteAuthUser(UserRole userRole)
    {
        return userRoleMapper.deleteUserRoleInfo(userRole);
    }

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    @Override
    public int deleteAuthUsers(Long roleId, String userIds)
    {
        return userRoleMapper.deleteUserRoleInfos(roleId, Convert.toLongArray(userIds));
    }

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要删除的用户数据ID
     * @return 结果
     */
    @Override
    public int insertAuthUsers(Long roleId, String userIds)
    {
        Long[] users = Convert.toLongArray(userIds);
        // 新增用户与角色管理
        List<UserRole> list = new ArrayList<UserRole>();
        for (Long userId : users)
        {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        return userRoleMapper.batchUserRole(list);
    }
}
