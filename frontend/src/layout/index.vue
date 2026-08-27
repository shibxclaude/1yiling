<template>
  <div class="app-wrapper">
    <div class="sidebar-container"><Sidebar /></div>
    <div class="main-container">
      <Navbar />
      <TagsView />
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
</script>

<style scoped>
.app-wrapper { display:flex; height:100vh; }
.sidebar-container { width:212px; background:var(--y-sidebar-bg); border-right:1px solid var(--y-border); flex-shrink:0; overflow-y:auto; }
.main-container { flex:1; display:flex; flex-direction:column; overflow:hidden; background:var(--y-canvas); }
.app-main { flex:1; overflow:auto; }
</style>
