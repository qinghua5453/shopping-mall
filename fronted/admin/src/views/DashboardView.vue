<script setup lang="ts">
import { computed } from 'vue'
import { ArrowRight, Checked, Connection, Setting } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const permissionCount = computed(() => auth.permissions.length)

const modules = [
  {
    title: '系统与权限',
    description: '管理员、角色、权限树与操作日志',
    permission: 'system:admin:view',
    path: '/system/admins',
  },
  {
    title: '会员管理',
    description: '会员查询、详情与账号状态管理',
    permission: 'member:member:view',
    path: '/member/list',
  },
  {
    title: '优惠券运营',
    description: '券模板、券码、用户券与叠加规则',
    permission: 'coupon:template:view',
    path: '/coupon/templates',
  },
]
</script>

<template>
  <div class="page-container dashboard-page">
    <section class="welcome-card">
      <div>
        <p class="welcome-eyebrow">OPERATIONS CONSOLE</p>
        <h2>{{ auth.displayName }}，欢迎回来</h2>
        <p>管理端基础框架已就绪，可开始接入后端业务接口。</p>
      </div>
      <div class="welcome-icon"><Setting /></div>
    </section>

    <section class="status-grid">
      <el-card class="page-card status-card" shadow="never">
        <el-icon class="status-icon success"><Checked /></el-icon>
        <div><span>工程状态</span><strong>基础框架已就绪</strong></div>
      </el-card>
      <el-card class="page-card status-card" shadow="never">
        <el-icon class="status-icon"><Connection /></el-icon>
        <div><span>后端接口</span><strong>/api/v1/admin</strong></div>
      </el-card>
      <el-card class="page-card status-card" shadow="never">
        <el-icon class="status-icon"><Setting /></el-icon>
        <div>
          <span>当前权限</span><strong>{{ permissionCount }} 项权限码</strong>
        </div>
      </el-card>
    </section>

    <section class="module-section">
      <div class="section-heading">
        <div>
          <h3>业务模块</h3>
          <p>按后端权限展示可访问的功能入口</p>
        </div>
      </div>
      <div class="module-grid">
        <template v-for="module in modules" :key="module.path">
          <router-link
            v-if="auth.checkPermission(module.permission)"
            :to="module.path"
            class="module-card"
          >
            <div>
              <h4>{{ module.title }}</h4>
              <p>{{ module.description }}</p>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </template>
      </div>
    </section>

    <el-alert
      class="backend-alert"
      title="当前为工程骨架，不展示模拟业务数据。启动后端服务后即可使用管理员账号登录并联调。"
      type="info"
      :closable="false"
      show-icon
    />
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 34px 38px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(120deg, #172554, #365cf5);
  box-shadow: 0 12px 30px rgb(54 92 245 / 18%);
}

.welcome-eyebrow {
  margin: 0 0 10px;
  color: #bfccff;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
}

.welcome-card h2 {
  margin: 0 0 10px;
  font-size: 26px;
}

.welcome-card p:last-child {
  margin: 0;
  color: #dbe4ff;
  font-size: 14px;
}

.welcome-icon {
  width: 76px;
  height: 76px;
  opacity: 0.35;
}

.welcome-icon :deep(svg) {
  width: 100%;
  height: 100%;
}

.status-grid,
.module-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.status-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.status-icon {
  padding: 10px;
  border-radius: 10px;
  color: #365cf5;
  background: #edf1ff;
  font-size: 24px;
}

.status-icon.success {
  color: #16a34a;
  background: #ecfdf3;
}

.status-card span,
.status-card strong {
  display: block;
}

.status-card span {
  margin-bottom: 5px;
  color: #8a94a6;
  font-size: 12px;
}

.status-card strong {
  color: #374151;
  font-size: 15px;
}

.section-heading h3 {
  margin: 0 0 6px;
  font-size: 18px;
}

.section-heading p {
  margin: 0 0 16px;
  color: #8a94a6;
  font-size: 13px;
}

.module-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 120px;
  padding: 22px;
  border: 1px solid #e9edf5;
  border-radius: 12px;
  background: #fff;
  transition: all 0.2s ease;
}

.module-card:hover {
  border-color: #b9c7ff;
  box-shadow: 0 8px 24px rgb(54 92 245 / 10%);
  transform: translateY(-2px);
}

.module-card h4 {
  margin: 0 0 10px;
  color: #1f2937;
  font-size: 16px;
}

.module-card p {
  margin: 0;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.6;
}

.module-card > .el-icon {
  color: #365cf5;
}

.backend-alert {
  border-radius: 10px;
}
</style>
