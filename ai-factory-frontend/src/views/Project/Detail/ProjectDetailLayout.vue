<script setup lang="ts">
import AppHeader from '@/components/layout/AppHeader.vue'
import ProjectSidebar from '@/components/layout/ProjectSidebar.vue'
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-950">
    <AppHeader />
    <div class="flex" style="height: calc(100vh - 64px);">
      <ProjectSidebar />
      <main class="flex-1 overflow-auto">
        <router-view />
      </main>
    </div>
  </div>
</template>
