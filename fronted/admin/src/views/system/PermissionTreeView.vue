<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { systemApi } from '@/api/system'
import type { PermissionNode } from '@/types/system'
import { PERMISSION_TYPE_LABEL, PERMISSION_TYPE_TAG, STATUS_LABEL, STATUS_TYPE } from '@/types/system'
import { getErrorMessage } from '@/utils/error'

const loading = ref(false)
const tree = ref<PermissionNode[]>([])

const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({ id: '', name: '', icon: '', sort: 0, visible: 1 })

async function loadTree() {
  loading.value = true
  try {
    tree.value = await systemApi.treePermissions()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

function openEdit(row: PermissionNode) {
  editForm.id = row.id
  editForm.name = row.name
  editForm.icon = row.icon ?? ''
  editForm.sort = row.sort ?? 0
  editForm.visible = row.visible ?? 1
  editVisible.value = true
}

async function submitEdit() {
  editLoading.value = true
  try {
    await systemApi.updatePermission(editForm.id, {
      name: editForm.name,
      icon: editForm.icon || undefined,
      sort: editForm.sort,
      visible: editForm.visible,
    })
    editVisible.value = false
    ElMessage.success('保存成功')
    await loadTree()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    editLoading.value = false
  }
}

onMounted(() => {
  void loadTree()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <p class="page-tip">权限的新增与删除由系统迁移维护，此处仅支持编辑名称、图标、排序与可见性。</p>
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column prop="code" label="编码" min-width="220" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag :type="PERMISSION_TYPE_TAG[row.permissionType]" size="small">
              {{ PERMISSION_TYPE_LABEL[row.permissionType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接口" min-width="240">
          <template #default="{ row }: { row: any }">
            <span v-if="row.apiMethod && row.apiPath">
              <el-tag size="small" effect="plain">{{ row.apiMethod }}</el-tag>
              <span class="api-path">{{ row.apiPath }}</span>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }: { row: any }">{{ row.visible === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }: { row: any }">
            <el-tag :type="STATUS_TYPE[row.status]" size="small">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:permission:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑权限" width="460px">
      <el-form label-width="90px">
        <el-form-item label="名称" required>
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="editForm.icon" placeholder="Element Plus 图标名，可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="菜单可见">
          <el-switch v-model="editForm.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
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
.page-tip {
  margin: 0 0 12px;
  color: #909399;
  font-size: 12px;
}
.api-path {
  margin-left: 6px;
  color: #606266;
  font-family: monospace;
  font-size: 12px;
}
</style>
