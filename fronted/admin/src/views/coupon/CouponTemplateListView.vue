<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'

import { couponApi } from '@/api/coupon'
import {
  TEMPLATE_STATUS,
  TEMPLATE_STATUS_LABEL,
  TEMPLATE_STATUS_TYPE,
  type CouponTemplate,
  type CouponTemplateForm,
  type TemplateStatus,
} from '@/types/coupon'
import {
  couponTypeLabel,
  fenToYuan,
  formatCouponRule,
  formatDiscountRate,
  formatValidity,
  scopeTypeLabel,
  validityTypeLabel,
} from '@/utils/coupon'
import { getErrorMessage } from '@/utils/error'

// ===== 列表状态 =====
const loading = ref(false)
const list = ref<CouponTemplate[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const query = reactive<{ status: TemplateStatus | ''; keyword: string }>({
  status: '',
  keyword: '',
})

async function fetchList() {
  loading.value = true
  try {
    const params: { page: number; pageSize: number; status?: number; keyword?: string } = {
      page: page.value,
      pageSize: pageSize.value,
    }
    if (query.status !== '') params.status = query.status
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    const res = await couponApi.pageTemplates(params)
    list.value = res.list
    total.value = res.total
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  page.value = p
  void fetchList()
}
function handlePageSizeChange(s: number) {
  page.value = 1
  pageSize.value = s
  void fetchList()
}
function handleSearch() {
  page.value = 1
  void fetchList()
}
function handleReset() {
  query.status = ''
  query.keyword = ''
  page.value = 1
  void fetchList()
}

// ===== 弹窗：新建/修改 =====
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

function defaultForm(): CouponTemplateForm {
  return {
    name: '',
    couponType: 1,
    scopeType: 1,
    scopeValues: '{}',
    thresholdAmount: 0,
    discountAmount: 0,
    discountRate: null,
    maxDiscountAmount: null,
    totalQuantity: 100,
    perUserLimit: 1,
    validityType: 2,
    validStart: '',
    validEnd: '',
    validDays: 30,
    receiveStart: '',
    receiveEnd: '',
    stackGroup: 'PLATFORM',
    stackable: 1,
    refundPolicy: 0,
    receiveChannels: 'ALL',
    sortWeight: 0,
    remark: '',
  }
}
const form = reactive<CouponTemplateForm>(defaultForm())

// el-date-picker datetimerange 不支持 v-model 数组字面量，用 computed 代理
const validRange = computed<string[]>({
  get: () => [form.validStart || '', form.validEnd || ''],
  set: (v) => {
    form.validStart = v?.[0] ?? ''
    form.validEnd = v?.[1] ?? ''
  },
})
const receiveRange = computed<string[]>({
  get: () => [form.receiveStart || '', form.receiveEnd || ''],
  set: (v) => {
    form.receiveStart = v?.[0] ?? ''
    form.receiveEnd = v?.[1] ?? ''
  },
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  couponType: [{ required: true, message: '请选择券类型', trigger: 'change' }],
  scopeType: [{ required: true, message: '请选择适用范围', trigger: 'change' }],
  thresholdAmount: [{ required: true, message: '门槛金额必填', trigger: 'blur' }],
  discountAmount: [{ required: true, message: '优惠金额必填（折扣券请填 0）', trigger: 'blur' }],
  totalQuantity: [{ required: true, message: '发放总量必填', trigger: 'blur' }],
  perUserLimit: [{ required: true, message: '每人限领必填', trigger: 'blur' }],
  validityType: [{ required: true, message: '请选择有效期类型', trigger: 'change' }],
  stackGroup: [{ required: true, message: '叠加组必填', trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}
async function openEdit(row: CouponTemplate) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    couponType: row.couponType,
    scopeType: row.scopeType,
    scopeValues: row.scopeValues ?? '{}',
    thresholdAmount: row.thresholdAmount,
    discountAmount: row.discountAmount,
    discountRate: row.discountRate,
    maxDiscountAmount: row.maxDiscountAmount,
    totalQuantity: row.totalQuantity,
    perUserLimit: row.perUserLimit,
    validityType: row.validityType,
    validStart: row.validStart ?? '',
    validEnd: row.validEnd ?? '',
    validDays: row.validDays ?? null,
    receiveStart: row.receiveStart ?? '',
    receiveEnd: row.receiveEnd ?? '',
    stackGroup: row.stackGroup,
    stackable: row.stackable ?? 1,
    refundPolicy: row.refundPolicy ?? 0,
    receiveChannels: row.receiveChannels ?? 'ALL',
    sortWeight: row.sortWeight ?? 0,
    remark: row.remark ?? '',
  })
  dialogVisible.value = true
}

const submitLoading = ref(false)
async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    if (editingId.value) {
      await couponApi.updateTemplate(editingId.value, form)
      ElMessage.success('修改成功')
    } else {
      await couponApi.createTemplate(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    void fetchList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    submitLoading.value = false
  }
}

function handleDialogClosed() {
  formRef.value?.resetFields()
}

// ===== 单行操作 =====
const rowLoading = ref<Record<number, boolean>>({})
async function withRowLoading<T>(id: number, fn: () => Promise<T>) {
  rowLoading.value[id] = true
  try {
    return await fn()
  } finally {
    rowLoading.value[id] = false
  }
}

async function handlePublish(row: CouponTemplate) {
  try {
    await ElMessageBox.confirm(
      `确认发布 "${row.name}"？发布后关键字段将被锁定，不可修改。`,
      '发布确认',
      { type: 'warning', confirmButtonText: '发布', cancelButtonText: '取消' },
    )
    await withRowLoading(row.id, () => couponApi.publishTemplate(row.id))
    ElMessage.success('发布成功')
    void fetchList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

async function handleOffline(row: CouponTemplate) {
  try {
    await ElMessageBox.confirm(`确认下线 "${row.name}"？`, '下线确认', { type: 'warning' })
    await withRowLoading(row.id, () => couponApi.offlineTemplate(row.id))
    ElMessage.success('已下线')
    void fetchList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

async function handleDelete(row: CouponTemplate) {
  try {
    await ElMessageBox.confirm(
      `删除后无法恢复。确认删除草稿 "${row.name}"？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' },
    )
    await withRowLoading(row.id, () => couponApi.deleteTemplate(row.id))
    ElMessage.success('已删除')
    void fetchList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

// ===== 按钮可用性 =====
function canEdit(t: CouponTemplate) {
  return t.status !== TEMPLATE_STATUS.PUBLISHED
}
function canDelete(t: CouponTemplate) {
  return t.status === TEMPLATE_STATUS.DRAFT
}
function canPublish(t: CouponTemplate) {
  return t.status !== TEMPLATE_STATUS.PUBLISHED
}
function canOffline(t: CouponTemplate) {
  return t.status === TEMPLATE_STATUS.PUBLISHED
}

const typeOptions = [
  { label: '满减券', value: 1 },
  { label: '折扣券', value: 2 },
  { label: '无门槛券', value: 3 },
]
const scopeOptions = [
  { label: '全场', value: 1 },
  { label: '类目', value: 2 },
  { label: 'SPU', value: 3 },
  { label: 'SKU', value: 4 },
]
const validityOptions = [
  { label: '固定区间', value: 1 },
  { label: '领取后 N 天', value: 2 },
]

// 联动：折扣券时 rate 必填
watch(
  () => form.couponType,
  (v) => {
    if (v === 2) {
      if (!formRules.discountRate) {
        formRules.discountRate = [
          { required: true, message: '折扣券需要填 discountRate（1~10）', trigger: 'blur' },
        ]
      }
    } else {
      formRules.discountRate = undefined
      form.discountRate = null
      form.maxDiscountAmount = null
    }
  },
  { immediate: true },
)

// 格式化显示辅助
function ruleOf(row: CouponTemplate) {
  return formatCouponRule(row.couponType, row.thresholdAmount, row.discountAmount, row.discountRate, row.maxDiscountAmount)
}
function validityOf(row: CouponTemplate) {
  return formatValidity(row.validityType, row.validDays, row.validStart, row.validEnd)
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <div class="page-container coupon-template-page">
    <el-card class="page-card" shadow="never">
      <!-- 筛选 -->
      <div class="filter-bar">
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option
            v-for="(label, v) in TEMPLATE_STATUS_LABEL"
            :key="v"
            :label="label"
            :value="v"
          />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="按名称或模板编号搜索"
          clearable
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
        <el-button type="primary" :icon="Plus" class="ml-auto" @click="openCreate">
          新建模板
        </el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="list" v-loading="loading" stripe border size="default">
        <el-table-column label="模板号" prop="templateNo" width="170" fixed />
        <el-table-column label="名称" prop="name" min-width="180">
          <template #default="{ row }">
            <div>{{ row.name }}</div>
            <div class="sub-row" v-if="row.remark">{{ row.remark }}</div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ couponTypeLabel(row.couponType) }}</template>
        </el-table-column>
        <el-table-column label="优惠规则" min-width="180">
          <template #default="{ row }">{{ ruleOf(row) }}</template>
        </el-table-column>
        <el-table-column label="适用" width="90">
          <template #default="{ row }">{{ scopeTypeLabel(row.scopeType) }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="240">
          <template #default="{ row }">{{ validityOf(row) }}</template>
        </el-table-column>
        <el-table-column label="库存/已发" width="130">
          <template #default="{ row }">
            <div>{{ row.totalQuantity }}</div>
            <div class="sub-row">已发 {{ row.issuedQuantity }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="TEMPLATE_STATUS_TYPE[row.status as TemplateStatus]" effect="light">
              {{ TEMPLATE_STATUS_LABEL[row.status as TemplateStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              :loading="rowLoading[row.id]"
              :disabled="!canEdit(row)"
              @click="openEdit(row)"
            >
              修改
            </el-button>
            <el-button
              v-if="canPublish(row)"
              size="small"
              type="success"
              :loading="rowLoading[row.id]"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="canOffline(row)"
              size="small"
              type="warning"
              :loading="rowLoading[row.id]"
              @click="handleOffline(row)"
            >
              下线
            </el-button>
            <el-button
              v-if="canDelete(row)"
              size="small"
              type="danger"
              :loading="rowLoading[row.id]"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>

    <!-- 新建/修改 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '修改券模板' : '新建券模板'"
      width="720px"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
        label-position="right"
      >
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" placeholder="如：端午满减券" maxlength="128" show-word-limit />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="券类型" prop="couponType">
              <el-select v-model="form.couponType">
                <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用范围" prop="scopeType">
              <el-select v-model="form.scopeType">
                <el-option v-for="o in scopeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="门槛金额(分)" prop="thresholdAmount">
              <el-input-number v-model="form.thresholdAmount" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优惠金额(分)" prop="discountAmount">
              <el-input-number v-model="form.discountAmount" :min="0" />
              <div class="field-hint">折扣券请填 0</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item v-if="form.couponType === 2" label="折扣率" prop="discountRate">
              <el-input-number v-model="form.discountRate" :min="0.1" :max="10" :step="0.1" :precision="1" />
              <div class="field-hint">如 8.5 表示 8.5 折</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item v-if="form.couponType === 2" label="封顶金额(分)">
              <el-input-number v-model="form.maxDiscountAmount" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="发放总量" prop="totalQuantity">
              <el-input-number v-model="form.totalQuantity" :min="0" />
              <div class="field-hint">0 = 不限量</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="每人限领" prop="perUserLimit">
              <el-input-number v-model="form.perUserLimit" :min="1" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="叠加组" prop="stackGroup">
              <el-input v-model="form.stackGroup" placeholder="如 PLATFORM / SUMMER" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="有效期类型" prop="validityType">
              <el-select v-model="form.validityType">
                <el-option v-for="o in validityOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.validityType === 2" label="有效天数" prop="validDays">
              <el-input-number v-model="form.validDays" :min="1" :max="3650" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="form.validityType === 1" label="有效区间">
          <el-date-picker
            v-model="validRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 440px"
          />
        </el-form-item>

        <el-form-item label="领取时间窗">
          <el-date-picker
            v-model="receiveRange"
            type="datetimerange"
            start-placeholder="不限制"
            end-placeholder="不限制"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 440px"
          />
          <div class="field-hint">留空 = 不限制</div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
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
.coupon-template-page {
  .filter-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }
  .pagination-bar {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
  .ml-auto {
    margin-left: auto;
  }
  .sub-row {
    color: #8a94a6;
    font-size: 12px;
    line-height: 1.4;
  }
  .field-hint {
    margin-top: 4px;
    color: #8a94a6;
    font-size: 12px;
  }
}
</style>
