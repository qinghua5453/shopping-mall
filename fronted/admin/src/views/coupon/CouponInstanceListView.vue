<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'

import { couponApi } from '@/api/coupon'
import {
  INSTANCE_STATUS_LABEL,
  INSTANCE_STATUS_TYPE,
  type CouponInstance,
  type CouponTemplate,
} from '@/types/coupon'
import { fenToYuan } from '@/utils/coupon'
import { getErrorMessage } from '@/utils/error'

const loading = ref(false)
const list = ref<CouponInstance[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const query = reactive<{ memberId: number | null; templateId: number | null; status: number | null }>({
  memberId: null,
  templateId: null,
  status: null,
})

// ===== 模板下拉（定向发券用）=====
const templates = ref<CouponTemplate[]>([])
const templateLoading = ref(false)
async function loadTemplates() {
  templateLoading.value = true
  try {
    const res = await couponApi.pageTemplates({ page: 1, pageSize: 200 })
    templates.value = res.list.filter((t) => t.status === 1) // 已发布
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    templateLoading.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params: {
      page: number
      pageSize: number
      memberId?: number
      templateId?: number
      status?: number
    } = { page: page.value, pageSize: pageSize.value }
    if (query.memberId != null) params.memberId = query.memberId
    if (query.templateId != null) params.templateId = query.templateId
    if (query.status != null) params.status = query.status
    const res = await couponApi.pageInstances(params)
    list.value = res.list
    total.value = res.total
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}

// ===== 定向发券 弹窗 =====
const grantVisible = ref(false)
const grantLoading = ref(false)
const grantTemplateId = ref<number | null>(null)
const grantMemberIds = ref<string>('') // 逗号分隔
const grantReason = ref('')

function openGrant() {
  grantMemberIds.value = ''
  grantReason.value = ''
  grantTemplateId.value = templates.value.length > 0 ? templates.value[0].id : null
  grantVisible.value = true
  void loadTemplates()
}

async function handleGrant() {
  if (!grantTemplateId.value) {
    ElMessage.warning('请先选择已发布模板')
    return
  }
  const ids = grantMemberIds.value
    .split(/[,\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map(Number)
  if (ids.length === 0) {
    ElMessage.warning('请输入至少一个会员 ID')
    return
  }
  if (ids.some(Number.isNaN)) {
    ElMessage.warning('会员 ID 必须是数字')
    return
  }
  if (ids.length > 500) {
    ElMessage.warning('单次最多 500 人')
    return
  }
  grantLoading.value = true
  try {
    await couponApi.grant({
      templateId: grantTemplateId.value,
      memberIds: ids,
      reason: grantReason.value,
    })
    ElMessage.success('定向发券已提交')
    grantVisible.value = false
    void fetchList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    grantLoading.value = false
  }
}

// ===== 冻结 / 解冻 =====
const freezeFormRef = ref<FormInstance>()
const freezeVisible = ref(false)
const freezeId = ref<number | null>(null)
const freezeReason = ref('')

async function handleFreeze(row: CouponInstance) {
  freezeId.value = row.id
  freezeReason.value = ''
  freezeVisible.value = true
}

async function confirmFreeze() {
  if (!freezeId.value) return
  if (!freezeReason.value.trim()) {
    ElMessage.warning('请填写冻结原因')
    return
  }
  try {
    await couponApi.freezeInstance(freezeId.value, { reason: freezeReason.value.trim() })
    ElMessage.success('已冻结')
    freezeVisible.value = false
    void fetchList()
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  }
}

async function handleUnfreeze(row: CouponInstance) {
  try {
    await ElMessageBox.confirm('确认解冻？如已过有效期，解冻后会自动变"已过期"。', '解冻确认', { type: 'warning' })
    await couponApi.unfreezeInstance(row.id)
    ElMessage.success('已解冻')
    void fetchList()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(getErrorMessage(err))
  }
}

function canFreeze(row: CouponInstance) {
  return row.status === 1 // UNUSED
}
function canUnfreeze(row: CouponInstance) {
  return row.status === 5 // FROZEN
}

onMounted(() => {
  void fetchList()
  void loadTemplates()
})
</script>

<template>
  <div class="page-container coupon-instance-page">
    <el-card class="page-card" shadow="never">
      <div class="filter-bar">
        <el-input-number
          v-model="query.memberId"
          :min="0"
          placeholder="会员 ID"
          controls-position="right"
          style="width: 160px"
        />
        <el-select
          v-model="query.templateId"
          placeholder="全部模板"
          clearable
          filterable
          loading
          style="width: 220px"
        >
          <el-option
            v-for="t in templates"
            :key="t.id"
            :label="`${t.name}（${t.templateNo}）`"
            :value="t.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option
            v-for="(label, v) in INSTANCE_STATUS_LABEL"
            :key="v"
            :label="label"
            :value="v"
          />
        </el-select>
        <el-button type="primary" @click="() => { page = 1; fetchList() }">查询</el-button>
        <el-button @click="() => { Object.assign(query, { memberId: null, templateId: null, status: null }); page = 1; fetchList() }">
          重置
        </el-button>
        <el-button type="primary" :icon="UserFilled" class="ml-auto" @click="openGrant">
          定向发券
        </el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column label="券号" prop="couponNo" width="200" copyable />
        <el-table-column label="模板ID" prop="templateId" width="100" />
        <el-table-column label="会员ID" prop="memberId" width="120" />
        <el-table-column label="来源" prop="source" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="INSTANCE_STATUS_TYPE[row.status]" effect="light">
              {{ INSTANCE_STATUS_LABEL[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="面额" width="120">
          <template #default="{ row }">¥ {{ fenToYuan(row.discountAmount) }}</template>
        </el-table-column>
        <el-table-column label="有效期" min-width="260">
          <template #default="{ row }">
            {{ row.validStart?.replace('T', ' ').slice(0, 16) ?? '-' }}
            ~
            {{ row.validEnd?.replace('T', ' ').slice(0, 16) ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="领取时间" prop="receiveTime" width="190">
          <template #default="{ row }">{{ row.receiveTime?.replace('T', ' ').slice(0, 16) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              :disabled="!canFreeze(row)"
              @click="handleFreeze(row)"
            >
              冻结
            </el-button>
            <el-button
              size="small"
              :disabled="!canUnfreeze(row)"
              @click="handleUnfreeze(row)"
            >
              解冻
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

    <!-- 定向发券 弹窗 -->
    <el-dialog v-model="grantVisible" title="定向发券" width="520px">
      <el-form label-width="100px">
        <el-form-item label="模板">
          <el-select
            v-model="grantTemplateId"
            :loading="templateLoading"
            style="width: 100%"
            placeholder="仅已发布模板"
          >
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="`${t.name}（${t.templateNo}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="会员 ID">
          <el-input
            v-model="grantMemberIds"
            type="textarea"
            :rows="3"
            placeholder="多个 ID 用逗号或空格分隔，如：9001, 9002, 9003"
          />
          <div class="hint">单次最多 500 人</div>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="grantReason" maxlength="255" show-word-limit placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantLoading" @click="handleGrant">
          立即发放
        </el-button>
      </template>
    </el-dialog>

    <!-- 冻结原因 弹窗 -->
    <el-dialog v-model="freezeVisible" title="冻结用户券" width="420px">
      <el-form ref="freezeFormRef" label-width="80px">
        <el-form-item label="冻结原因" required>
          <el-input v-model="freezeReason" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="如：风控异常 / 会员投诉" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freezeVisible = false">取消</el-button>
        <el-button type="danger" :loading="false" @click="confirmFreeze">确认冻结</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.coupon-instance-page {
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
