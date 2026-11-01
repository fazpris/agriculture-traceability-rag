<template>
  <Teleport to="body">
    <ElImageViewer
      v-if="preview.visible"
      :url-list="preview.urls"
      :initial-index="preview.index"
      teleported
      hide-on-click-modal
      @close="closeImagePreview"
    />
  </Teleport>
</template>

<script setup>
import { ElImageViewer } from 'element-plus'
import { useImagePreview } from '@/composables/useImagePreview'

const { preview, closeImagePreview } = useImagePreview()
</script>
