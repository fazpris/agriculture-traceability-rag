<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-header__title">投诉反馈</h2>
    </div>

    <div class="search-bar">
      <el-input v-model="query.traceCode" placeholder="追溯码" clearable style="width: 220px" @keyup.enter="loadData" />
      <el-select v-model="query.status" placeholder="处理状态" clearable style="width: 140px">
        <el-option label="待处理" :value="0" />
        <el-option label="处理中" :value="1" />
        <el-option label="已处理" :value="2" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
    </div>

    <div class="card">
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="traceCode" label="追溯码" width="220" />
        <el-table-column prop="contact" label="联系方式" width="140" />
        <el-table-column prop="content" label="反馈内容" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">查看/处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="padding: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="投诉处理" width="500px">
      <div v-if="currentRow" class="complaint-detail">
        <div class="detail-item"><span class="label">追溯码：</span>{{ currentRow.traceCode }}</div>
        <div class="detail-item"><span class="label">提交时间：</span>{{ currentRow.createdAt }}</div>
        <div class="detail-item"><span class="label">联系人：</span>{{ currentRow.contact }}</div>
        <div class="detail-item"><span class="label">反馈内容：</span>
          <div class="content-box">{{ currentRow.content }}</div>
        </div>
        <el-divider />
        <el-form label-position="top">
          <el-form-item label="处理结果">
            <el-input v-model="handleForm.result" type="textarea" :rows="4" placeholder="请输入处理备注或结果" />
          </el-form-item>
          <el-form-item label="状态更新">
            <el-radio-group v-model="handleForm.status">
              <el-radio :label="1">处理中</el-radio>
              <el-radio :label="2">已处理</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitHandle">提交处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, traceCode: '', status: null })
const dialogVisible = ref(false)
const currentRow = ref(null)
const handleForm = reactive({ result: '', status: 1 })

const statusMap = { 0: '待处理', 1: '处理中', 2: '已处理' }
const statusLabel = (s) => statusMap[s] || '-'
const statusType = (s) => ({ 0: 'danger', 1: 'warning', 2: 'success' }[s] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    // 假设 API 路径
    const res = await request.get('/sys/complaint/list', { params: query })
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleDetail = (row) => {
  currentRow.value = row
  handleForm.result = row.handleResult || ''
  handleForm.status = row.status || 1
  dialogVisible.value = true
}

const submitHandle = async () => {
  try {
    await request.post(`/sys/complaint/${currentRow.value.id}/handle`, handleForm)
    ElMessage.success('处理成功')
    dialogVisible.value = false
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.complaint-detail {
  .detail-item {
    margin-bottom: 12px;
    font-size: 14px;
    .label { font-weight: 600; color: #5C6475; width: 80px; display: inline-block; }
  }
  .content-box {
    margin-top: 8px;
    padding: 12px;
    background: #F8FAFB;
    border-radius: 8px;
    color: #1A1F2E;
    line-height: 1.6;
  }
}
</style>
