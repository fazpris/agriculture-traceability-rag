<template>
  <div class="public-layout">
    <header class="public-header" v-if="showHeader">
      <div class="header-inner">
        <div class="logo">
          <el-icon :size="24" color="#1A7A4A"><Connection /></el-icon>
          <span>农品溯源 · 农产品溯源</span>
        </div>
        <div class="header-actions">
          <el-button v-if="!userStore.isLoggedIn" @click="router.push('/auth/login')" type="primary" size="small">
            登录 / 注册
          </el-button>
          <el-button v-else @click="handleEnterBusiness" type="primary" size="small" :loading="entering">
            进入业务系统
          </el-button>
        </div>
      </div>
    </header>
    <main class="public-main">
      <RouterView />
    </main>

    <!-- 全局 AI 悬浮小人助手 -->
    <AiAssistant :productCode="productCode" />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Connection } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AiAssistant from '@/components/AiAssistant.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const showHeader = computed(() => !route.path.includes('/auth'))
const entering = ref(false)

// 动态获取当前页面可能存在的溯源吗，用于 AI 上下文
const productCode = computed(() => route.query.code || '')

const handleEnterBusiness = async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    entering.value = true
    try {
      await userStore.fetchMe()
    } catch {
      userStore.clearToken()
      router.push('/auth/login')
      return
    } finally {
      entering.value = false
    }
  }
  
  if (!userStore.userInfo?.role) {
    ElMessage.error('账号异常：未绑定角色权限')
    return
  }
  
  router.push(userStore.homePath)
}
</script>

<style lang="scss" scoped>
.public-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.public-header {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.96) 100%);
  backdrop-filter: blur(14px) saturate(1.2);
  border-bottom: 1px solid rgba($color-border, 0.85);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);

  .header-inner {
    max-width: 960px;
    margin: 0 auto;
    padding: 0 $space-6;
    height: $topbar-height;
    @include flex(row, space-between, center);
  }

  .logo {
    @include flex(row, flex-start, center);
    gap: $space-3;
    cursor: default;
    user-select: none;

    span {
      font-size: $text-md;
      font-weight: $font-bold;
      color: $color-text-primary;
      letter-spacing: -0.2px;
    }
  }
}

.public-main {
  flex: 1;
}

@media (max-width: 768px) {
  .public-header {
    box-shadow: 0 1px 0 rgba(15, 23, 42, 0.06);

    .header-inner {
      max-width: none;
      padding: 0 12px;
      min-height: 52px;
      height: auto;
      padding-top: 10px;
      padding-bottom: 10px;
      gap: 10px;
    }

    .logo {
      min-width: 0;
      flex: 1;
      gap: $space-2;

      span {
        font-size: 13px;
        line-height: 1.3;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .header-actions {
      flex-shrink: 0;
    }
  }
}
</style>
