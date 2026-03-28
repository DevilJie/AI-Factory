<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Plus, FileText, FolderOpen, Settings } from 'lucide-vue-next'
import Btn from '@/components/ui/Btn.vue'

const router = useRouter()

const actions = [
  { icon: Plus, label: '新建项目', color: 'from-blue-500 to-cyan-500', action: () => router.push('/project/create') },
  { icon: FileText, label: '新建章节', color: 'from-purple-500 to-pink-500', action: () => router.push('/chapter/create') },
  { icon: FolderOpen, label: '我的项目', color: 'from-orange-500 to-amber-500', action: () => router.push('/projects') },
  { icon: Settings, label: '设置', color: 'from-gray-500 to-slate-500', action: () => router.push('/settings') }
]
</script>

<template>
  <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
    <button
      v-for="(action, index) in actions"
      :key="index"
      class="group flex flex-col items-center gap-2 p-4 rounded-2xl bg-white/90 dark:bg-gray-800/50 backdrop-blur-md border border-gray-200/50 dark:border-gray-700/50 hover:shadow-lg hover:shadow-blue-500/10 hover:border-blue-500/30 transition-all duration-300"
      @click="action.action"
    >
      <div :class="['w-10 h-10 rounded-xl bg-gradient-to-br flex items-center justify-center transition-transform group-hover:scale-110', action.color]">
        <component :is="action.icon" class="w-5 h-5 text-white" />
      </div>
      <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ action.label }}</span>
    </button>
  </div>
</template>
