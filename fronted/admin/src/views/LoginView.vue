<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Goods, Lock, User } from '@element-plus/icons-vue'

import { useAuthStore } from '@/stores/auth'
import { getErrorMessage } from '@/utils/error'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

async function handleLogin(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  try {
    await auth.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const target = redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/dashboard'
    await router.replace(target)
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '登录失败，请检查账号和密码'))
  }
}
</script>

<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon :size="34"><Goods /></el-icon>
        </div>
        <p class="brand-eyebrow">SHOPPING MALL ADMIN</p>
        <h1>电商运营管理后台</h1>
        <p class="brand-description">
          统一管理会员、权限与优惠券，为运营团队提供安全、清晰、高效的工作台。
        </p>
        <div class="brand-features">
          <span>角色权限</span>
          <span>会员管理</span>
          <span>优惠券运营</span>
        </div>
      </div>
      <p class="brand-footer">Production-ready Vue 3 Admin</p>
    </section>

    <main class="login-panel">
      <el-card class="login-card" shadow="never">
        <div class="login-heading">
          <p class="login-welcome">欢迎回来</p>
          <h2>管理员登录</h2>
          <p>请使用后台管理员账号登录</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="账号" prop="username">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              autocomplete="username"
              placeholder="请输入管理员账号"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              type="password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button
            class="login-button"
            :loading="auth.loggingIn"
            native-type="submit"
            type="primary"
          >
            登录
          </el-button>
        </el-form>

        <div class="service-tip">
          <span class="status-dot"></span>
          开发环境接口代理：http://localhost:8080
        </div>
      </el-card>
    </main>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  display: grid;
  grid-template-columns: minmax(420px, 1.15fr) minmax(460px, 0.85fr);
  min-height: 100vh;
  background: #fff;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  padding: 72px;
  color: #fff;
  background: linear-gradient(145deg, #101a36 0%, #243a8f 58%, #365cf5 100%);
}

.brand-panel::before,
.brand-panel::after {
  position: absolute;
  border: 1px solid rgb(255 255 255 / 12%);
  border-radius: 50%;
  content: '';
}

.brand-panel::before {
  top: -180px;
  right: -140px;
  width: 480px;
  height: 480px;
}

.brand-panel::after {
  right: 100px;
  bottom: -260px;
  width: 560px;
  height: 560px;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 560px;
}

.brand-logo {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: 42px;
  border: 1px solid rgb(255 255 255 / 24%);
  border-radius: 18px;
  background: rgb(255 255 255 / 12%);
  place-items: center;
}

.brand-eyebrow {
  margin: 0 0 14px;
  color: #b8c7ff;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.2em;
}

.brand-content h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.2;
}

.brand-description {
  max-width: 480px;
  margin: 24px 0 32px;
  color: #d8e0ff;
  font-size: 16px;
  line-height: 1.9;
}

.brand-features {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.brand-features span {
  padding: 8px 14px;
  border: 1px solid rgb(255 255 255 / 16%);
  border-radius: 20px;
  color: #e6ebff;
  background: rgb(255 255 255 / 8%);
  font-size: 13px;
}

.brand-footer {
  position: absolute;
  bottom: 28px;
  left: 72px;
  margin: 0;
  color: rgb(255 255 255 / 45%);
  font-size: 12px;
}

.login-panel {
  display: grid;
  padding: 48px;
  background: #f8faff;
  place-items: center;
}

.login-card {
  width: min(100%, 420px);
  padding: 20px;
  border: 1px solid #edf0f7;
  border-radius: 18px;
  box-shadow: 0 20px 60px rgb(35 55 110 / 10%);
}

.login-heading {
  margin-bottom: 32px;
}

.login-welcome {
  margin: 0 0 8px;
  color: var(--admin-primary);
  font-size: 13px;
  font-weight: 600;
}

.login-heading h2 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 28px;
}

.login-heading p:last-child {
  margin: 0;
  color: #8a94a6;
  font-size: 14px;
}

.login-button {
  width: 100%;
  height: 44px;
  margin-top: 8px;
  border-radius: 8px;
  font-weight: 600;
}

.service-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 28px;
  color: #9aa3b2;
  font-size: 12px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #67c23a;
}

@media (max-width: 1100px) {
  .brand-panel {
    padding: 48px;
  }

  .brand-footer {
    left: 48px;
  }
}
</style>
