<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { createBusinessAccount } from '@/api/systemUser'
import { createCareStaff } from '@/api/careStaff'
import { bindElderFamily } from '@/api/elderFamily'
import { pageElders } from '@/api/admin'
import { buildNamePrefix, previewUsername, randomFiveDigits } from '@/utils/usernamePreview'
import { maskIdCard, maskPhone, elderStatusText } from '@/utils/sensitive'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const step = ref(1)
const roleCode = ref('')
const submitting = ref(false)
const previewDigits = ref(randomFiveDigits())

const form = reactive({
  name: '',
  relationship: '家属',
  isPrimary: 1,
  employeeNo: '',
  phone: '',
  gender: null,
  position: '',
})

const elderKeyword = ref('')
const elderLoading = ref(false)
const elderPage = ref(1)
const elderTotal = ref(0)
const elderList = ref([])
const selectedElder = ref(null)

const previewAccount = computed(() => previewUsername(form.name, previewDigits.value))
const namePrefixOk = computed(() => Boolean(buildNamePrefix(form.name)))

watch(
  () => form.name,
  () => {
    // 姓名变化时刷新预览后缀，避免每次渲染抖动
    previewDigits.value = randomFiveDigits()
  },
)

function regeneratePreview() {
  previewDigits.value = randomFiveDigits()
}

function resetAll() {
  step.value = 1
  roleCode.value = ''
  submitting.value = false
  previewDigits.value = randomFiveDigits()
  form.name = ''
  form.relationship = '家属'
  form.isPrimary = 1
  form.employeeNo = ''
  form.phone = ''
  form.gender = null
  form.position = ''
  elderKeyword.value = ''
  elderPage.value = 1
  elderTotal.value = 0
  elderList.value = []
  selectedElder.value = null
}

function close() {
  visible.value = false
  resetAll()
}

function selectRole(code) {
  roleCode.value = code
  step.value = 2
  if (code === 'FAMILY') {
    loadElders()
  }
}

function backToRole() {
  step.value = 1
}

function calcAge(birthDate) {
  if (!birthDate) return '-'
  const d = new Date(birthDate)
  if (Number.isNaN(d.getTime())) return '-'
  const now = new Date()
  let age = now.getFullYear() - d.getFullYear()
  const m = now.getMonth() - d.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age -= 1
  return age >= 0 ? `${age}岁` : '-'
}

function genderText(g) {
  if (g === 1) return '男'
  if (g === 2) return '女'
  return '-'
}

async function loadElders() {
  elderLoading.value = true
  try {
    const res = await pageElders({
      page: elderPage.value,
      size: 8,
      name: elderKeyword.value?.trim() || undefined,
      status: 1,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载老人失败')
    elderList.value = res.data?.records || []
    elderTotal.value = Number(res.data?.total || 0)
  } catch (e) {
    elderList.value = []
    elderTotal.value = 0
    if (!e?.toastShown) ElMessage.error(e.message || '加载老人失败')
  } finally {
    elderLoading.value = false
  }
}

function searchElders() {
  elderPage.value = 1
  loadElders()
}

function onElderPageChange(p) {
  elderPage.value = p
  loadElders()
}

function pickElder(row) {
  selectedElder.value = row
}

async function showSuccessResult(account, extraTip) {
  const text = [
    `账号：${account.username}`,
    '初始密码：123456',
    '该账号首次登录后必须修改密码。',
    '新密码须包含英文字母和数字（6-20位）。',
    extraTip || '',
  ]
    .filter(Boolean)
    .join('\n')

  try {
    await ElMessageBox.confirm(text, '账号创建成功', {
      confirmButtonText: '复制账号与密码',
      cancelButtonText: '关闭',
      type: 'success',
      distinguishCancelAndClose: true,
    })
    const clip = `账号：${account.username}\n密码：123456`
    try {
      await navigator.clipboard.writeText(clip)
      ElMessage.success('已复制')
    } catch {
      ElMessage.warning('请手动复制账号与密码')
    }
  } catch {
    // 关闭
  }
}

async function submitFamily() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!namePrefixOk.value) {
    ElMessage.warning('请输入有效姓名')
    return
  }
  if (!selectedElder.value?.id) {
    ElMessage.warning('请选择要绑定的老人')
    return
  }

  submitting.value = true
  let created = null
  try {
    const accRes = await createBusinessAccount({
      name: form.name.trim(),
      roleCode: 'FAMILY',
    })
    if (accRes?.code !== 200 || !accRes.data?.userId) {
      throw new Error(accRes?.message || '创建账号失败')
    }
    created = accRes.data

    try {
      const bindRes = await bindElderFamily({
        elderId: selectedElder.value.id,
        familyUserId: created.userId,
        relationship: form.relationship?.trim() || '家属',
        isPrimary: form.isPrimary ?? 1,
      })
      if (bindRes?.code !== 200) {
        throw new Error(bindRes?.message || '绑定失败')
      }
      await showSuccessResult(created, `已绑定老人：${selectedElder.value.name}`)
      emit('success')
      close()
    } catch (bindErr) {
      ElMessage.warning(
        `账号已创建（${created.username}），但老人绑定失败：${bindErr.message || '未知错误'}。请稍后在绑定功能中处理。`,
      )
      emit('success')
      close()
    }
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '开户失败')
  } finally {
    submitting.value = false
  }
}

async function submitCareStaff() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (!namePrefixOk.value) {
    ElMessage.warning('请输入有效姓名')
    return
  }
  if (!form.employeeNo.trim()) {
    ElMessage.warning('请输入工号')
    return
  }

  submitting.value = true
  let created = null
  try {
    const accRes = await createBusinessAccount({
      name: form.name.trim(),
      roleCode: 'CARE_STAFF',
    })
    if (accRes?.code !== 200 || !accRes.data?.userId) {
      throw new Error(accRes?.message || '创建账号失败')
    }
    created = accRes.data

    try {
      const staffRes = await createCareStaff({
        userId: created.userId,
        employeeNo: form.employeeNo.trim(),
        name: form.name.trim(),
        phone: form.phone?.trim() || undefined,
        gender: form.gender ?? undefined,
        position: form.position?.trim() || undefined,
      })
      if (staffRes?.code !== 200) {
        throw new Error(staffRes?.message || '护理员档案创建失败')
      }
      await showSuccessResult(created, `工号：${form.employeeNo.trim()}`)
      emit('success')
      close()
    } catch (staffErr) {
      ElMessage.warning(
        `账号已创建（${created.username}），但护理员档案创建失败：${staffErr.message || '未知错误'}。请稍后补建档案。`,
      )
      emit('success')
      close()
    }
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '开户失败')
  } finally {
    submitting.value = false
  }
}

function onSubmit() {
  if (roleCode.value === 'FAMILY') return submitFamily()
  if (roleCode.value === 'CARE_STAFF') return submitCareStaff()
}
</script>

<template>
  <el-drawer
    v-model="visible"
    title="新建系统账号"
    size="520px"
    destroy-on-close
    @closed="resetAll"
  >
    <div class="create-account">
      <el-steps :active="step" finish-status="success" align-center class="steps">
        <el-step title="选择类型" />
        <el-step :title="roleCode === 'CARE_STAFF' ? '账号与档案' : '账号与绑定'" />
      </el-steps>

      <div v-if="step === 1" class="role-pick">
        <button type="button" class="role-card" @click="selectRole('FAMILY')">
          <strong>家属账号</strong>
          <p>用于查看已绑定老人的健康与照护信息</p>
          <span>FAMILY</span>
        </button>
        <button type="button" class="role-card role-card--staff" @click="selectRole('CARE_STAFF')">
          <strong>护理员账号</strong>
          <p>用于执行照护服务（需再创建护理员档案）</p>
          <span>CARE_STAFF</span>
        </button>
        <p class="tip">不支持通过此入口创建管理员账号。</p>
      </div>

      <div v-else class="form-block">
        <el-button link type="primary" @click="backToRole">← 重新选择类型</el-button>

        <el-form label-position="top" class="form">
          <el-form-item label="姓名" required>
            <el-input v-model="form.name" maxlength="64" placeholder="请输入真实姓名" clearable />
          </el-form-item>

          <div class="preview">
            <div class="preview__row">
              <span>账号预览（仅示意，最终以系统生成为准）</span>
              <el-button
                link
                type="primary"
                :icon="RefreshRight"
                :disabled="!namePrefixOk"
                @click="regeneratePreview"
              >
                换一组数字
              </el-button>
            </div>
            <code>{{ previewAccount || '请先输入有效姓名' }}</code>
            <p>初始密码固定为 123456，首次登录必须修改。</p>
          </div>

          <template v-if="roleCode === 'FAMILY'">
            <el-form-item label="与老人关系">
              <el-input v-model="form.relationship" maxlength="32" placeholder="如：子女" />
            </el-form-item>
            <el-form-item label="主要联系人">
              <el-radio-group v-model="form.isPrimary">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="绑定老人" required>
              <div class="elder-box">
                <div class="elder-box__search">
                  <el-input
                    v-model="elderKeyword"
                    placeholder="按姓名搜索老人"
                    clearable
                    @keyup.enter="searchElders"
                  />
                  <el-button type="primary" @click="searchElders">搜索</el-button>
                </div>
                <div v-loading="elderLoading" class="elder-list">
                  <button
                    v-for="item in elderList"
                    :key="item.id"
                    type="button"
                    class="elder-item"
                    :class="{ 'is-active': selectedElder?.id === item.id }"
                    @click="pickElder(item)"
                  >
                    <strong>{{ item.name }}</strong>
                    <span>
                      {{ genderText(item.gender) }} · {{ calcAge(item.birthDate) }} ·
                      {{ maskPhone(item.phone) }}
                    </span>
                    <em>{{ elderStatusText(item.status) }} · 证件 {{ maskIdCard(item.idCard) }}</em>
                  </button>
                  <div v-if="!elderLoading && !elderList.length" class="empty">暂无老人数据</div>
                </div>
                <el-pagination
                  v-if="elderTotal > 8"
                  small
                  layout="prev, pager, next"
                  :page-size="8"
                  :current-page="elderPage"
                  :total="elderTotal"
                  @current-change="onElderPageChange"
                />
                <div v-if="selectedElder" class="selected">
                  已选：{{ selectedElder.name }}（ID {{ selectedElder.id }}）
                </div>
              </div>
            </el-form-item>
          </template>

          <template v-else>
            <el-form-item label="工号" required>
              <el-input v-model="form.employeeNo" maxlength="32" placeholder="请输入唯一工号" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" maxlength="20" placeholder="选填" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="岗位">
              <el-input v-model="form.position" maxlength="64" placeholder="选填，如护理员" />
            </el-form-item>
          </template>
        </el-form>

        <div class="actions">
          <el-button @click="close">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">创建            创建账号
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.create-account {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
}

.steps {
  margin-bottom: 8px;
}

.role-pick {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-card {
  border: 1px solid rgba(74, 144, 194, 0.18);
  background: #f7fbfe;
  border-radius: 14px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.role-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--ec-shadow-hover);
}

.role-card--staff {
  background: #f4fbf7;
}

.role-card strong {
  display: block;
  font-size: 16px;
  color: var(--ec-text);
  margin-bottom: 6px;
}

.role-card p {
  margin: 0 0 8px;
  color: var(--ec-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.role-card span {
  font-size: 12px;
  color: var(--ec-color-primary);
}

.tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ec-text-muted);
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(74, 144, 194, 0.08);
}

.preview__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ec-text-secondary);
  margin-bottom: 6px;
}

.preview code {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: var(--ec-color-primary-dark);
  letter-spacing: 0.02em;
}

.preview p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--ec-text-muted);
}

.elder-box {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.elder-box__search {
  display: flex;
  gap: 8px;
}

.elder-list {
  min-height: 160px;
  max-height: 260px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.elder-item {
  border: 1px solid rgba(74, 144, 194, 0.15);
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
}

.elder-item.is-active {
  border-color: var(--ec-color-primary);
  background: rgba(74, 144, 194, 0.08);
}

.elder-item strong {
  display: block;
  color: var(--ec-text);
}

.elder-item span,
.elder-item em {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--ec-text-muted);
  font-style: normal;
}

.selected {
  font-size: 13px;
  color: var(--ec-color-secondary);
}

.empty {
  padding: 24px;
  text-align: center;
  color: var(--ec-text-muted);
  font-size: 13px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(74, 144, 194, 0.12);
}
</style>
