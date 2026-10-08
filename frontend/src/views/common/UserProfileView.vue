<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Camera, Lock, UserFilled } from '@element-plus/icons-vue'
import { getCurrentUserProfile } from '@/api/auth'
import { updateMyProfile, uploadMyAvatar } from '@/api/systemUser'
import { getMyCareStaff } from '@/api/careStaff'
import { toastIfNeeded } from '@/api/request'
import { useUserStore } from '@/stores/user'

/** variant 仅用于三端包装页区分，行为由当前登录角色决定 */
defineProps({
  variant: {
    type: String,
    default: '',
  },
})

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const loadError = ref('')
const profile = ref(null)
const careStaff = ref(null)
const fileInputRef = ref(null)

const form = reactive({
  realName: '',
  phone: '',
})

const roleLabel = computed(() => {
  if (userStore.hasRole('ADMIN')) return '管理员'
  if (userStore.hasRole('CARE_STAFF')) return '护理员'
  if (userStore.hasRole('FAMILY')) return '家属'
  return (profile.value?.roles || []).join('、') || '-'
})

const isCareStaff = computed(() => userStore.hasRole('CARE_STAFF'))

const avatarUrl = computed(() => profile.value?.avatar || userStore.userInfo?.avatar || '')

const avatarLetter = computed(() => {
  const n = form.realName || profile.value?.realName || userStore.displayName || '用'
  return String(n).trim().charAt(0) || '用'
})

function applyProfile(data) {
  profile.value = data
  form.realName = data?.realName || ''
  form.phone = data?.phone || ''
  userStore.applyProfile(data)
}

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  careStaff.value = null
  try {
    const res = await getCurrentUserProfile()
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    applyProfile(res.data)

    if (isCareStaff.value) {
      try {
        const staffRes = await getMyCareStaff()
        if (staffRes?.code === 200) {
          careStaff.value = staffRes.data || null
        }
      } catch {
        careStaff.value = null
      }
    }
  } catch (e) {
    loadError.value = e.message || '加载个人资料失败'
    profile.value = null
  } finally {
    loading.value = false
  }
}

async function onSave() {
  const realName = form.realName.trim()
  if (!realName) {
    ElMessage.warning('姓名不能为空')
    return
  }
  const phone = form.phone.trim()
  if (phone && !/^1\d{10}$/.test(phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }

  saving.value = true
  try {
    const res = await updateMyProfile({
      realName,
      phone: phone || '',
    })
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    applyProfile(res.data)
    if (isCareStaff.value) {
      await loadProfile()
    }
    ElMessage.success('资料已保存')
  } catch (e) {
    toastIfNeeded(e, '保存失败')
  } finally {
    saving.value = false
  }
}

function pickAvatar() {
  fileInputRef.value?.click()
}

async function onAvatarSelected(ev) {
  const file = ev.target?.files?.[0]
  ev.target.value = ''
  if (!file) return

  const allowed = ['image/jpeg', 'image/png', 'image/webp']
  const nameOk = /\.(jpe?g|png|webp)$/i.test(file.name || '')
  if (!allowed.includes(file.type) && !nameOk) {
    ElMessage.warning('仅支持 jpg/jpeg/png/webp 图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像文件不能超过 2MB')
    return
  }

  uploading.value = true
  try {
    const res = await uploadMyAvatar(file)
    if (res?.code !== 200) throw new Error(res?.message || '上传失败')
    applyProfile(res.data)
    ElMessage.success('头像已更新')
  } catch (e) {
    toastIfNeeded(e, '头像上传失败')
  } finally {
    uploading.value = false
  }
}

function goChangePassword() {
  router.push('/change-password')
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>个人中心</h2>
        <p>查看并修改头像、姓名与手机号；修改密码请使用专用入口。</p>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="err"
      :title="loadError"
    >
      <template #default>
        <el-button type="primary" link @click="loadProfile">重新加载</el-button>
      </template>
    </el-alert>

    <div v-if="profile" class="profile-card">
      <div class="avatar-block">
        <button
          type="button"
          class="avatar-btn"
          :disabled="uploading"
          title="点击更换头像"
          @click="pickAvatar"
        >
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar-img" />
          <span v-else class="avatar-letter">{{ avatarLetter }}</span>
          <span class="avatar-mask">
            <el-icon :size="18"><Camera /></el-icon>
            <em>{{ uploading ? '上传中…' : '更换' }}</em>
          </span>
        </button>
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
          class="file-input"
          @change="onAvatarSelected"
        />
        <div class="avatar-meta">
          <strong>{{ form.realName || profile.realName || profile.username }}</strong>
          <span>{{ roleLabel }}</span>
        </div>
      </div>

      <el-form label-width="96px" class="profile-form" @submit.prevent>
        <el-form-item label="用户名">
          <el-input :model-value="profile.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <div class="role-row">
            <el-tag
              v-for="r in profile.roles || []"
              :key="r"
              size="small"
              class="role-tag"
            >
              {{ r }}
            </el-tag>
            <span v-if="!(profile.roles || []).length">-</span>
          </div>
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" maxlength="64" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="11" placeholder="选填，11 位手机号" clearable />
        </el-form-item>

        <template v-if="isCareStaff">
          <el-form-item label="护理员工号">
            <el-input :model-value="careStaff?.employeeNo || '-'" disabled />
          </el-form-item>
          <el-form-item label="业务姓名">
            <el-input :model-value="careStaff?.name || '-'" disabled />
            <div class="hint">修改上方「姓名」并保存后，会同步更新护理员业务姓名。</div>
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存资料</el-button>
          <el-button :icon="Lock" @click="goChangePassword">修改密码</el-button>
          <el-button :icon="UserFilled" @click="loadProfile">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 720px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--ec-text);
}

.page-head p {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.err {
  border-radius: 12px;
}

.profile-card {
  padding: 22px 24px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--ec-shadow);
}

.avatar-block {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(74, 144, 194, 0.12);
}

.avatar-btn {
  position: relative;
  width: 88px;
  height: 88px;
  border: none;
  border-radius: 50%;
  padding: 0;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(135deg, #4a90c2, #69b98c);
  flex-shrink: 0;
}

.avatar-btn:disabled {
  cursor: wait;
  opacity: 0.85;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-letter {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: rgba(20, 40, 60, 0.55);
  color: #fff;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.avatar-btn:hover .avatar-mask,
.avatar-btn:focus-visible .avatar-mask {
  opacity: 1;
}

.avatar-mask em {
  font-style: normal;
  font-size: 12px;
}

.file-input {
  display: none;
}

.avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.avatar-meta strong {
  font-size: 18px;
  color: var(--ec-text);
}

.avatar-meta span {
  font-size: 13px;
  color: var(--ec-text-muted);
}

.profile-form {
  max-width: 480px;
}

.role-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  min-height: 32px;
}

.role-tag {
  margin: 0;
}

.hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ec-text-muted);
  line-height: 1.5;
}

@media (max-width: 640px) {
  .profile-card {
    padding: 16px;
  }

  .avatar-block {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
