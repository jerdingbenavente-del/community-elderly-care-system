<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User,
  Lock,
  Right,
  FirstAidKit,
  House,
  UserFilled,
  EditPen,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { canAccessPath } from '@/utils/role'
import heroBg from '@/assets/images/8.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const features = [
  {
    title: '健康管理',
    desc: '实时监测 · 预警提醒',
    icon: FirstAidKit,
    tone: 'blue',
  },
  {
    title: '照护服务',
    desc: '专业服务 · 贴心陪伴',
    icon: House,
    tone: 'green',
  },
  {
    title: '家庭关爱',
    desc: '家属协同 · 共同守护',
    icon: UserFilled,
    tone: 'warm',
  },
  {
    title: '服务评价',
    desc: '持续改进 · 提升质量',
    icon: EditPen,
    tone: 'purple',
  },
]

async function onSubmit() {
  if (!formRef.value || loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const data = await userStore.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')

    if (data?.mustChangePassword || userStore.mustChangePassword) {
      await router.replace('/change-password')
      return
    }

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect && canAccessPath(redirect, userStore.roles)) {
      await router.replace(redirect)
    } else {
      await router.replace(userStore.getHomePath())
    }
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!--
    方案 A：背景图 8.png（已含左侧宣传语/品牌氛围）
    + HTML 登录卡片 + HTML 底部功能区
    禁止再用 HTML 叠加左侧标题/宣传语/页面级 Logo，避免重影
  -->
  <div class="login-page">
    <div class="login-bg" :style="{ backgroundImage: `url(${heroBg})` }" aria-hidden="true" />
    <div class="login-bg-mask" aria-hidden="true" />

    <div class="login-shell">
      <!-- 右侧真实登录卡片（非整图）；页面级 Logo/宣传语仅保留在背景图中 -->
      <section class="login-panel">
        <div class="login-card">
          <div class="card-brand">
            <span class="brand-logo" aria-hidden="true">护</span>
            <div>
              <h2>智慧养老服务平台</h2>
              <p class="card-en">Elder Care Management System</p>
              <p class="card-welcome">欢迎登录，请输入您的账号和密码</p>
            </div>
          </div>

          <el-form
            ref="formRef"
            class="login-form"
            :model="form"
            :rules="rules"
            size="large"
            @keyup.enter="onSubmit"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                clearable
                autocomplete="username"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                autocomplete="current-password"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item class="login-form__action">
              <el-button
                type="primary"
                class="login-btn"
                :loading="loading"
                @click="onSubmit"
              >
                <span>登录</span>
                <el-icon class="login-btn__arrow"><Right /></el-icon>
              </el-button>
            </el-form-item>
          </el-form>

          <p class="card-foot">
            <i class="foot-heart" aria-hidden="true" />
            用科技守护每一位老人
          </p>
        </div>
      </section>

      <footer class="feature-bar">
        <div
          v-for="item in features"
          :key="item.title"
          class="feature-item"
          :class="`feature-item--${item.tone}`"
        >
          <div class="feature-icon">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </div>
          <div class="feature-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  --login-blue: #4a9bd1;
  --login-blue-deep: #3a86b8;
  --login-green: #66c89b;
  --login-warm: #f5c76a;
  --login-purple: #9b7ed9;
  --login-text: #23415f;
  --login-muted: #7a94a8;

  position: relative;
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  color: var(--login-text);
  isolation: isolate;
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-size: cover;
  background-position: center center;
  background-repeat: no-repeat;
}

/* 轻遮罩：右侧/底部略提亮保证卡片与功能条可读，左侧保持自然色彩 */
.login-bg-mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(
      90deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0) 55%,
      rgba(255, 255, 255, 0.12) 78%,
      rgba(255, 255, 255, 0.22) 100%
    ),
    linear-gradient(
      180deg,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 0) 58%,
      rgba(255, 255, 255, 0.35) 86%,
      rgba(255, 255, 255, 0.72) 100%
    );
}

.login-shell {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  animation: loginFade 0.45s ease both;
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(145deg, var(--login-blue), var(--login-blue-deep));
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(74, 155, 209, 0.24);
}

.login-panel {
  position: absolute;
  top: 18%;
  right: 7%;
  z-index: 4;
  width: min(540px, 38vw);
  max-width: 540px;
  min-width: 420px;
}

.login-card {
  width: 100%;
  padding: 36px 36px 20px;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 16px 40px rgba(58, 104, 140, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.95);
  animation: cardRise 0.5s ease 0.05s both;
}

.card-brand {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 28px;
}

.card-brand h2 {
  margin: 0 0 2px;
  font-size: 18px;
  color: var(--login-blue-deep);
  font-weight: 700;
}

.card-en {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--login-muted);
}

.card-welcome {
  margin: 0;
  font-size: 13px;
  color: #5f7a90;
  line-height: 1.5;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  min-height: 48px;
  box-shadow: 0 0 0 1px #d5e3ee inset;
  background: #fff;
  padding-left: 12px;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b8d0e2 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--login-blue) inset !important;
}

.login-form :deep(.el-input__prefix) {
  color: var(--login-muted);
}

.login-form__action {
  margin-bottom: 6px !important;
  margin-top: 10px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.04em;
  background: linear-gradient(135deg, var(--login-blue) 0%, var(--login-blue-deep) 100%);
  box-shadow: 0 8px 18px rgba(74, 155, 209, 0.26);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(74, 155, 209, 0.32);
}

.login-btn__arrow {
  margin-left: 6px;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--login-muted);
}

.foot-heart {
  display: inline-block;
  width: 9px;
  height: 9px;
  margin-right: 6px;
  background: var(--login-warm);
  transform: rotate(-45deg);
  position: relative;
}

.foot-heart::before,
.foot-heart::after {
  content: '';
  position: absolute;
  width: 9px;
  height: 9px;
  background: inherit;
  border-radius: 50%;
}

.foot-heart::before {
  top: -4.5px;
  left: 0;
}

.foot-heart::after {
  top: 0;
  left: 4.5px;
}

.feature-bar {
  position: absolute;
  left: 4%;
  right: 4%;
  bottom: 14px;
  z-index: 3;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 0;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 7px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 3px 12px rgba(80, 120, 150, 0.07);
}

.feature-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-item--blue .feature-icon {
  color: var(--login-blue);
  background: rgba(74, 155, 209, 0.14);
}

.feature-item--green .feature-icon {
  color: #3fa978;
  background: rgba(102, 200, 155, 0.18);
}

.feature-item--warm .feature-icon {
  color: #d29a2e;
  background: rgba(245, 199, 106, 0.22);
}

.feature-item--purple .feature-icon {
  color: #7f63c7;
  background: rgba(155, 126, 217, 0.16);
}

.feature-copy {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.feature-copy strong {
  font-size: 13px;
  color: var(--login-text);
}

.feature-copy span {
  font-size: 11px;
  color: var(--login-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@keyframes loginFade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes cardRise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1366px) {
  .login-panel {
    right: 5%;
    top: 16%;
    width: min(500px, 40vw);
    min-width: 400px;
  }

  .login-card {
    padding: 30px 28px 16px;
  }
}

@media (max-width: 1100px) {
  .login-panel {
    right: 4%;
    width: min(460px, 44vw);
    min-width: 360px;
  }

  .feature-bar {
    left: 3%;
    right: 3%;
    gap: 10px;
  }
}

@media (max-width: 900px) {
  .login-shell {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 20px 110px;
  }

  .login-panel {
    position: relative;
    top: auto;
    right: auto;
    width: min(480px, 100%);
    min-width: 0;
    margin-top: 48px;
  }

  .feature-bar {
    left: 16px;
    right: 16px;
    bottom: 12px;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .login-panel {
    margin-top: 36px;
  }

  .login-card {
    padding: 24px 18px 14px;
    border-radius: 20px;
  }

  .feature-item {
    min-height: 44px;
    padding: 6px 8px;
  }
}

@media (max-height: 780px) {
  .login-panel {
    top: 12%;
  }

  .login-card {
    padding-top: 26px;
    padding-bottom: 14px;
  }

  .card-brand {
    margin-bottom: 18px;
  }

  .feature-bar {
    bottom: 10px;
  }

  .feature-item {
    min-height: 46px;
  }
}
</style>
