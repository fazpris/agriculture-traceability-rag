<template>
  <div class="page-container prod-page">
    <div class="page-header">
      <div class="page-header__title-wrap">
        <h2 class="page-header__title">生产批次审核</h2>
        <p class="page-header__subtitle">审核生产主体提交的生产批次；通过后加工企业可在原料入库中引用。</p>
      </div>
      <div class="page-header__actions">
        <el-tooltip content="刷新" placement="bottom">
          <el-button :icon="Refresh" circle plain @click="loadData" />
        </el-tooltip>
      </div>
    </div>

    <div class="prod-toolbar">
      <el-select v-model="query.status" placeholder="审核状态" clearable style="width: 160px">
        <el-option label="待审核" :value="0" />
        <el-option label="审核通过" :value="1" />
        <el-option label="审核驳回" :value="2" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="onFilterChange">应用筛选</el-button>
      <el-button @click="resetFilter">重置为待审核</el-button>
    </div>

    <div class="prod-surface">
      <div class="prod-surface__bar">
        <span class="prod-surface__hint">共 {{ total }} 条</span>
      </div>
      <el-table v-if="tableData.length || loading" :data="tableData" class="prod-table" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono-num">{{ row.batchNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="plotId" label="地块" width="100" align="center">
          <template #default="{ row }">
            <span class="mono-num">{{ row.plotId ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收获数量" width="120" align="right">
          <template #default="{ row }">
            <span class="mono-num">{{ row.quantity }}</span>
            <span>{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="112" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="light" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="168" show-overflow-tooltip />
        <el-table-column prop="auditorName" label="审核人" width="100" show-overflow-tooltip />
        <el-table-column prop="auditAt" label="审核时间" width="168" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div v-if="row.status === 0" class="op-actions">
              <el-button link type="primary" size="small" @click="handleAudit(row, 1)">通过</el-button>
              <el-button link type="danger" size="small" @click="handleAudit(row, 2)">驳回</el-button>
            </div>
            <span v-else class="text-muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="prod-empty">
        <el-empty description="暂无待处理批次" :image-size="120" />
      </div>
      <div class="prod-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadData"
          @size-change="onPageSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="rejectVisible" title="驳回批次" width="440px" destroy-on-close>
      <el-form label-width="0" label-position="top">
        <el-form-item label="请输入具体驳回原因" required>
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="例如：地块信息与实际不符 / 数量超出逻辑范围"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBatchAuditList, auditProdBatch } from '@/api/prod'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: 0 })

const rejectVisible = ref(false)
const rejectReason = ref('')
const currentAuditRow = ref(null)

const statusLabel = (s) => ({ 0: '待审核', 1: '审核通过', 2: '审核驳回' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

const normalizePage = (data) => {
  const records = data?.records ?? data?.list ?? []
  const t = data?.total
  return {
    records: Array.isArray(records) ? records : [],
    total: typeof t === 'number' ? t : t != null ? Number(t) : 0,
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBatchAuditList(query)
    const { records, total: t } = normalizePage(res)
    tableData.value = records
    total.value = t > 0 ? t : records.length
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  query.page = 1
  loadData()
}

const onPageSizeChange = () => {
  query.page = 1
  loadData()
}

const resetFilter = () => {
  query.status = 0
  query.page = 1
  loadData()
}

const handleAudit = async (row, status) => {
  if (status === 1) {
    try {
      await ElMessageBox.confirm(`确定要通过该生产批次「${row.batchNo}」吗？`, '审核确认', { type: 'success' })
      await auditProdBatch(row.id, 1)
      ElMessage.success('操作成功：批次已通过')
      loadData()
    } catch {}
  } else {
    currentAuditRow.value = row
    rejectReason.value = ''
    rejectVisible.value = true
  }
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return ElMessage.warning('请填写驳回原因')
  try {
    await auditProdBatch(currentAuditRow.value.id, 2, rejectReason.value)
    ElMessage.success('操作成功：批次已驳回')
    rejectVisible.value = false
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
@import '../prod/prodPageCommon.scss';

.text-muted {
  font-size: $text-sm;
  color: $color-text-disabled;
}
</style>
