<script setup>
import UserProfileView from '@/views/common/UserProfileView.vue'
import { profilePageDecor } from '@/config/familyImages'
</script>

<template>
  <div class="family-profile">
    <div
      class="family-profile__decor"
      :style="{
        backgroundImage: `url(${profilePageDecor.image})`,
        backgroundPosition: profilePageDecor.objectPosition,
      }"
      aria-hidden="true"
    />
    <UserProfileView variant="family" />
  </div>
</template>

<style scoped>
.family-profile {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.family-profile__decor {
  height: 88px;
  border-radius: 16px;
  overflow: hidden;
  background-size: cover;
  background-repeat: no-repeat;
  box-shadow: 0 6px 18px rgba(80, 120, 150, 0.06);
  opacity: 0.92;
}
</style>
