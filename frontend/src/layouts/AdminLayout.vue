<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar__logo">
        <div class="logo-container" :class="{ collapsed: isCollapsed }">
          <el-icon :size="24" color="#1A7A4A"><Connection /></el-icon>
          <span v-show="!isCollapsed">农品溯源 · 管理端</span>
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
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>系统概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/register-audit">
          <el-icon><DocumentChecked /></el-icon><span>注册审核</span>
          <el-badge v-if="pendingCount > 0" :value="pendingCount" class="menu-badge" />
        </el-menu-item>
        <el-menu-item index="/admin/batch-audit">
          <el-icon><CircleCheck /></el-icon><span>生产批次审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/inbound-audit">
          <el-icon><CopyDocument /></el-icon><span>原料入库审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/trace-code">
          <el-icon><Ticket /></el-icon><span>追溯码管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/org-inventory-link">
          <el-icon><Link /></el-icon><span>库存核销配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/complaint">
          <el-icon><ChatLineSquare /></el-icon><span>投诉反馈</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部栏 -->
      <header class="topbar">
        <div class="topbar__breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar__right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="topbar-message">
            <el-icon @click="router.push('/admin/message')"><Bell /></el-icon>
          </el-badge>
          <span class="topbar__username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
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

      <!-- 页面内容 -->
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { 
  DataAnalysis, User, DocumentChecked, CircleCheck,
  Ticket, ChatLineSquare, Bell, Connection, Link, CopyDocument
} from '@element-plus/icons-vue'
import { logout } from '@/api/auth'
import { getMyMessages } from '@/api/sys'
import { ElMessage } from 'element-plus'
import { onMounted, onUnmounted } from 'vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const pendingCount = ref(0)

const pageTitleMap = {
  '/admin/dashboard': '系统概览',
  '/admin/user': '用户管理',
  '/admin/register-audit': '注册审核',
  '/admin/batch-audit': '生产批次审核',
  '/admin/inbound-audit': '原料入库审核',
  '/admin/trace-code': '追溯码管理',
  '/admin/org-inventory-link': '库存核销配置',
  '/admin/complaint': '投诉反馈',
}
const currentPageTitle = computed(() => pageTitleMap[route.path] || '后台管理')

const unreadCount = ref(0)
let timer = null

const loadUnreadCount = async () => {
  try {
    const res = await getMyMessages({ status: 0, size: 1 })
    unreadCount.value = res.total || 0
  } catch {}
}

onMounted(() => {
  loadUnreadCount()
  // 每30秒检查一次消息
  timer = setInterval(loadUnreadCount, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

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
.admin-layout {
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
  overflow: hidden;

  &__logo {
    @include flex(row, flex-start, center);
    gap: $space-3;
    padding: $space-5 $space-4;
    border-bottom: 1px solid rgba(255,255,255,0.08);

    .logo-icon {
      font-size: 24px;
      width: 36px;
      height: 36px;
      background: $color-primary;
      border-radius: $radius-md;
      @include flex(row, center, center);
    }

    .logo-title {
      font-size: $text-md;
      font-weight: $font-bold;
      color: $color-text-white;
      letter-spacing: 0.5px;
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
    overflow-y: auto;

    :deep(.el-menu-item) {
      margin: 2px $space-2;
      border-radius: $radius-md;
      transition: $transition-fast;
      position: relative;

      &:hover {
        background: rgba(255,255,255,0.08) !important;
        color: #fff !important;
      }

      &.is-active {
        background: $color-primary !important;
        color: #fff !important;
      }
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
    font-weight: $font-semibold;
  }
}

.topbar-message {
  cursor: pointer;
  @include flex(row, center, center);
  padding: 4px;
  border-radius: $radius-sm;
  transition: $transition-fast;
  color: $color-text-secondary;
  margin-right: $space-2;

  &:hover {
    background: #F3F4F6;
    color: $color-primary;
  }
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: $color-bg-base;
}

.menu-badge {
  position: absolute;
  right: 16px;
}
</style>
