<script setup>
import { computed, onMounted, reactive } from 'vue'
import { listMyElders } from '@/api/elder'
import { listFamilyHealthRecords, listFamilyHealthWarnings } from '@/api/health'
import { listServiceOrders } from '@/api/serviceOrder'
import { unwrap } from '@/utils/familyHome'
import HeroCarousel from '@/components/family/HeroCarousel.vue'
import QuickActions from '@/components/family/QuickActions.vue'
import AlertBanner from '@/components/family/AlertBanner.vue'
import ElderOverview from '@/components/family/ElderOverview.vue'
import HealthOverview from '@/components/family/HealthOverview.vue'
import ServiceOverview from '@/components/family/ServiceOverview.vue'

const eldersState = reactive({
  loading: true,
  error: '',
  list: [],
})

const healthState = reactive({
  loading: true,
  error: '',
  record: null,
  elderName: '',
})

const alertState = reactive({
  loading: true,
  error: '',
  unhandledCount: null,
})

const orderState = reactive({
  loading: true,
  error: '',
  empty: false,
  stats: {
    PENDING: null,
    CONFIRMED: null,
    IN_SERVICE: null,
    COMPLETED: null,
  },
})

const hasUnhandledWarning = computed(() => (alertState.unhandledCount || 0) > 0)

const MAX_ELDERS_FOR_DETAIL = 5

async function loadElders() {
  eldersState.loading = true
  eldersState.error = ''
  try {
    const data = unwrap(await listMyElders())
    eldersState.list = Array.isArray(data) ? data : []
  } catch (e) {
    eldersState.error = e.message || '获取老人数据失败'
    eldersState.list = []
  } finally {
    eldersState.loading = false
  }
}

async function loadHealthAndAlerts(elders) {
  healthState.loading = true
  alertState.loading = true
  healthState.error = ''
  alertState.error = ''
  healthState.record = null
  healthState.elderName = ''
  alertState.unhandledCount = null

  if (!elders.length) {
    healthState.loading = false
    alertState.loading = false
    alertState.unhandledCount = 0
    return
  }

  const targets = elders.slice(0, MAX_ELDERS_FOR_DETAIL)

  const healthSettled = await Promise.allSettled(
    targets.map((e) => listFamilyHealthRecords(e.id, { page: 1, size: 1 })),
  )
  const warningSettled = await Promise.allSettled(
    targets.map((e) => listFamilyHealthWarnings(e.id)),
  )

  let latest = null
  let latestElderName = ''
  let healthFail = 0
  healthSettled.forEach((r, idx) => {
    if (r.status !== 'fulfilled') {
      healthFail += 1
      return
    }
    try {
      const list = unwrap(r.value)
      const first = Array.isArray(list) && list.length ? list[0] : null
      if (!first) return
      if (
        !latest ||
        String(first.measuredAt || '') > String(latest.measuredAt || '')
      ) {
        latest = first
        latestElderName = targets[idx].name || first.elderName || ''
      }
    } catch {
      healthFail += 1
    }
  })

  if (latest) {
    healthState.record = latest
    healthState.elderName = latestElderName
  } else if (healthFail === targets.length) {
    healthState.error = '获取健康数据失败'
  }

  let unhandled = 0
  let warnFail = 0
  warningSettled.forEach((r) => {
    if (r.status !== 'fulfilled') {
      warnFail += 1
      return
    }
    try {
      const list = unwrap(r.value)
      if (!Array.isArray(list)) return
      unhandled += list.filter((w) => w.status === 'UNHANDLED').length
    } catch {
      warnFail += 1
    }
  })

  if (warnFail === targets.length) {
    alertState.error = '获取预警数据失败'
  } else {
    alertState.unhandledCount = unhandled
  }

  healthState.loading = false
  alertState.loading = false
}

async function loadOrders() {
  orderState.loading = true
  orderState.error = ''
  orderState.empty = false
  orderState.stats = {
    PENDING: null,
    CONFIRMED: null,
    IN_SERVICE: null,
    COMPLETED: null,
  }
  try {
    const page = unwrap(await listServiceOrders({ page: 1, size: 100 }))
    const records = page?.records || []
    orderState.empty = records.length === 0
    orderState.stats = {
      PENDING: records.filter((o) => o.status === 'PENDING').length,
      CONFIRMED: records.filter((o) => o.status === 'CONFIRMED').length,
      IN_SERVICE: records.filter((o) => o.status === 'IN_SERVICE').length,
      COMPLETED: records.filter((o) => o.status === 'COMPLETED').length,
    }
  } catch (e) {
    orderState.error = e.message || '获取服务订单失败'
  } finally {
    orderState.loading = false
  }
}

async function loadHome() {
  await loadElders()
  await Promise.all([
    loadHealthAndAlerts(eldersState.list),
    loadOrders(),
  ])
}

onMounted(() => {
  loadHome()
})
</script>

<template>
  <div class="family-home">
    <div class="family-home__top">
      <section class="family-home__center">
        <HeroCarousel />
        <QuickActions />
      </section>

      <aside class="family-home__right">
        <ElderOverview
          :loading="eldersState.loading"
          :error="eldersState.error"
          :elders="eldersState.list"
          :has-unhandled-warning="hasUnhandledWarning"
        />
        <HealthOverview
          :loading="healthState.loading"
          :error="healthState.error"
          :record="healthState.record"
          :elder-name="healthState.elderName"
        />
        <ServiceOverview
          :loading="orderState.loading"
          :error="orderState.error"
          :empty="orderState.empty"
          :stats="orderState.stats"
        />
      </aside>
    </div>

    <AlertBanner
      class="family-home__alert"
      :loading="alertState.loading"
      :error="alertState.error"
      :unhandled-count="alertState.unhandledCount"
    />
  </div>
</template>

<style scoped>
.family-home {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.family-home__top {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--ec-right-panel, 320px);
  gap: 16px;
  align-items: start;
}

.family-home__center {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.family-home__right {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 8px;
}

.family-home__alert {
  width: 100%;
}

@media (max-width: 1200px) {
  .family-home__top {
    grid-template-columns: 1fr;
  }

  .family-home__right {
    position: static;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .family-home__right {
    grid-template-columns: 1fr;
  }
}
</style>
