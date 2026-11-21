<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isForbidden = computed(() => route.name === 'Forbidden')
const code = computed(() => (isForbidden.value ? '403' : '404'))
const message = computed(() =>
  isForbidden.value ? '当前账号没有访问该页面的权限' : '访问的页面不存在或已被移除',
)
</script>

<template>
  <div class="error-page">
    <el-card class="page-card error-card" shadow="never">
      <p class="error-code">{{ code }}</p>
      <h1>{{ message }}</h1>
      <p>如需继续访问，请联系系统管理员或返回工作台。</p>
      <el-button type="primary" @click="$router.replace('/dashboard')">返回工作台</el-button>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: grid;
  min-height: 100vh;
  padding: 32px;
  place-items: center;
}

.error-card {
  width: min(100%, 520px);
  padding: 32px;
  text-align: center;
}

.error-code {
  margin: 0;
  color: var(--admin-primary);
  font-size: 80px;
  font-weight: 700;
  line-height: 1;
}

.error-card h1 {
  margin: 24px 0 12px;
  font-size: 20px;
}

.error-card p:not(.error-code) {
  margin: 0 0 28px;
  color: #8a94a6;
}
</style>
