<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { couponApi } from '@/api/coupon'
import type { CouponStackRule } from '@/types/coupon'
import { getErrorMessage } from '@/utils/error'

const loading = ref(false)
const list = ref<CouponStackRule[]>([])

async function fetchList() {
  loading.value = true
  try {
    list.value = await couponApi.listStackRules()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

// ===== 新增/编辑 弹窗 =====
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  groupA: '',
  groupB: '',
  allowStack: 0,
  maxStackCount: 1,
  status: 1,
})

function defaultForm() {
  return { groupA: '', groupB: '', allowStack: 0, maxStackCount: 1, status: 1 }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}
function openEdit(row: CouponStackRule) {
  editingId.value = row.id
  Object.assign(form, {
    groupA: row.groupA,
    groupB: row.groupB,
    allowStack: row.allowStack,
    maxStackCount: row.maxStackCount,
    status: row.status,
  })
  dialogVisible.value = true
}

const submitLoading = ref(false)
async function handleSubmit() {
  if (!form.groupA.trim() || !form.groupB.trim()) {
    ElMessage.warning('叠加组 A / B 不能为空')
    return
  }
  if (form.maxStackCount < 1 || form.maxStackCount > 3) {
    ElMessage.warning('maxStackCount 范围 1~3')
    return
  }
  submitLoading.value = true
  try {
    await couponApi.upsertStackRule({
      groupA: form.groupA.trim(),
      groupB: form.groupB.trim(),
      allowStack: form.allowStack,
      maxStackCount: form.maxStackCount,
      status: form.status,
    })
    ElMessage.success(editingId.value ? '已更新' : '已创建')
    dialogVisible.value = false
    void fetchList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: CouponStackRule) {
  try {
    await ElMessageBox.confirm(
      `确认删除叠加规则 ${row.groupA} + ${row.groupB}？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' },
    )
    await couponApi.deleteStackRule(row.groupA, row.groupB)
    ElMessage.success('已删除')
    void fetchList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <div class="page-container coupon-rule-page">
    <el-card class="page-card" shadow="never">
      <div class="filter-bar">
        <div>
          <h3 class="page-title">叠加规则</h3>
          <p class="page-desc">
            不同叠加组（stack_group）之间是否可叠加使用，以及叠加数量上限。组内券互斥，不同组之间按此表判定。
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column label="叠加组 A" prop="groupA" width="180" />
        <el-table-column label="叠加组 B" prop="groupB" width="180" />
        <el-table-column label="是否允许叠加" width="140">
          <template #default="{ row }: { row: any }">
            <el-tag :type="row.allowStack === 1 ? 'success' : 'info'" effect="light">
              {{ row.allowStack === 1 ? '允许叠加' : '不允许叠加' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="叠加上限" width="120">
          <template #default="{ row }: { row: any }">
            {{ row.allowStack === 1 ? `最多 ${row.maxStackCount} 张` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }: { row: any }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="200">
          <template #default="{ row }: { row: any }">{{ row.createdAt.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button size="small" @click="openEdit(row)">修改</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '修改叠加规则' : '新增叠加规则'" width="480px">
      <el-form label-width="110px">
        <el-form-item label="叠加组 A">
          <el-input v-model="form.groupA" placeholder="如 PLATFORM" />
        </el-form-item>
        <el-form-item label="叠加组 B">
          <el-input v-model="form.groupB" placeholder="如 SUMMER" />
        </el-form-item>
        <el-form-item label="是否允许叠加">
          <el-radio-group v-model="form.allowStack">
            <el-radio :value="0">不允许</el-radio>
            <el-radio :value="1">允许</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="叠加上限">
          <el-input-number
            v-model="form.maxStackCount"
            :min="1"
            :max="3"
            :disabled="form.allowStack === 0"
          />
          <div class="hint">范围 1 ~ 3，仅在"允许叠加"时生效</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.coupon-rule-page {
  .filter-bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #edf0f5;
  }
  .page-title { margin: 0 0 6px; font-size: 18px; }
  .page-desc { margin: 0; color: #8a94a6; font-size: 13px; line-height: 1.6; }
  .hint { color: #8a94a6; font-size: 12px; margin-top: 4px; }
}
</style>
