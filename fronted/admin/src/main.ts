import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

import App from '@/App.vue'
import { AUTH_EXPIRED_EVENT } from '@/api/http'
import { permissionDirective } from '@/directives/permission'
import router from '@/router'
import { resetPermissionRoutes } from '@/router/permission'
import { useAuthStore } from '@/stores/auth'
import '@/styles/index.scss'

const app = createApp(App)
const pinia = createPinia()

window.addEventListener(AUTH_EXPIRED_EVENT, () => {
  const auth = useAuthStore(pinia)
  const current = router.currentRoute.value
  auth.clearSession()
  resetPermissionRoutes(router)

  if (current.name !== 'Login') {
    void router.replace({ name: 'Login', query: { redirect: current.fullPath } })
  }
})

app.use(pinia)
app.use(router)
app.directive('permission', permissionDirective)
app.mount('#app')
