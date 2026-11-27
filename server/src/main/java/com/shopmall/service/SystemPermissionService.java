package com.shopmall.service;

import com.shopmall.common.BizException;
import com.shopmall.common.ErrorCode;
import com.shopmall.dto.PermissionNode;
import com.shopmall.dto.PermissionUpdateRequest;
import com.shopmall.entity.SysPermission;
import com.shopmall.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemPermissionService {

    private final SysPermissionMapper permissionMapper;
    private final AdminPermissionService permissionService;

    public List<PermissionNode> tree() {
        List<SysPermission> all = permissionMapper.selectAll();
        Map<Long, List<SysPermission>> byParent = all.stream()
                .collect(Collectors.groupingBy(SysPermission::getParentId));
        return buildChildren(0L, byParent);
    }

    @Transactional
    public void update(Long id, PermissionUpdateRequest req) {
        SysPermission perm = permissionMapper.selectById(id);
        if (perm == null) {
            throw new BizException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        perm.setName(req.name());
        perm.setIcon(req.icon() == null ? perm.getIcon() : req.icon());
        perm.setSort(req.sort() == null ? perm.getSort() : req.sort());
        perm.setVisible(req.visible());
        permissionMapper.update(perm);
        permissionService.invalidateAll();
    }

    private List<PermissionNode> buildChildren(Long parentId, Map<Long, List<SysPermission>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(p -> new PermissionNode(
                        String.valueOf(p.getId()), String.valueOf(p.getParentId()),
                        p.getPermissionType(), p.getCode(), p.getName(),
                        p.getRoutePath(), p.getComponent(), p.getApiMethod(), p.getApiPath(),
                        p.getIcon(), p.getSort(), p.getVisible(), p.getStatus(),
                        buildChildren(p.getId(), byParent)))
                .toList();
    }
}
