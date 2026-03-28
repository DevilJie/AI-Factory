<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { AlertTriangle, X } from 'lucide-vue-next'
import Btn from './Btn.vue'

interface Props {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning'
}

const props = withDefaults(defineProps<Props>(), {
  title: '确认',
  confirmText: '确认',
  cancelText: '取消',
  variant: 'danger'
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const visible = ref(false)

const handleConfirm = () => {
  emit('confirm')
  visible.value = false
}

const handleCancel = () => {
  emit('cancel')
  visible.value = false
}

// 监听 ESC 键关闭
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    handleCancel()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// 暴露方法给父组件调用
const show = () => {
  visible.value = true
}

const hide = () => {
  visible.value = false
}

defineExpose({
  show,
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 scale-95"
      leave-active-class="transition ease-in duration-150"
      leave-to-class="opacity-0 scale-100"
    >
      <div
        v-if="visible"
        class="fixed inset-0 z-50 overflow-y-auto"
        @click.self="handleCancel"
      >
        <div class="flex min-h-full items-center justify-center p-4">
          <Transition
            enter-active-class="transition ease-out duration-200"
            enter-from-class="opacity-0 translate-y-4 sm:scale-95"
            leave-active-class="transition ease-in duration-150"
            leave-to-class="opacity-0 translate-y-4 sm:scale-100"
          >
            <div
              class="bg-white dark:bg-gray-800 rounded-2xl shadow-xl max-w-md w-full overflow-hidden"
              @click.stop
            >
              <!-- Header -->
              <div class="flex items-center gap-3 p-4 border-b border-gray-200 dark:border-gray-700">
                <div
                  :class="[
                    'w-10 h-10 rounded-full flex items-center justify-center',
                    props.variant === 'danger' ? 'bg-red-100 dark:bg-red-900/30' : 'bg-yellow-100 dark:bg-yellow-900/30'
                  ]"
                >
                  <AlertTriangle
                    :class="[
                      'w-5 h-5',
                      props.variant === 'danger' ? 'text-red-600 dark:text-red-400' : 'text-yellow-600 dark:text-yellow-400'
                    ]"
                  />
                </div>
                <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                  {{ title }}
                </h3>
              </div>

              <!-- Body -->
              <div class="p-4">
                <p class="text-gray-600 dark:text-gray-300">{{ message }}</p>
              </div>

              <!-- Footer -->
              <div class="flex justify-end gap-2 p-4 border-t border-gray-200 dark:border-gray-700">
                <Btn variant="ghost" @click="handleCancel">
                  {{ cancelText }}
                </Btn>
                <Btn
                  :variant="props.variant === 'danger' ? 'danger' : 'primary'"
                  @click="handleConfirm"
                >
                  {{ confirmText }}
                </Btn>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
