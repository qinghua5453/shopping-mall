<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { systemApi } from '@/api/system'
import type { AdminDetail, AdminItem, RoleItem } from '@/types/system'
import { STATUS_LABEL, STATUS_TYPE } from '@/types/system'
import { getErrorMessage } from '@/utils/error'

const loading = ref(false)
const list = ref<AdminItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const query = reactive<{ keyword: string; status: number | '' }>({ keyword: '', status: '' })

const roles = ref<RoleItem[]>([])

const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({ username: '', nickname: '', roleIds: [] as string[] })

const editVisible = ref(false)
const editLoading = ref(false)
const editing = ref<AdminDetail | null>(null)
const editForm = reactive({ nickname: '', email: '', status: 1 })

const assignVisible = ref(false)
const assignLoading = ref(false)
const assignTarget = ref<AdminItem | null>(null)
const assignRoleIds = ref<string[]>([])

const resetVisible = ref(false)
const resetPassword = ref('')
const resetUsername = ref('')

async function loadRoles() {
  try {
    roles.value = await systemApi.listRoles()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await systemApi.pageAdmins({
      keyword: query.keyword || undefined,
      status: query.status === '' ? undefined : query.status,
      page: page.value,
      pageSize: pageSize.value,
    })
    list.value = res.list
    total.value = res.total
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadList()
}

function openCreate() {
  createForm.username = ''
  createForm.nickname = ''
  createForm.roleIds = []
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.username.trim()) {
    ElMessage.warning('请输入登录名')
    return
  }
  createLoading.value = true
  try {
    const res = await systemApi.createAdmin({
      username: createForm.username.trim(),
      nickname: createForm.nickname.trim() || undefined,
      roleIds: createForm.roleIds,
    })
    resetUsername.value = res.username
    resetPassword.value = res.password
    createVisible.value = false
    resetVisible.value = true
    ElMessage.success('创建成功，请保存初始密码')
    await loadList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    createLoading.value = false
  }
}

async function openEdit(row: AdminItem) {
  try {
    const detail = await systemApi.getAdmin(row.id)
    editing.value = detail
    editForm.nickname = detail.nickname ?? ''
    editForm.email = detail.email ?? ''
    editForm.status = detail.status
    editVisible.value = true
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  }
}

async function submitEdit() {
  if (!editing.value) return
  editLoading.value = true
  try {
    await systemApi.updateAdmin(editing.value.id, {
      nickname: editForm.nickname,
      email: editForm.email,
      status: editForm.status,
    })
    editVisible.value = false
    ElMessage.success('保存成功')
    await loadList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    editLoading.value = false
  }
}

async function openAssign(row: AdminItem) {
  try {
    const detail = await systemApi.getAdmin(row.id)
    assignTarget.value = row
    assignRoleIds.value = detail.roleIds ?? []
    assignVisible.value = true
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  }
}

async function submitAssign() {
  if (!assignTarget.value) return
  assignLoading.value = true
  try {
    await systemApi.assignAdminRoles(assignTarget.value.id, assignRoleIds.value)
    assignVisible.value = false
    ElMessage.success('角色已更新')
    await loadList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    assignLoading.value = false
  }
}

async function handleResetPassword(row: AdminItem) {
  try {
    await ElMessageBox.confirm(`确定为「${row.username}」重置密码？重置后将强制下线并要求首次改密。`, '重置密码', {
      type: 'warning',
    })
    const res = await systemApi.resetAdminPassword(row.id)
    resetUsername.value = row.username
    resetPassword.value = res.password
    resetVisible.value = true
    ElMessage.success('密码已重置')
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

async function handleDelete(row: AdminItem) {
  try {
    await ElMessageBox.confirm(`确定删除管理员「${row.username}」？此操作不可恢复。`, '删除管理员', {
      type: 'warning',
    })
    await systemApi.deleteAdmin(row.id)
    ElMessage.success('删除成功')
    await loadList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

function copyPassword() {
  void navigator.clipboard?.writeText(resetPassword.value)
}

onMounted(() => {
  void loadRoles()
  void loadList()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索登录名 / 昵称"
          clearable
          style="width: 220px"
          @keyup.enter="search"
        />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <div class="toolbar-right">
          <el-button v-permission="'system:admin:create'" type="primary" @click="openCreate">新建管理员</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="username" label="登录名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag :type="STATUS_TYPE[row.status]">{{ STATUS_LABEL[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="需改密" width="90">
          <template #default="{ row }: { row: any }">
            <el-tag v-if="row.pwdResetRequired === 1" type="warning">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="170" />
        <el-table-column prop="createdAt" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button v-permission="'system:admin:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'system:admin:assign-role'" link type="primary" @click="openAssign(row)">分配角色</el-button>
            <el-button v-permission="'system:admin:reset-password'" link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button v-permission="'system:admin:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建管理员" width="480px">
      <el-form label-width="90px">
        <el-form-item label="登录名" required>
          <el-input v-model="createForm.username" placeholder="3-64 位" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" title="编辑管理员" width="480px">
      <el-form label-width="90px">
        <el-form-item label="登录名">
          <el-input :model-value="editing?.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="assignVisible" title="分配角色" width="480px">
      <p class="dialog-tip">为「{{ assignTarget?.username }}」分配角色（可多选）。</p>
      <el-select v-model="assignRoleIds" multiple style="width: 100%">
        <el-option v-for="r in roles" :key="r.id" :label="`${r.name}（${r.code}）`" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码结果 -->
    <el-dialog v-model="resetVisible" title="初始密码" width="420px">
      <p>管理员「{{ resetUsername }}」的新密码如下，请妥善保存并告知对方：</p>
      <el-input :model-value="resetPassword" readonly type="text">
        <template #append>
          <el-button @click="copyPassword">复制</el-button>
        </template>
      </el-input>
      <p class="dialog-tip">该账号首次登录时将被强制修改密码。</p>
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
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  .toolbar-right {
    margin-left: auto;
  }
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.dialog-tip {
  color: #909399;
  font-size: 12px;
}
</style>
