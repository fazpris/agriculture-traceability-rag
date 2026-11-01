<template>
  <div class="ai-assistant-container" :class="{ 'is-open': isOpen }">
    <!-- 悬浮小人 (Sprite) -->
    <div 
      class="ai-sprite" 
      @click="toggleChat"
      :title="isOpen ? '关闭助手' : '点击对话'"
    >
      <div class="sprite-body">
        <svg viewBox="0 0 100 100" class="robot-svg">
          <!-- 头部本体 -->
          <defs>
            <linearGradient id="robotGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#10B981" />
              <stop offset="100%" style="stop-color:#059669" />
            </linearGradient>
            <filter id="glow">
              <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
          </defs>
          
          <!-- 耳机/天线座 -->
          <rect x="20" y="35" width="10" height="20" rx="5" fill="#065F46" />
          <rect x="70" y="35" width="10" height="20" rx="5" fill="#065F46" />
          
          <!-- 脑袋 -->
          <rect x="25" y="20" width="50" height="50" rx="18" fill="url(#robotGrad)" />
          
          <!-- 屏幕背景 -->
          <rect x="32" y="30" width="36" height="25" rx="8" fill="#064E3B" />
          
          <!-- 眼睛 (带闪烁动画) -->
          <g class="eyes" filter="url(#glow)">
            <circle class="eye-left" cx="43" cy="42" r="3" fill="#34D399" />
            <circle class="eye-right" cx="57" cy="42" r="3" fill="#34D399" />
          </g>
          
          <!-- 嘴巴/状态灯 -->
          <rect x="44" y="60" width="12" height="3" rx="1.5" fill="#065F46" />
          
          <!-- 晃动的天线 -->
          <g class="antenna">
            <line x1="50" y1="20" x2="50" y2="10" stroke="#10B981" stroke-width="3" />
            <circle cx="50" cy="8" r="4" fill="#FCD34D" filter="url(#glow)" />
          </g>
        </svg>
      </div>
      <!-- 气泡提示 -->
      <Transition name="pop">
        <div v-if="!isOpen && showTip" class="sprite-tip">
          有什么可以帮您？
        </div>
      </Transition>
    </div>

    <!-- 聊天窗口 (玻璃拟态) -->
    <Transition name="panel-slide">
      <div v-if="isOpen" class="chat-panel">
        <div class="panel-header">
          <div class="header-left">
            <div class="status-dot"></div>
            <span>AI 溯源助手</span>
          </div>
          <el-icon class="close-icon" @click="isOpen = false"><Close /></el-icon>
        </div>
        
        <div class="panel-body" ref="threadRef">
          <div class="safe-hint">
            依据档与知识库为您解答，仅供参考。
          </div>
          
          <div 
            v-for="(msg, i) in messages" 
            :key="i" 
            class="msg-row" 
            :class="msg.role"
          >
            <div class="msg-avatar">
              <template v-if="msg.role === 'assistant'">AI</template>
              <template v-else>我</template>
            </div>
            <div class="msg-bubble shadow-sm">
              {{ msg.content }}
            </div>
          </div>
          
          <div v-if="sending" class="msg-row assistant">
            <div class="msg-avatar pulse">AI</div>
            <div class="msg-bubble typing">正在生成回答...</div>
          </div>
        </div>

        <div class="panel-footer">
          <div class="input-wrapper">
            <textarea 
              v-model="input" 
              placeholder="请输入您的问题..."
              @keydown.enter.exact.prevent="sendMsg"
              :disabled="sending"
              rows="1"
            ></textarea>
            <el-button 
              type="primary" 
              circle 
              :loading="sending" 
              @click="sendMsg"
              class="send-btn"
            >
              <el-icon><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import { Close, Promotion } from '@element-plus/icons-vue'
import { ragChat } from '@/api/sys'
import { ElMessage } from 'element-plus'

const props = defineProps({
  productCode: {
    type: String,
    default: ''
  }
})

const isOpen = ref(false)
const showTip = ref(true)
const sending = ref(false)
const input = ref('')
const threadRef = ref(null)
const sessionId = ref('')

const messages = ref([
  { role: 'assistant', content: '您好！我是您的 AI 溯源管家，关于当前产品的产地、批次或检测报告，您可以直接问我。' }
])

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    showTip.value = false
    scrollBottom()
  }
}

const scrollBottom = () => {
  nextTick(() => {
    if (threadRef.value) {
      threadRef.value.scrollTop = threadRef.value.scrollHeight
    }
  })
}

watch(messages, () => scrollBottom(), { deep: true })

const sendMsg = async () => {
  const q = input.value.trim()
  if (!q || sending.value) return
  
  if (!sessionId.value) {
    sessionId.value = `${props.productCode || 'global'}-${Date.now()}`
  }

  messages.value.push({ role: 'user', content: q })
  input.value = ''
  sending.value = true

  try {
    const res = await ragChat({
      productCode: props.productCode,
      question: q,
      sessionId: sessionId.value,
    })
    messages.value.push({ 
      role: 'assistant', 
      content: res?.answer || '抱歉，我暂时无法回答这个问题。' 
    })
  } catch (e) {
    messages.value.push({ 
      role: 'assistant', 
      content: '连接 AI 引擎超时，请稍后再试或检查后端服务。' 
    })
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  // 3秒后隐藏初始提示气泡
  setTimeout(() => {
    if (!isOpen.value) showTip.value = false
  }, 8000)
})
</script>

<style lang="scss" scoped>
.ai-assistant-container {
  position: fixed;
  right: 30px;
  bottom: 30px;
  z-index: 2000;
  pointer-events: none;

  &.is-open {
    pointer-events: all;
  }
}

/* 悬浮小人样式 */
.ai-sprite {
  pointer-events: all;
  cursor: pointer;
  position: relative;
  width: 70px;
  height: 70px;
  transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  
  &:hover {
    transform: scale(1.1) translateY(-5px);
  }

  .sprite-body {
    width: 100%;
    height: 100%;
    filter: drop-shadow(0 10px 20px rgba(16, 185, 129, 0.3));
    animation: float 4s ease-in-out infinite;
  }

  .robot-svg {
    width: 100%;
    height: 100%;
    
    .eyes circle {
      animation: blink 5s infinite;
    }
    
    .antenna {
      transform-origin: 50% 20%;
      animation: swing 3s ease-in-out infinite;
    }
  }

  .sprite-tip {
    position: absolute;
    right: 80px;
    top: 10px;
    background: #fff;
    padding: 8px 16px;
    border-radius: 12px 12px 0 12px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    font-size: 13px;
    color: var(--color-primary);
    white-space: nowrap;
    border: 1px solid #EEF2F7;
    
    &::after {
      content: '';
      position: absolute;
      right: -8px;
      bottom: 0;
      border-left: 8px solid #fff;
      border-top: 8px solid transparent;
    }
  }
}

/* 聊天面板 - 玻璃拟态设计 */
.chat-panel {
  position: absolute;
  right: 0;
  bottom: 85px;
  width: 360px;
  height: 520px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transform-origin: right bottom;
}

.panel-header {
  padding: 16px 20px;
  background: rgba(16, 185, 129, 0.05);
  border-bottom: 1px solid rgba(0,0,0,0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 700;
    color: var(--color-primary);
    
    .status-dot {
      width: 8px;
      height: 8px;
      background: #10B981;
      border-radius: 50%;
      box-shadow: 0 0 8px #10B981;
    }
  }
  
  .close-icon {
    cursor: pointer;
    font-size: 18px;
    color: #64748B;
    &:hover { color: #EF4444; }
  }
}

.panel-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .safe-hint {
    font-size: 11px;
    color: #94A3B8;
    background: rgba(0,0,0,0.03);
    padding: 6px 12px;
    border-radius: 8px;
    text-align: center;
  }

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 2px; }
}

.msg-row {
  display: flex;
  gap: 10px;
  max-width: 85%;
  
  &.user {
    align-self: flex-end;
    flex-direction: row-reverse;
    .msg-bubble {
      background: var(--color-primary);
      color: #fff;
      border-radius: 16px 4px 16px 16px;
    }
    .msg-avatar { background: #ECFDF5; color: #059669; }
  }

  &.assistant {
    align-self: flex-start;
    .msg-bubble {
      background: #fff;
      color: #1E293B;
      border-radius: 4px 16px 16px 16px;
      border: 1px solid #F1F5F9;
    }
    .msg-avatar { background: var(--color-primary); color: #fff; }
  }

  .msg-avatar {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    flex-shrink: 0;
    font-size: 10px;
    font-weight: 800;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .msg-bubble {
    padding: 10px 14px;
    font-size: 13.5px;
    line-height: 1.6;
    word-break: break-word;
  }
}

.panel-footer {
  padding: 16px 20px 24px;
  
  .input-wrapper {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 16px;
    display: flex;
    align-items: flex-end;
    padding: 8px 12px;
    transition: all 0.2s;
    
    &:focus-within {
      background: #fff;
      border-color: var(--color-primary);
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
    }
    
    textarea {
      flex: 1;
      border: none;
      background: transparent;
      outline: none;
      padding: 8px 0;
      font-size: 14px;
      resize: none;
      max-height: 120px;
      &::placeholder { color: #94A3B8; }
    }
    
    .send-btn {
      margin-left: 10px;
      margin-bottom: 2px;
    }
  }
}

/* Animations */
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes blink {
  0%, 90%, 100% { transform: scaleY(1); }
  95% { transform: scaleY(0.1); }
}

@keyframes swing {
  0%, 100% { transform: rotate(-5deg); }
  50% { transform: rotate(5deg); }
}

/* Transitions */
.pop-enter-active, .pop-leave-active { transition: all 0.3s ease; }
.pop-enter-from { opacity: 0; transform: translateX(10px) scale(0.8); }
.pop-leave-to { opacity: 0; transform: translateX(10px) scale(0.8); }

.panel-slide-enter-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.panel-slide-leave-active { transition: all 0.3s ease; }
.panel-slide-enter-from { opacity: 0; transform: scale(0.5) translateY(40px); }
.panel-slide-leave-to { opacity: 0; transform: scale(0.9) translateY(20px); }

/* 移动端适配 */
@media (max-width: 600px) {
  .ai-assistant-container {
    right: 15px;
    bottom: 15px;
  }
  
  .chat-panel {
    width: calc(100vw - 30px);
    right: 0;
    height: 60vh;
  }
}
</style>
