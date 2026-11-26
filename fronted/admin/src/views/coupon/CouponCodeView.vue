<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { couponApi } from '@/api/coupon'
import type { CouponCode, CouponCodeBatch, CouponTemplate } from '@/types/coupon'
import { CODE_STATUS_LABEL, CODE_STATUS_TYPE } from '@/types/coupon'
import { getErrorMessage } from '@/utils/error'

const loading = ref(false)
const list = ref<CouponCode[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const query = reactive<{ batchId: number | ''; status: number | '' }>({
  batchId: '',
  status: '',
})

// ===== 批次相关 =====
const templateLoading = ref(false)
const templates = ref<CouponTemplate[]>([])
const generateVisible = ref(false)
const generateLoading = ref(false)
const generateCount = ref(100)
const selectedTemplateId = ref<number | null>(null)
const batches = ref<CouponCodeBatch[]>([])

async function loadBatches() {
  try {
    batches.value = await couponApi.listBatches(200)
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  }
}

async function loadTemplates() {
  templateLoading.value = true
  try {
    const res = await couponApi.pageTemplates({ page: 1, pageSize: 100 })
    templates.value = res.list
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    templateLoading.value = false
  }
}

function openGenerate() {
  generateCount.value = 100
  selectedTemplateId.value = templates.value.length > 0 ? templates.value[0].id : null
  generateVisible.value = true
  void loadTemplates()
}

async function handleGenerate() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择模板')
    return
  }
  if (generateCount.value <= 0 || generateCount.value > 5000) {
    ElMessage.warning('生成数量范围 1 ~ 5000')
    return
  }
  generateLoading.value = true
  try {
    const batch = await couponApi.generateCodes(selectedTemplateId.value, { count: generateCount.value })
    query.batchId = batch.id
    generateVisible.value = false
    page.value = 1
    await loadBatches()
    await fetchList()
    ElMessage.success(`生成成功：批次 ${batch.batchNo}，共 ${batch.total} 张`)
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    generateLoading.value = false
  }
}

async function fetchList() {
  if (!query.batchId) {
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const params: { batchId: number; page: number; pageSize: number; status?: number } = {
      batchId: query.batchId,
      page: page.value,
      pageSize: pageSize.value,
    }
    if (query.status !== '') params.status = query.status
    const res = await couponApi.pageCodes(params)
    list.value = res.list
    total.value = res.total
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

function handleBatchChange() {
  page.value = 1
  void fetchList()
}
function handleStatusChange() {
  page.value = 1
  void fetchList()
}

async function handleVoid(row: CouponCode) {
  try {
    await ElMessageBox.confirm(`确认作废券码 "${row.code}"？此操作不可恢复。`, '作废确认', {
      type: 'warning',
      confirmButtonText: '作废',
      confirmButtonClass: 'el-button--danger',
    })
    await couponApi.voidCode(row.code)
    ElMessage.success('已作废')
    void fetchList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

onMounted(() => {
  void loadTemplates()
  void loadBatches()
})
</script>

<template>
  <div class="page-container coupon-code-page">
    <el-card class="page-card" shadow="never">
      <div class="filter-bar">
        <el-select
          v-model="query.batchId"
          placeholder="请选择批次"
          filterable
          clearable
          style="width: 240px"
          @change="handleBatchChange"
        >
          <el-option
            v-for="b in batches"
            :key="b.id"
            :label="`${b.batchNo}（${b.total} 张）`"
            :value="b.id"
          />
        </el-select>
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 150px"
          :disabled="!query.batchId"
          @change="handleStatusChange"
        >
          <el-option label="未使用" :value="1" />
          <el-option label="已使用" :value="2" />
          <el-option label="已作废" :value="3" />
        </el-select>
        <el-button type="primary" :disabled="!query.batchId" @click="fetchList">查询</el-button>
        <el-button type="primary" :icon="Plus" class="ml-auto" @click="openGenerate">
          批量生成券码
        </el-button>
      </div>

      <el-alert
        v-if="!query.batchId"
        type="info"
        :closable="false"
        show-icon
        title="请先生成券码批次或选择已有批次。"
        style="margin-bottom: 16px"
      />

      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column label="券码" prop="code" width="220" copyable />
        <el-table-column label="批次ID" prop="batchId" width="100" />
        <el-table-column label="模板ID" prop="templateId" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="CODE_STATUS_TYPE[row.status]" effect="light">
              {{ CODE_STATUS_LABEL[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="领用人" width="120">
          <template #default="{ row }">
            {{ row.memberId ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="使用时间" prop="usedTime" width="200">
          <template #default="{ row }">{{ row.usedTime?.replace('T', ' ') ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createdAt" width="200">
          <template #default="{ row }">{{ row.createdAt.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              :disabled="row.status !== 1"
              @click="handleVoid(row)"
            >
              作废
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { page = p; fetchList() }"
          @size-change="(s) => { page = 1; pageSize = s; fetchList() }"
        />
      </div>
    </el-card>

    <!-- 生成券码 弹窗 -->
    <el-dialog v-model="generateVisible" title="批量生成券码" width="420px">
      <el-form label-width="120px">
        <el-form-item label="模板">
          <el-select
            v-model="selectedTemplateId"
            :loading="templateLoading"
            style="width: 100%"
          >
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="`${t.name}（${t.templateNo}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量">
          <el-input-number
            v-model="generateCount"
            :min="1"
            :max="5000"
            :step="50"
            style="width: 100%"
          />
          <div class="hint">单次最多 5000 张</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">
          立即生成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.coupon-code-page {
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
  .ml-auto { margin-left: auto; }
  .hint { color: #8a94a6; font-size: 12px; margin-top: 4px; }
}
</style>
