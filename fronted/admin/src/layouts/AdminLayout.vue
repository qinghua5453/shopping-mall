<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  DataBoard,
  Expand,
  Fold,
  Goods,
  Menu,
  UserFilled,
} from '@element-plus/icons-vue'

import { changePassword } from '@/api/auth'
import routerInstance from '@/router'
import { resetPermissionRoutes } from '@/router/permission'
import { businessRoutes } from '@/router/routes'
import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/utils/error'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const menuRoutes = computed(() =>
  businessRoutes.filter((item) => item.meta?.menu && auth.checkPermission(item.meta.permission)),
)

function menuPath(path: string): string {
  return `/${path}`
}

async function handleCommand(command: string): Promise<void> {
  if (command === 'password') {
    openChangePwd()
    return
  }
  if (command !== 'logout') {
    return
  }

  await ElMessageBox.confirm('确定退出当前管理员账号吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  })

  try {
    await auth.logout()
  } catch {
    ElMessage.warning('服务端退出失败，已清理本地登录状态')
  } finally {
    resetPermissionRoutes(routerInstance)
    await router.replace({ name: 'Login' })
  }
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

function openChangePwd() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdVisible.value = true
}

async function submitChangePwd() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwdForm.newPassword.length < 8) {
    ElMessage.warning('新密码至少 8 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    pwdVisible.value = false
    auth.clearSession()
    resetPermissionRoutes(routerInstance)
    await router.replace({ name: 'Login' })
  } catch (err) {
    ElMessage.error(getErrorMessage(err))
  } finally {
    pwdLoading.value = false
  }
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '64px' : '224px'" class="admin-aside">
      <div class="admin-logo" :class="{ 'is-collapsed': collapsed }">
        <el-icon :size="26"><Goods /></el-icon>
        <span v-show="!collapsed">MALL ADMIN</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        class="admin-menu"
        background-color="#111a2e"
        text-color="#aab4cb"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item v-for="item in menuRoutes" :key="item.name" :index="menuPath(item.path)">
          <el-icon><Menu /></el-icon>
          <template #title>{{ item.meta?.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="admin-body">
      <el-header class="admin-header">
        <div class="header-left">
          <el-button class="collapse-button" text @click="collapsed = !collapsed">
            <el-icon :size="20"><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <div>
            <p class="header-breadcrumb">运营管理 / {{ route.meta.title || '工作台' }}</p>
            <h1 class="header-title">{{ route.meta.title || '工作台' }}</h1>
          </div>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <button class="user-trigger" type="button">
            <span class="user-avatar"
              ><el-icon><UserFilled /></el-icon
            ></span>
            <span class="user-name">{{ auth.displayName }}</span>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ auth.admin?.username }}</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>

      <el-dialog v-model="pwdVisible" title="修改密码" width="420px" :close-on-click-modal="false">
        <el-form label-width="90px">
          <el-form-item label="原密码">
            <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入原密码"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input
              v-model="pwdForm.newPassword"
              type="password"
              show-password
              placeholder="至少 8 位"
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input
              v-model="pwdForm.confirm"
              type="password"
              show-password
              placeholder="再次输入新密码"
              autocomplete="off"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="pwdVisible = false">取消</el-button>
          <el-button type="primary" :loading="pwdLoading" @click="submitChangePwd">确认修改</el-button>
        </template>
      </el-dialog>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.admin-layout {
  min-height: 100vh;
  background: #f4f6fa;
}

.admin-aside {
  overflow: hidden;
  background: var(--admin-sidebar);
  transition: width 0.2s ease;
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 68px;
  padding: 0 20px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.admin-logo.is-collapsed {
  justify-content: center;
  padding: 0;
}

.admin-menu {
  border-right: 0;
}

.admin-menu:not(.el-menu--collapse) {
  width: 224px;
}

.admin-body {
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 24px;
  border-bottom: 1px solid #edf0f5;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-button {
  padding: 8px;
  color: #606b80;
}

.header-breadcrumb {
  margin: 0 0 2px;
  color: #9aa3b2;
  font-size: 11px;
}

.header-title {
  margin: 0;
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 8px;
  border: 0;
  border-radius: 8px;
  color: #374151;
  background: transparent;
  cursor: pointer;
}

.user-trigger:hover {
  background: #f3f5f9;
}

.user-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #5675f5, #7a9bff);
  place-items: center;
}

.user-name {
  font-size: 14px;
}

.user-arrow {
  color: #9aa3b2;
  font-size: 12px;
}

.admin-main {
  min-width: 0;
  padding: 0;
}
</style>
