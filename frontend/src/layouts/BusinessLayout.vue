<template>
  <div class="business-layout">
    <aside class="sidebar">
      <div class="sidebar__logo">
        <div class="logo-icon">
          <el-icon :size="20" color="#fff"><Connection /></el-icon>
        </div>
        <div class="logo-text">
          <div class="logo-title">农品溯源系统</div>
          <div class="logo-sub">{{ roleName }}</div>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.75)"
        active-text-color="#ffffff"
        class="sidebar__menu"
      >
        <template v-if="userStore.isProducer">
          <el-menu-item index="/prod/dashboard"><el-icon><House /></el-icon><span>工作台</span></el-menu-item>
          <el-menu-item index="/prod/plot"><el-icon><Location /></el-icon><span>地块管理</span></el-menu-item>
          <el-menu-item index="/prod/farming-record"><el-icon><List /></el-icon><span>农事记录</span></el-menu-item>
          <el-menu-item index="/prod/batch"><el-icon><Box /></el-icon><span>生产批次</span></el-menu-item>
          <el-menu-item index="/prod/trace-code"><el-icon><Ticket /></el-icon><span>溯源码管理</span></el-menu-item>
          <el-menu-item index="/prod/org-profile"><el-icon><User /></el-icon><span>主体信息管理</span></el-menu-item>
        </template>
        <template v-if="userStore.isProcessor">
          <el-menu-item index="/proc/dashboard"><el-icon><House /></el-icon><span>工作台</span></el-menu-item>
          <el-menu-item index="/proc/raw-inbound"><el-icon><Download /></el-icon><span>原料入库</span></el-menu-item>
          <el-menu-item index="/proc/quality"><el-icon><Finished /></el-icon><span>质检管理</span></el-menu-item>
          <el-menu-item index="/proc/product-batch"><el-icon><GoodsFilled /></el-icon><span>成品批次与库存</span></el-menu-item>
          <el-menu-item index="/proc/trace-code"><el-icon><Ticket /></el-icon><span>溯源码管理</span></el-menu-item>
          <el-menu-item index="/proc/org-profile"><el-icon><User /></el-icon><span>主体信息管理</span></el-menu-item>
        </template>
        <template v-if="userStore.isDistributor">
          <el-menu-item index="/dist/dashboard"><el-icon><House /></el-icon><span>工作台</span></el-menu-item>
          <el-menu-item index="/dist/inout-record"><el-icon><Sort /></el-icon><span>出入库记录</span></el-menu-item>
          <el-menu-item index="/dist/logistics"><el-icon><Van /></el-icon><span>物流跟踪</span></el-menu-item>
          <el-menu-item index="/dist/sale-record"><el-icon><ShoppingCart /></el-icon><span>终端销售</span></el-menu-item>
          <el-menu-item index="/dist/org-profile"><el-icon><User /></el-icon><span>主体信息管理</span></el-menu-item>
        </template>
      </el-menu>
    </aside>

    <div class="main-wrapper">
      <header class="topbar">
        <div class="topbar__breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar__right">
          <el-button link @click="router.push('/public/query')">
            <el-icon><Search /></el-icon> 公开查询
          </el-button>
          <span class="topbar__username">{{ userStore.userInfo?.realName }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar class="topbar__avatar" size="small">
              {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="main-content">
        <RouterView v-slot="{ Component }">
          <Transition name="fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Connection, House, Location, List, Box, Download, Finished, GoodsFilled, Sort, Van, ShoppingCart, Search, Ticket, User } from '@element-plus/icons-vue'
import { logout } from '@/api/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

const roleName = computed(() => {
  const map = { 2: '生产主体', 3: '加工企业', 4: '流通经销' }
  return map[userStore.userInfo?.role] || '业务端'
})

const currentPageTitle = computed(() => route.meta?.title || '')

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    try { await logout() } catch {}
    userStore.logout()
    router.push('/auth/login')
    ElMessage.success('已退出登录')
  }
}
</script>

<style lang="scss" scoped>
.business-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: $sidebar-width;
  background: $color-bg-sidebar;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  &__logo {
    @include flex(row, flex-start, center);
    gap: $space-3;
    padding: $space-5 $space-4;
    border-bottom: 1px solid rgba(255,255,255,0.08);

    .logo-icon {
      font-size: 22px;
      width: 36px;
      height: 36px;
      background: $color-primary;
      border-radius: $radius-md;
      @include flex(row, center, center);
    }

    .logo-title {
      font-size: $text-md;
      font-weight: $font-bold;
      color: #fff;
    }

    .logo-sub {
      font-size: $text-xs;
      color: rgba(255,255,255,0.45);
      margin-top: 2px;
    }
  }

  &__menu {
    flex: 1;
    padding: $space-2 0;

    :deep(.el-menu-item) {
      margin: 2px $space-2;
      border-radius: $radius-md;
      transition: $transition-fast;

      &:hover { background: rgba(255,255,255,0.08) !important; color: #fff !important; }
      &.is-active { background: $color-primary !important; color: #fff !important; }
    }
  }
}

.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.topbar {
  height: $topbar-height;
  background: $color-bg-card;
  border-bottom: 1px solid $color-border;
  @include flex(row, space-between, center);
  padding: 0 $space-6;
  flex-shrink: 0;
  box-shadow: $shadow-xs;

  &__right {
    @include flex(row, flex-end, center);
    gap: $space-3;
  }

  &__username {
    font-size: $text-sm;
    color: $color-text-secondary;
    font-weight: $font-medium;
  }

  &__avatar {
    background: $color-primary;
    cursor: pointer;
  }
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: $color-bg-base;
}
</style>
