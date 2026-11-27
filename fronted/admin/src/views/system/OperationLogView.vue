<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { systemApi } from '@/api/system'
import type { OperationLogItem } from '@/types/system'
import { LOG_SUCCESS_LABEL, LOG_SUCCESS_TYPE, MODULE_OPTIONS } from '@/types/system'
import { getErrorMessage } from '@/utils/error'

const loading = ref(false)
const list = ref<OperationLogItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const query = reactive<{
  module: string
  success: number | ''
  range: [string, string] | []
}>({ module: '', success: '', range: [] })

async function loadList() {
  loading.value = true
  try {
    const [start, end] = query.range && query.range.length === 2 ? query.range : [undefined, undefined]
    const res = await systemApi.pageLogs({
      module: query.module || undefined,
      success: query.success === '' ? undefined : query.success,
      start,
      end,
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

onMounted(() => {
  void loadList()
})
</script>

<template>
  <div class="page-container">
    <el-card class="page-card" shadow="never">
      <div class="page-toolbar">
        <el-select v-model="query.module" placeholder="模块" clearable style="width: 140px">
          <el-option v-for="m in MODULE_OPTIONS" :key="m.value" :label="m.label" :value="m.value" />
        </el-select>
        <el-select v-model="query.success" placeholder="结果" clearable style="width: 120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-date-picker
          v-model="query.range"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
        />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="operation" label="操作" min-width="140" />
        <el-table-column label="请求" min-width="260">
          <template #default="{ row }: { row: any }">
            <el-tag size="small" effect="plain">{{ row.method }}</el-tag>
            <span class="api-path">{{ row.path }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }: { row: any }">
            <el-tag :type="LOG_SUCCESS_TYPE[row.success]" size="small">{{ LOG_SUCCESS_LABEL[row.success] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="errorMsg" label="失败原因" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </el-card>
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
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.api-path {
  margin-left: 6px;
  color: #606266;
  font-family: monospace;
  font-size: 12px;
}
</style>
