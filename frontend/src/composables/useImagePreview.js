import { shallowReactive } from 'vue'

const preview = shallowReactive({
  visible: false,
  urls: [],
  index: 0,
})

export function useImagePreview() {
  const openImagePreview = (urls, index = 0) => {
    const list = (Array.isArray(urls) ? urls : [urls]).filter(Boolean)
    if (!list.length) return
    preview.urls = list
    preview.index = Math.max(0, Math.min(index, list.length - 1))
    preview.visible = true
  }

  const closeImagePreview = () => {
    preview.visible = false
  }

  return { preview, openImagePreview, closeImagePreview }
}
