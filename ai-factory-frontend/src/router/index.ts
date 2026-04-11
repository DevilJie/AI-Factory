import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login/index.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard/index.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/project/create',
      name: 'CreateProject',
      component: () => import('@/views/Project/CreateProject/index.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/project/:id',
      component: () => import('@/views/Project/Detail/ProjectDetailLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: 'overview' },
        { path: 'overview', name: 'ProjectOverview', component: () => import('@/views/Project/Detail/Overview.vue') },
        { path: 'world-setting', name: 'ProjectWorldSetting', component: () => import('@/views/Project/Detail/WorldSetting.vue') },
        { path: 'settings', name: 'ProjectSettings', component: () => import('@/views/Project/Detail/Settings.vue') },
        { path: 'characters', name: 'ProjectCharacters', component: () => import('@/views/Project/Detail/Characters.vue') },
        { path: 'foreshadowing', name: 'ProjectForeshadowing', component: () => import('@/views/Project/Detail/Foreshadowing.vue') },
        { path: 'creation', name: 'ProjectCreation', component: () => import('@/views/Project/Detail/creation/CreationCenter.vue') },
        { path: 'creation/volume/:volumeId', name: 'ProjectCreationVolume', component: () => import('@/views/Project/Detail/creation/CreationCenter.vue') },
        { path: 'creation/volume/:volumeId/chapter/:chapterId', name: 'ProjectCreationChapter', component: () => import('@/views/Project/Detail/creation/CreationCenter.vue') }
      ]
    },
    {
      path: '/',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && userStore.isLoggedIn) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
