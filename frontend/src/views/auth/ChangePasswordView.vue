<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { isValidNewPassword } from '@/utils/password'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref()
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const isForced = computed(() => Boolean(userStore.mustChangePassword))
const pageTitle = computed(() =>
  isForced.value ? '首次登录，请修改密码' : '修改密码',
)
const pageSubtitle = computed(() =>
  isForced.value
    ? '为了保障您的账号安全，请修改初始密码后继续使用系统。'
    : '请设置符合安全要求的新密码。',
)

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请输入新密码'))
          return
        }
        if (!isValidNewPassword(value)) {
          callback(new Error('新密码须为6-20位，并同时包含英文字母和数字'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请确认新密码'))
          return
        }
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function onSubmit() {
  if (!formRef.value || loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.resetState()
    await router.replace('/login')
  } catch (e) {
    ElMessage.error(e.message || '修改密码失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="change-password-page">
    <el-card class="change-password-card" shadow="hover">
      <div class="brand">
        <span class="brand-logo">
          <el-icon :size="22"><Lock /></el-icon>
        </span>
        <h1>智慧养老服务平台</h1>
        <p>账号安全验证</p>
      </div>

      <h2 class="title">{{ pageTitle }}</h2>
      <p class="subtitle">{{ pageSubtitle }}</p>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="rule-alert"
        title="新密码需为 6-20 位，并同时包含英文字母和数字。"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @keyup.enter="onSubmit"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="onSubmit"
          >
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.change-password-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    linear-gradient(180deg, #eaf3f8 0%, var(--ec-bg-page) 55%, #eef6f1 100%);
}

.change-password-card {
  width: 100%;
  max-width: 440px;
  border-radius: 12px;
}

.brand {
  text-align: center;
  margin-bottom: 8px;
}

.brand-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--ec-color-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.brand h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--ec-color-primary-dark);
}

.brand p {
  margin: 0;
  color: var(--ec-text-muted);
  font-size: 13px;
}

.title {
  margin: 12px 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--ec-text);
  text-align: center;
}

.subtitle {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ec-text-muted);
  text-align: center;
}

.rule-alert {
  margin-bottom: 16px;
}

.submit-btn {
  width: 100%;
}
</style>
