<script setup lang="ts">
import { ChevronDown } from 'lucide-vue-next'
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'

interface Option {
  value: string | number
  label: string
}

interface Props {
  modelValue?: string | number
  options: Option[]
  placeholder?: string
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请选择',
  disabled: false
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const isOpen = ref(false)
const buttonRef = ref<HTMLElement | null>(null)
const dropdownRef = ref<HTMLElement | null>(null)
const dropdownStyle = ref({})

const selectedLabel = computed(() => {
  const option = props.options.find(o => o.value === props.modelValue)
  return option?.label || props.placeholder
})

const updatePosition = () => {
  if (!buttonRef.value) return

  const rect = buttonRef.value.getBoundingClientRect()
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`,
    width: `${rect.width}px`,
    zIndex: 9999
  }
}

const selectOption = (option: Option) => {
  emit('update:modelValue', option.value)
  isOpen.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  if (
    buttonRef.value &&
    !buttonRef.value.contains(event.target as Node) &&
    dropdownRef.value &&
    !dropdownRef.value.contains(event.target as Node)
  ) {
    isOpen.value = false
  }
}

const openDropdown = async () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    await nextTick()
    updatePosition()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', updatePosition)
  window.addEventListener('scroll', updatePosition, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', updatePosition)
  window.removeEventListener('scroll', updatePosition, true)
})
</script>

<template>
  <div class="relative">
    <button
      ref="buttonRef"
      type="button"
      :disabled="disabled"
      class="w-full flex items-center justify-between gap-2 px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-700 bg-white/50 dark:bg-gray-800/50 backdrop-blur-sm text-gray-900 dark:text-gray-100 transition-all duration-200 hover:border-gray-300 dark:hover:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 disabled:opacity-50"
      @click="openDropdown"
    >
      <span :class="modelValue ? '' : 'text-gray-400 dark:text-gray-500'">
        {{ selectedLabel }}
      </span>
      <ChevronDown
        :size="16"
        :class="['transition-transform duration-200', isOpen ? 'rotate-180' : '']"
      />
    </button>

    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0 -translate-y-1"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-1"
      >
        <div
          v-if="isOpen"
          ref="dropdownRef"
          :style="dropdownStyle"
          class="py-1 rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-xl"
        >
          <button
            v-for="option in options"
            :key="option.value"
            type="button"
            :class="[
              'w-full px-4 py-2 text-left text-sm transition-colors',
              option.value === modelValue
                ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
                : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700'
            ]"
            @click="selectOption(option)"
          >
            {{ option.label }}
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
