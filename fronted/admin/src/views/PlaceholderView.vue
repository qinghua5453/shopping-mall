<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta.title || '业务模块')
const permission = computed(() => route.meta.permission)
</script>

<template>
  <div class="page-container">
    <el-card class="page-card placeholder-card" shadow="never">
      <div class="placeholder-heading">
        <div>
          <h2 class="page-title">{{ title }}</h2>
          <p class="page-description">路由、菜单和访问权限已接入，后续在此实现业务列表与操作。</p>
        </div>
        <el-tag v-if="permission" effect="plain" type="info">{{ permission }}</el-tag>
      </div>
      <el-empty description="模块骨架已就绪，等待后端业务接口接入">
        <el-button type="primary" plain @click="$router.push('/dashboard')">返回工作台</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.placeholder-card {
  min-height: calc(100vh - 116px);
}

.placeholder-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 24px;
  border-bottom: 1px solid #edf0f5;
}
</style>
