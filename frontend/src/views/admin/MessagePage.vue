<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-header__title">消息中心</h2>
    </div>

    <div class="card">
      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="全部消息" name="all" />
        <el-tab-pane label="未读消息" name="unread" />
        <el-tab-pane label="已读消息" name="read" />
      </el-tabs>

      <el-table :data="tableData" stripe v-loading="loading" style="width: 100%">
        <el-table-column width="60">
          <template #default="{ row }">
            <el-badge is-dot :hidden="row.status === 1" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeConfig[row.type].color">{{ typeConfig[row.type].label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发送时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleRead(row)">
              {{ row.status === 0 ? '标为已读' : '查看详情' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="padding:16px; display:flex; justify-content:flex-end">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="消息详情" width="480px">
      <div v-if="currentMsg" class="msg-detail">
        <h3>{{ currentMsg.title }}</h3>
        <p class="time">{{ currentMsg.createdAt }}</p>
        <div class="content">{{ currentMsg.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyMessages, markMessageRead } from '@/api/sys'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const activeTab = ref('unread')
const detailVisible = ref(false)
const currentMsg = ref(null)

const query = reactive({ page: 1, size: 10 })

const typeConfig = {
  0: { label: '系统消息', color: 'info' },
  1: { label: '审核通知', color: 'primary' },
  2: { label: '投诉提醒', color: 'warning' },
}

const loadData = async () => {
  loading.value = true
  const params = { ...query }
  if (activeTab.value === 'unread') params.status = 0
  else if (activeTab.value === 'read') params.status = 1
  
  try {
    const res = await getMyMessages(params)
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const handleRead = async (row) => {
  if (row.status === 0) {
    await markMessageRead(row.id)
    row.status = 1
  }
  currentMsg.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.msg-detail {
  padding: 10px 0;
  h3 { margin-bottom: 8px; font-weight: 700; color: #333; }
  .time { font-size: 12px; color: #999; margin-bottom: 20px; }
  .content { line-height: 1.6; color: #666; white-space: pre-wrap; }
}
</style>
