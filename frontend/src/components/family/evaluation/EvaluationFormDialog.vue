<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createOrderEvaluation } from '@/api/evaluation'
import { unwrap, formatDateTime } from '@/utils/familyHome'

const props = defineProps({
  visible: Boolean,
  /** 待评价订单摘要：id / orderNo / serviceName / elderName / ... */
  order: { type: Object, default: null },
  /** 只读查看已有评价 */
  evaluation: { type: Object, default: null },
  mode: {
    type: String,
    default: 'create', // create | view
  },
})

const emit = defineEmits(['update:visible', 'success'])

const open = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
})

const submitting = ref(false)
const form = reactive({
  score: 5,
  content: '',
})

const isView = computed(() => props.mode === 'view' || !!props.evaluation)
const title = computed(() => (isView.value ? '评价详情' : '提交服务评价'))

const displayOrder = computed(() => {
  if (props.order) return props.order
  if (props.evaluation) {
    return {
      id: props.evaluation.serviceOrderId,
      orderNo: props.evaluation.orderNo,
      serviceName: props.evaluation.serviceName,
      elderName: props.evaluation.elderName,
    }
  }
  return null
})

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    if (isView.value && props.evaluation) {
      form.score = props.evaluation.score || 0
      form.content = props.evaluation.content || ''
    } else {
      form.score = 5
      form.content = ''
    }
  },
)

async function onSubmit() {
  if (!props.order?.id) {
    ElMessage.warning('缺少订单信息')
    return
  }
  if (!form.score || form.score < 1 || form.score > 5) {
    ElMessage.warning('请选择 1–5 分评分')
    return
  }
  if (form.content && form.content.length > 500) {
    ElMessage.warning('评价内容不能超过 500 字')
    return
  }

  submitting.value = true
  try {
    const body = { score: form.score }
    if (form.content?.trim()) body.content = form.content.trim()
    const data = unwrap(await createOrderEvaluation(props.order.id, body))
    ElMessage.success('评价提交成功')
    open.value = false
    emit('success', data)
  } catch {
    // 业务错误（含 409 重复评价）已由 request.js 统一提示，避免双提示
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="open"
    :title="title"
    width="480px"
    destroy-on-close
    align-center
  >
    <div v-if="displayOrder" class="order-summary">
      <div>
        <span class="label">服务项目</span>
        <strong>{{ displayOrder.serviceName || '照护服务' }}</strong>
      </div>
      <div>
        <span class="label">服务对象</span>
        <span>{{ displayOrder.elderName || '-' }}</span>
      </div>
      <div>
        <span class="label">订单号</span>
        <span>{{ displayOrder.orderNo || '-' }}</span>
      </div>
      <div v-if="isView && evaluation?.createdAt">
        <span class="label">评价时间</span>
        <span>{{ formatDateTime(evaluation.createdAt) }}</span>
      </div>
    </div>

    <div class="form-block">
      <p class="form-label">服务评分<span v-if="!isView" class="req">*</span></p>
      <el-rate
        v-model="form.score"
        :disabled="isView"
        show-text
        :texts="['很差', '较差', '一般', '满意', '非常满意']"
      />
    </div>

    <div class="form-block">
      <p class="form-label">
        评价内容
        <span class="optional">{{ isView ? '' : '可选，最多 500 字' }}</span>
      </p>
      <el-input
        v-if="!isView"
        v-model="form.content"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="分享本次服务体验，帮助我们做得更好"
      />
      <p v-else class="content-view">
        {{ form.content?.trim() || '（未填写评价内容）' }}
      </p>
    </div>

    <template #footer>
      <el-button @click="open = false">{{ isView ? '关闭' : '取消' }}</el-button>
      <el-button
        v-if="!isView"
        type="primary"
        :loading="submitting"
        @click="onSubmit"
      >
        提交评价
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.order-summary {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: rgba(234, 244, 251, 0.55);
  border: 1px solid rgba(74, 144, 194, 0.12);
}

.order-summary > div {
  display: flex;
  gap: 10px;
  font-size: 13px;
  color: #2c4a5e;
}

.order-summary .label {
  width: 64px;
  flex-shrink: 0;
  color: #8aa0b5;
}

.form-block {
  margin-bottom: 14px;
}

.form-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #4a6072;
  font-weight: 600;
}

.form-label .req {
  color: #e6a23c;
  margin-left: 2px;
}

.form-label .optional {
  margin-left: 8px;
  font-weight: 400;
  font-size: 12px;
  color: #8aa0b5;
}

.content-view {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(240, 243, 246, 0.7);
  font-size: 13px;
  line-height: 1.55;
  color: #4a6072;
  min-height: 72px;
  white-space: pre-wrap;
}
</style>
