<template>
  <div class="login-page">
    <!-- 背景：柔和网格纹理 + 渐变，给铁砧质感 -->
    <div class="bg-layer" />

    <!-- 可选：左侧展示区（大屏可放置产品名 / 口号），在窄屏隐藏 -->
    <div class="hero" aria-hidden="true">
      <div class="hero-inner">
        <svg class="anvil" viewBox="0 0 64 64" aria-hidden="true">
          <!-- 简化的铁砧图标 SVG（纯内嵌）-->
          <path d="M4 36h56v6H4z" fill="#2b2f33"/>
          <path d="M10 26h44v10H10z" fill="#3b3f44"/>
          <path d="M6 20h52v6H6z" fill="#23272a"/>
          <path d="M18 12h28v8H18z" fill="#1b1d20"/>
          <circle cx="48" cy="18" r="2" fill="#7f8fa3" />
        </svg>

        <h1 class="brand">TestForge</h1>
        <p class="tagline">缺陷管理 • 用例协同 • 简洁高效</p>
      </div>
    </div>

    <!-- 右侧登录卡（主交互）-->
    <div class="panel-wrap">
      <transition name="slide-fade">
        <div class="login-panel card" key="login-panel" role="main" aria-labelledby="login-title">
          <div class="panel-top">
            <!-- 小铁砧装饰 -->
            <div class="anvil-deco" aria-hidden="true">
              <svg viewBox="0 0 48 48" width="38" height="38">
                <path d="M2 28h44v4H2z" fill="#222"/>
                <path d="M8 20h32v10H8z" fill="#2b2f33"/>
                <path d="M12 8h24v12H12z" fill="#1b1d20"/>
              </svg>
            </div>
            <div class="panel-title">
              <div id="login-title" class="title">欢迎回到 TestForge</div>
              <div class="subtitle">用账号登录 · 管理你的用例与缺陷</div>
            </div>
          </div>

          <el-form :model="form" status-icon ref="formRef" label-position="top" class="login-form">
            <el-form-item prop="username" :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
              <el-input
                  v-model="form.username"
                  placeholder="用户名 / 邮箱"
                  autocomplete="username"
                  clearable
                  size="large"
                  :class="['mc-input']"
              />
            </el-form-item>

            <el-form-item prop="password" :rules="[{ required: true, message: '请输入密码', trigger: 'blur' }]">
              <!-- Element Plus 内置 show-password 控件，交互与动画都很好 -->
              <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="密码"
                  show-password
                  autocomplete="current-password"
                  size="large"
                  :class="['mc-input']"
              />
            </el-form-item>

            <div class="row-between">
              <el-checkbox v-model="remember">记住我</el-checkbox>
              <a class="forgot" @click.prevent="onForgot">忘记密码？</a>
            </div>

            <el-button
                :loading="loading"
                type="primary"
                size="large"
                class="login-btn"
                @click="submit"
                :disabled="loading"
            >
              登录
            </el-button>

            <div class="or-split">或使用</div>

            <div class="alt-actions">
              <el-button @click="demoLogin" size="medium" plain>演示账号</el-button>
              <el-button @click="$emit('goto-register')" size="medium" plain>注册</el-button>
            </div>
          </el-form>

          <div class="panel-footer">
            <small>登录即表示同意 <a href="#" @click.prevent>服务条款</a> 与 <a href="#" @click.prevent>隐私政策</a></small>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { savePermissions } from '@/utils/perm';

const formRef = ref(null);
const router = (() => {
  try { return useRouter(); } catch (e) { return null; }
})();

const form = reactive({
  username: '',
  password: '',
});

const remember = ref(true);
const loading = ref(false);

/**
 * 处理后端返回体：兼容 resp.data.token 或 resp.token
 * 同时返回 permissions
 */
const extractResp = (resp) => {
  const b = resp?.data ?? resp;
  return {
    token: b?.token ?? (b?.data && b.data.token) ?? null,
    expiresAt: b?.expiresAt ?? (b?.data && b.data.expiresAt) ?? null,
    permissions: b?.permissions ?? (b?.data && b.data.permissions) ?? []
  };
};

const saveToken = ({ token, expiresAt }) => {
  if (!token) return;
  localStorage.setItem('auth.token', token);
  localStorage.setItem('auth.token', token);
  if (expiresAt) localStorage.setItem('auth.expiresAt', String(expiresAt));
  localStorage.removeItem('auth.shortExpire');

  // 把 token 加到全局 axios header（注意要带上 "Bearer " 前缀）
  try {
    if (request?.defaults?.headers && request.defaults.headers.common) {
      request.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
  } catch (e) { /* ignore */ }
};

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码');
    return;
  }
  loading.value = true;
  try {
    const resp = await request.post('/auth/login', { username: form.username, password: form.password });
    const { token, expiresAt, permissions } = extractResp(resp);
    if (!token) {
      const msg = (resp?.data && resp.data.message) || (resp?.message) || '登录失败，未返回 token';
      ElMessage.error(msg);
      loading.value = false;
      return;
    }
    saveToken({ token, expiresAt });
    console.log(permissions)
    try { savePermissions(permissions || []); } catch (e) { console.warn('savePermissions failed', e); }

    ElMessage.success('登录成功');

    // 跳转：优先使用 router（若项目使用 vue-router），否则派发事件
    if (router && router.replace) {
      router.replace({ path: '/testforge/testCase' });
    } else {
      window.dispatchEvent(new CustomEvent('auth:login', { detail: { token } }));
    }
  } catch (err) {
    console.error(err);
    const msg = err?.response?.data?.message ?? err?.message ?? '登录失败';
    ElMessage.error(String(msg));
  } finally {
    loading.value = false;
  }
};

const demoLogin = () => {
  form.username = 'admin';
  form.password = 'admin123';
  submit();
};

const onForgot = () => {
  ElMessage.info('请联系管理员重置密码');
};
</script>


<style scoped>
/* 引入像素风字体（近似 MC） - 可选 */
@import url('https://fonts.googleapis.com/css2?family=Press+Start+2P&display=swap');

/* 页面背景 */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, #0b1215 0%, #0f1417 55%, #101418 100%);
  color: #e6eef6;
}

/* 背景纹理层，轻微噪点+金属感 */
.bg-layer {
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(180deg, rgba(255,255,255,0.02), rgba(0,0,0,0.06)),
      radial-gradient(circle at 10% 20%, rgba(255,255,255,0.01), transparent 12%),
      linear-gradient(90deg, rgba(255,255,255,0.01), rgba(255,255,255,0.00) 50%, rgba(255,255,255,0.01));
  mix-blend-mode: overlay;
  pointer-events: none;
}

/* 左侧 hero（展示区），大屏显示，窄屏隐藏 */
.hero {
  width: 48%;
  min-width: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 48px;
  box-sizing: border-box;
}
.hero-inner {
  text-align: left;
  transform: translateY(-10px);
}
.anvil { width: 96px; height: 96px; margin-bottom: 12px; filter: drop-shadow(0 8px 12px rgba(0,0,0,0.6)); }
.brand {
  font-family: 'Press Start 2P', system-ui, -apple-system, 'Segoe UI', Roboto, "Helvetica Neue", Arial;
  font-size: 20px;
  color: #c7d5e0;
  margin: 0 0 6px 0;
  letter-spacing: 1px;
}
.tagline {
  color: #98a8b6;
  margin: 0;
  font-size: 13px;
  max-width: 520px;
  line-height: 1.4;
}

/* 登录面板容器：始终靠右（大屏），窄屏居中 */
.panel-wrap {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 36px;
  box-sizing: border-box;
}

/* 登录卡片 */
.login-panel {
  width: min(460px, 92%);
  max-width: 520px;
  border-radius: 12px;
  background: linear-gradient(180deg, #101317, #0f1215);
  box-shadow: 0 12px 40px rgba(2,6,10,0.6), inset 0 1px 0 rgba(255,255,255,0.02);
  padding: 20px;
  color: #e6eef6;
  border: 1px solid rgba(255,255,255,0.03);
  transform-origin: right center;
}

/* panel top: title & anvil deco */
.panel-top {
  display:flex;
  align-items:center;
  gap: 12px;
  margin-bottom: 12px;
}
.anvil-deco { width: 44px; height: 44px; border-radius: 8px; display:flex; align-items:center; justify-content:center; background: linear-gradient(180deg,#2b2f33,#1b1d20); box-shadow: 0 4px 12px rgba(8,10,12,0.6) inset; border: 1px solid rgba(255,255,255,0.02); }
.panel-title .title {
  font-weight: 700;
  font-size: 16px;
  color: #e6eef6;
}
.panel-title .subtitle {
  font-size: 12px;
  color: #98a8b6;
}

/* 表单与输入 */
.login-form { margin-top: 6px; }
.mc-input .el-input__inner {
  background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(0,0,0,0.08));
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.04);
  color: #e6eef6;
  height: 44px;
  padding-left: 12px;
  transition: box-shadow .18s ease, transform .12s ease;
  font-family: 'Press Start 2P', system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial;
  font-size: 12px;
}
.mc-input .el-input__inner::placeholder { color: rgba(230,238,246,0.38); font-family: inherit; font-size: 12px; }
.mc-input .el-input__inner:focus {
  box-shadow: 0 6px 22px rgba(31,111,235,0.12);
  transform: translateY(-1px);
  border-color: rgba(31,111,235,0.25);
}

/* 行排列（记住我 / 忘记密码） */
.row-between {
  display:flex;
  justify-content:space-between;
  align-items:center;
  margin: 8px 0 14px;
  color: #98a8b6;
  font-size: 13px;
}
.row-between .forgot { color: #8fb1ff; cursor: pointer; text-decoration: none; }
.row-between .forgot:hover { text-decoration: underline; }

/* 登录按钮 */
.login-btn {
  width: 100%;
  margin-top: 6px;
  background: linear-gradient(180deg,#1f6feb,#1160e6);
  border: none;
  box-shadow: 0 10px 30px rgba(17,98,212,0.18);
  color: white;
  font-weight: 700;
}

/* 分割（or） */
.or-split {
  text-align:center;
  color: #8597a8;
  margin: 12px 0 8px;
  font-size: 12px;
}

/* 备用操作 */
.alt-actions {
  display:flex;
  gap: 8px;
  justify-content: center;
}

/* footer 小字 */
.panel-footer {
  margin-top: 12px;
  text-align:center;
  color: #7e8b97;
  font-size: 11px;
}

/* 动画 */
.slide-fade-enter-active { transition: all .36s cubic-bezier(.2,.9,.3,1); }
.slide-fade-leave-active { transition: all .24s ease; }
.slide-fade-enter-from { transform: translateX(16px); opacity: 0; }
.slide-fade-enter-to { transform: translateX(0); opacity: 1; }

/* 响应式：窄屏隐藏 hero 区，使 panel 居中 */
@media (max-width: 920px) {
  .hero { display: none; }
  .panel-wrap { justify-content: center; padding: 20px; }
  .login-panel { width: min(92%, 560px); }
}
</style>
