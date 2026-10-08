<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { pageSystemUsers } from '@/api/systemUser'
import { bindElderFamily } from '@/api/elderFamily'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  elderId: { type: Number, required: true },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(8)
const total = ref(0)
const users = ref([])
const selected = ref(null)

const form = reactive({
  relationship: '家属',
  isPrimary: 0,
})

function reset() {
  keyword.value = ''
  page.value = 1
  total.value = 0
  users.value = []
  selected.value = null
  form.relationship = '家属'
  form.isPrimary = 0
}

async function loadUsers() {
  loading.value = true
  try {
    const kw = keyword.value.trim()
    const params = {
      page: page.value,
      size: size.value,
      roleCode: 'FAMILY',
      status: 1,
    }
    // 优先按姓名；纯账号风格关键字走 username
    if (kw) {
      if (/^[a-zA-Z0-9@_.-]+$/.test(kw)) params.username = kw
      else params.realName = kw
    }
    const res = await pageSystemUsers(params)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    users.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    users.value = []
    total.value = 0
    ElMessage.error(e.message || '加载家属账号失败')
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      reset()
      return
    }
    reset()
    loadUsers()
  },
)

function onSearch() {
  page.value = 1
  selected.value = null
  loadUsers()
}

function onPageChange(p) {
  page.value = p
  loadUsers()
}

function selectUser(row) {
  selected.value = row
}

function close() {
  visible.value = false
}

async function submit() {
  if (!selected.value?.id) {
    ElMessage.warning('请选择家属账号')
    return
  }
  submitting.value = true
  try {
    const res = await bindElderFamily({
      elderId: props.elderId,
      familyUserId: selected.value.id,
      relationship: form.relationship.trim() || undefined,
      isPrimary: form.isPrimary ? 1 : 0,
    })
    if (res?.code !== 200) throw new Error(res?.message || '绑定失败')
    ElMessage.success('绑定成功')
    emit('success')
    close()
  } catch (e) {
    ElMessage.error(e.message || '绑定失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" title="绑定家属账号" size="480px" destroy-on-close>
    <div class="bind-body">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅可绑定角色为 FAMILY 且已启用的账号。绑定前请先在用户管理中创建家属账号。"
        class="tip"
      />

      <div class="search-row">
        <el-input
          v-model="keyword"
          clearable
          placeholder="按姓名或用户名搜索"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" :icon="Search" @click="onSearch">搜索</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="users"
        stripe
        highlight-current-row
        empty-text="暂无家属账号"
        max-height="280"
        @current-change="selectUser"
      >
        <el-table-column width="48">
          <template #default="{ row }">
            <el-radio :model-value="selected?.id" :value="row.id" @change="selectUser(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
      </el-table>

      <div class="pager">
        <el-pagination
          small
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="onPageChange"
        />
      </div>

      <el-form label-width="90px" class="extra">
        <el-form-item label="关系">
          <el-input v-model="form.relationship" maxlength="32" placeholder="如：子女" />
        </el-form-item>
        <el-form-item label="主联系人">
          <el-switch v-model="form.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认绑定</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.bind-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip {
  border-radius: 10px;
}

.search-row {
  display: flex;
  gap: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
}

.extra {
  margin-top: 4px;
}
</style>
