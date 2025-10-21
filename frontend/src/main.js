// src/main.js
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import '@/assets/body.css';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

import permDirective from '@/plugins/permDirective'; // 全局权限指令
import { loadPermissions } from '@/utils/perm';     // 启动时恢复 permissions 到内存
import request from '@/utils/request';              // 你的 axios 实例（确保存在）

const app = createApp(App);

// 注册 Element Plus（含中文）
app.use(router);
app.use(ElementPlus, {
    locale: zhCn,
});

// 注册 Element Plus icons（保持你原来的写法）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

// 注册权限指令 v-perm
console.log('permDirective:', permDirective);
app.use(permDirective); // 确认这行确实执行
console.log('perm directive registered');


// 启动时：如果本地已有 token，自动把它加到 axios 的 Authorization header
// 优先从 localStorage（记住登录）读取，若无再从 sessionStorage 读取
try {
    const token = localStorage.getItem('auth.token') || sessionStorage.getItem('auth.token');
    if (token && request && request.defaults && request.defaults.headers && request.defaults.headers.common) {
        // 注意：后端拦截器期望格式 "Bearer <token>"
        request.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
} catch (e) {
    // 忽略任何运行时错误（防止因 request 未定义导致应用启动失败）
    console.warn('init auth header failed', e);
}

// 把本地保存的 permissions 读到内存（perm.js 有缓存逻辑）
try {
    loadPermissions();
} catch (e) {
    console.warn('load permissions failed', e);
}

app.mount('#app');
