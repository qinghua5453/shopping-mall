<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import type { ElTree } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'

import { systemApi } from '@/api/system'
import type { PermissionNode, RoleItem } from '@/types/system'
import { DATA_SCOPE_LABEL, STATUS_LABEL, STATUS_TYPE } from '@/types/system'
import { getErrorMessage } from '@/utils/error'

const SUPER_ADMIN_CODE = 'SUPER_ADMIN'

const loading = ref(false)
const roles = ref<RoleItem[]>([])
const permissionTree = ref<PermissionNode[]>([])

const editVisible = ref(false)
const editLoading = ref(false)
const isCreate = ref(true)
const editForm = reactive({
  id: '',
  code: '',
  name: '',
  description: '',
  dataScope: 1,
  status: 1,
  sort: 0,
})

const assignVisible = ref(false)
const assignLoading = ref(false)
const assignTarget = ref<RoleItem | null>(null)
const treeRef = ref<InstanceType<typeof ElTree> | null>(null)

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await systemApi.listRoles()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  try {
    permissionTree.value = await systemApi.treePermissions()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  }
}

function openCreate() {
  isCreate.value = true
  editForm.id = ''
  editForm.code = ''
  editForm.name = ''
  editForm.description = ''
  editForm.dataScope = 1
  editForm.status = 1
  editForm.sort = 0
  editVisible.value = true
}

function openEdit(row: RoleItem) {
  isCreate.value = false
  editForm.id = row.id
  editForm.code = row.code
  editForm.name = row.name
  editForm.description = row.description ?? ''
  editForm.dataScope = row.dataScope ?? 1
  editForm.status = row.status
  editForm.sort = row.sort ?? 0
  editVisible.value = true
}

async function submitEdit() {
  if (!editForm.name.trim()) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (isCreate.value && !editForm.code.trim()) {
    ElMessage.warning('请输入角色编码')
    return
  }
  editLoading.value = true
  try {
    if (isCreate.value) {
      await systemApi.createRole({
        code: editForm.code.trim(),
        name: editForm.name.trim(),
        description: editForm.description || undefined,
        dataScope: editForm.dataScope,
        sort: editForm.sort,
      })
    } else {
      await systemApi.updateRole(editForm.id, {
        name: editForm.name.trim(),
        description: editForm.description || undefined,
        dataScope: editForm.dataScope,
        status: editForm.status,
        sort: editForm.sort,
      })
    }
    editVisible.value = false
    ElMessage.success('保存成功')
    await loadRoles()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    editLoading.value = false
  }
}

async function openAssign(row: RoleItem) {
  assignTarget.value = row
  assignVisible.value = true
  await nextTick()
  const detail = await systemApi.getRole(row.id)
  treeRef.value?.setCheckedKeys(detail.permissionIds ?? [])
}

async function submitAssign() {
  if (!assignTarget.value) return
  const checked = treeRef.value?.getCheckedKeys() as string[]
  const halfChecked = treeRef.value?.getHalfCheckedKeys() as string[]
  const permissionIds = [...checked, ...halfChecked].map(String)
  assignLoading.value = true
  try {
    await systemApi.assignRolePermissions(assignTarget.value.id, permissionIds)
    assignVisible.value = false
    ElMessage.success('权限已更新，相关管理员将立即生效')
    await loadRoles()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    assignLoading.value = false
  }
}

async function handleDelete(row: RoleItem) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」？`, '删除角色', { type: 'warning' })
    await systemApi.deleteRole(row.id)
    ElMessage.success('删除成功')
    await loadRoles()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

onMounted(() => {
  void loadRoles()
  void loadTree()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <el-button v-permission="'system:role:create'" type="primary" @click="openCreate">新建角色</el-button>
      </div>

      <el-table v-loading="loading" :data="roles" border stripe>
        <el-table-column prop="code" label="编码" min-width="150" />
        <el-table-column prop="name" label="名称" min-width="140">
          <template #default="{ row }: { row: any }">
            {{ row.name }}
            <el-tag v-if="row.code === SUPER_ADMIN_CODE" type="danger" size="small" effect="plain">内置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="数据范围" width="110">
          <template #default="{ row }: { row: any }">{{ DATA_SCOPE_LABEL[row.dataScope] ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag :type="STATUS_TYPE[row.status]">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:role:assign-permission'" link type="primary" @click="openAssign(row)">分配权限</el-button>
            <el-button v-permission="'system:role:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.code !== SUPER_ADMIN_CODE"
              v-permission="'system:role:delete'"
              link
              type="danger"
              @click="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="editVisible" :title="isCreate ? '新建角色' : '编辑角色'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="editForm.code" :disabled="!isCreate" placeholder="如 OPERATOR" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="editForm.dataScope" style="width: 100%">
            <el-option label="全部数据" :value="1" />
            <el-option label="仅本人" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isCreate" label="状态">
          <el-switch
            v-model="editForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
            :disabled="editForm.code === SUPER_ADMIN_CODE"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="assignVisible" title="分配权限" width="560px">
      <p class="dialog-tip">为「{{ assignTarget?.name }}」勾选权限（含菜单与按钮），保存后相关管理员立即生效。</p>
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        node-key="id"
        show-checkbox
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.name }}</span>
            <el-tag size="small" type="info" effect="plain">{{ data.code }}</el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 16px;
}
.page-card {
  min-height: calc(100vh - 116px);
}
.page-toolbar {
  margin-bottom: 16px;
}
.dialog-tip {
  color: #909399;
  font-size: 12px;
}
.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
