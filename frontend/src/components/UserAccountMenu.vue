<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  roleHint: {
    type: String,
    default: '',
  },
  showAvatar: {
    type: Boolean,
    default: false,
  },
  /** 显式指定个人中心路径；不传则按角色推断 */
  profilePath: {
    type: String,
    default: '',
  },
})

const router = useRouter()
const userStore = useUserStore()

const name = computed(() => userStore.displayName)
const avatarUrl = computed(() => userStore.avatarUrl)
const avatarLetter = computed(() => {
  const n = name.value || '用'
  return String(n).trim().charAt(0) || '用'
})

const resolvedProfilePath = computed(() => {
  if (props.profilePath) return props.profilePath
  if (userStore.hasRole('ADMIN') && router.currentRoute.value.path.startsWith('/admin')) {
    return '/admin/profile'
  }
  if (userStore.hasRole('CARE_STAFF') && router.currentRoute.value.path.startsWith('/staff')) {
    return '/staff/profile'
  }
  if (userStore.hasRole('FAMILY') && router.currentRoute.value.path.startsWith('/family')) {
    return '/family/profile'
  }
  if (userStore.hasRole('ADMIN')) return '/admin/profile'
  if (userStore.hasRole('CARE_STAFF')) return '/staff/profile'
  if (userStore.hasRole('FAMILY')) return '/family/profile'
  return ''
})

const canSwitch = computed(() => {
  return userStore.hasRole('FAMILY') && userStore.hasRole('CARE_STAFF')
})

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出登录？', '提示', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
    userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
    return
  }

  if (cmd === 'profile') {
    if (resolvedProfilePath.value) {
      await router.push(resolvedProfilePath.value)
    } else {
      ElMessage.info('暂无个人中心入口')
    }
    return
  }

  if (cmd === 'change-password') {
    await router.push('/change-password')
    return
  }

  if (cmd === 'switch-family') {
    const path = userStore.switchRole('FAMILY')
    if (path) router.push(path)
    return
  }

  if (cmd === 'switch-staff') {
    const path = userStore.switchRole('CARE_STAFF')
    if (path) router.push(path)
  }
}
</script>

<template>
  <div class="user-account">
    <span v-if="roleHint" class="user-account__hint">{{ roleHint }}</span>
    <el-dropdown trigger="click" @command="handleCommand">
      <button type="button" class="user-account__trigger">
        <span v-if="showAvatar" class="user-account__avatar">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" class="user-account__avatar-img" />
          <template v-else>{{ avatarLetter }}</template>
        </span>
        <span class="user-account__name">{{ name }}</span>
        <el-icon class="user-account__arrow"><ArrowDown /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">个人信息</el-dropdown-item>
          <el-dropdown-item command="change-password">修改密码</el-dropdown-item>
          <template v-if="canSwitch">
            <el-dropdown-item divided disabled>切换身份</el-dropdown-item>
            <el-dropdown-item command="switch-family">家属端</el-dropdown-item>
            <el-dropdown-item command="switch-staff">护理员端</el-dropdown-item>
          </template>
          <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped>
.user-account {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-account__hint {
  font-size: 12px;
  color: var(--ec-color-primary);
  background: rgba(74, 144, 194, 0.1);
  padding: 4px 10px;
  border-radius: 12px;
}

.user-account__trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #2c4a5e;
  font-size: 13px;
  padding: 4px 0;
}

.user-account__trigger:hover {
  color: var(--ec-color-primary);
}

.user-account__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4a90c2, #69b98c);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  overflow: hidden;
}

.user-account__avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-account__name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.user-account__arrow {
  font-size: 12px;
  color: #8aa0b5;
}
</style>
