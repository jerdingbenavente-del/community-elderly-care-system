<script setup>
import { useRouter } from 'vue-router'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import BookingSummary from './BookingSummary.vue'

const props = defineProps({
  elderName: String,
  serviceName: String,
  scheduledStartTime: String,
  durationMinutes: [Number, String],
  price: [Number, String],
  remark: String,
  status: String,
  orderId: { type: [Number, String], default: null },
  paymentStatus: { type: String, default: 'UNPAID' },
})

const router = useRouter()

function goHome() {
  router.push('/family/care-services')
}

function goPay() {
  if (props.orderId) {
    router.push(`/family/orders/${props.orderId}/pay`)
    return
  }
  router.push('/family/orders')
}

function goOrders() {
  if (props.orderId) {
    router.push({ path: '/family/orders', query: { id: String(props.orderId) } })
    return
  }
  router.push('/family/orders')
}
</script>

<template>
  <section class="success">
    <el-icon class="success__icon" :size="56" color="#69b98c"><CircleCheckFilled /></el-icon>
    <h2>服务预约成功</h2>
    <p class="success__desc">
      您的服务预约已提交。<br />请先完成模拟支付，再等待管理员确认。
    </p>
    <BookingSummary
      class="success__summary"
      :elder-name="elderName"
      :service-name="serviceName"
      :scheduled-start-time="scheduledStartTime"
      :duration-minutes="durationMinutes"
      :price="price"
      :remark="remark"
      :status="status"
    />
    <div class="success__actions">
      <el-button
        v-if="orderId && paymentStatus !== 'PAID'"
        type="primary"
        @click="goPay"
      >
        去支付
      </el-button>
      <el-button :type="orderId && paymentStatus !== 'PAID' ? 'default' : 'primary'" @click="goOrders">
        查看我的订单
      </el-button>
      <el-button @click="goHome">返回服务首页</el-button>
    </div>
  </section>
</template>

<style scoped>
.success {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 32px 24px;
  text-align: center;
}

.success__icon {
  margin-bottom: 8px;
}

.success h2 {
  margin: 0;
  font-size: 22px;
  color: #2c4a5e;
}

.success__desc {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: #718096;
}

.success__summary {
  margin-top: 22px;
  text-align: left;
}

.success__actions {
  margin-top: 22px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
